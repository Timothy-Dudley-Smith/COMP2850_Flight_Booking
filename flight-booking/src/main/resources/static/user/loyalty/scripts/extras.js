// currency dropdown - reload page on change to update prices
const currencySelect = document.getElementById("currency-select");
if (currencySelect) {
    currencySelect.value = getSelectedCurrency();
    currencySelect.addEventListener("change", () => {
        setSelectedCurrency(currencySelect.value);
        location.reload();
    });
}

// load up the booking from session storage
const bookingDraftRaw = sessionStorage.getItem("bookingDraft");
let bookingDraft = bookingDraftRaw ? JSON.parse(bookingDraftRaw) : null;
const continueButton = document.querySelector(".btn");
const backButton = document.querySelector(".btnb");
const handoffMessage = document.getElementById("handoff-message");

if (bookingDraft && typeof bookingDraft.baseFareTotal !== "number") {
    bookingDraft.baseFareTotal = Number(bookingDraft.totalPrice) || Number(bookingDraft.price) || 0;
}

function getPassengerOneName() {
    if (!bookingDraft || !Array.isArray(bookingDraft.passengers) || bookingDraft.passengers.length === 0) {
        return "Guest";
    }

    const passengerOne = bookingDraft.passengers[0];
    const first = passengerOne.firstName || "";
    const last = passengerOne.lastName || "";
    return `${first} ${last}`.trim() || "Guest";
}

function getInitials(name) {
    if (!name) return "--";

    const parts = name.trim().split(/\s+/);
    if (parts.length === 1) {
        return parts[0].slice(0, 2).toUpperCase();
    }

    return `${parts[0][0] ?? ""}${parts[1][0] ?? ""}`.toUpperCase();
}

// price constants
const MEMBER_SEAT_PRICE = 0;
const GUEST_SEAT_PRICE = 20;
const MEMBER_WIFI_PRICE = 0;
const GUEST_WIFI_PRICE = 8.99;
const CLASSIC_MEAL_FULL_PRICE = 12;
const CLASSIC_MEAL_MEMBER_PRICE = 8.40;
const PREMIUM_MEAL_FULL_PRICE = 22;
const PREMIUM_MEAL_MEMBER_PRICE = 15.40;

// session info
const sessionId = sessionStorage.getItem("sessionId");
const userId = sessionStorage.getItem("userId");
const firstName = sessionStorage.getItem("firstName");
const lastName = sessionStorage.getItem("lastName");
const isSignedIn = !!sessionId && !!userId;

function applyPricingDisplayMode() {
    const seatPerkBadge = document.getElementById("seat-perk-badge");
    const mealDiscountLabel = document.getElementById("meal-discount-label");
    const wifiPerkBadge = document.getElementById("wifi-perk-badge");
    const wifiPerkText = document.getElementById("wifi-perk-text");

    const classicMealInput = document.querySelector('input[name="meal"][data-label="Classic hot meal"]');
    const premiumMealInput = document.querySelector('input[name="meal"][data-label="Premium dine set"]');

    const classicMealPriceDisplay = document.getElementById("classic-meal-price-display");
    const premiumMealPriceDisplay = document.getElementById("premium-meal-price-display");

    if (isSignedIn) {
        if (seatPerkBadge) seatPerkBadge.textContent = "Free";
        if (wifiPerkBadge) wifiPerkBadge.textContent = "Free";
        if (wifiPerkText) wifiPerkText.textContent = "Full-flight included";
        if (mealDiscountLabel) mealDiscountLabel.textContent = "30% Gold discount";
        if (classicMealInput) classicMealInput.dataset.price = String(CLASSIC_MEAL_MEMBER_PRICE);
        if (premiumMealInput) premiumMealInput.dataset.price = String(PREMIUM_MEAL_MEMBER_PRICE);
        if (classicMealPriceDisplay) {
            classicMealPriceDisplay.innerHTML = `<s>${fmt(CLASSIC_MEAL_FULL_PRICE)}</s><span class="gld">${fmt(CLASSIC_MEAL_MEMBER_PRICE)}</span>`;
        }
        if (premiumMealPriceDisplay) {
            premiumMealPriceDisplay.innerHTML = `<s>${fmt(PREMIUM_MEAL_FULL_PRICE)}</s><span class="gld">${fmt(PREMIUM_MEAL_MEMBER_PRICE)}</span>`;
        }
    } else {
        if (seatPerkBadge) seatPerkBadge.textContent = `${fmt(GUEST_SEAT_PRICE)}`;
        if (wifiPerkBadge) wifiPerkBadge.textContent = `${fmt(GUEST_WIFI_PRICE)}`;
        if (wifiPerkText) wifiPerkText.textContent = "Charged for guests";
        if (mealDiscountLabel) mealDiscountLabel.textContent = "";
        if (classicMealInput) classicMealInput.dataset.price = String(CLASSIC_MEAL_FULL_PRICE);
        if (premiumMealInput) premiumMealInput.dataset.price = String(PREMIUM_MEAL_FULL_PRICE);
        if (classicMealPriceDisplay) classicMealPriceDisplay.innerHTML = `${fmt(CLASSIC_MEAL_FULL_PRICE)}`;
        if (premiumMealPriceDisplay) premiumMealPriceDisplay.innerHTML = `${fmt(PREMIUM_MEAL_FULL_PRICE)}`;
    }
}

function getSelectedRadioPrice(name) {
    const input = document.querySelector(`input[name="${name}"]:checked`);
    if (!input || !input.dataset.price) return 0;
    return Number(input.dataset.price) || 0;
}

function getCheckedExtrasTotal() {
    const inputs = document.querySelectorAll('input[type="checkbox"]:checked[data-price]');
    let total = 0;
    for (let i = 0; i < inputs.length; i++) {
        total += Number(inputs[i].dataset.price) || 0;
    }
    return total;
}

function getPassengerCount() {
    if (Array.isArray(bookingDraft?.passengers) && bookingDraft.passengers.length > 0) {
        return bookingDraft.passengers.length;
    }
    return Math.max(1, Number(bookingDraft?.passengerCount ?? 1));
}

function calculateLiveTotal() {
    const baseFare = Number(bookingDraft?.baseFareTotal) || 0;
    const passengerCount = getPassengerCount();
    const baggagePrice = getSelectedRadioPrice("bag");
    const mealPrice = getSelectedRadioPrice("meal");
    const insurancePrice = getSelectedRadioPrice("ins");
    const extrasPrice = getCheckedExtrasTotal();
    const seatPrice = isSignedIn ? MEMBER_SEAT_PRICE : GUEST_SEAT_PRICE;
    const wifiPrice = isSignedIn ? MEMBER_WIFI_PRICE : GUEST_WIFI_PRICE;

    const addOnsTotalPerPassenger = baggagePrice + mealPrice + insurancePrice + extrasPrice + seatPrice + wifiPrice;
    return baseFare + (addOnsTotalPerPassenger * passengerCount);
}

function updateLiveTotalDisplay() {
    const total = calculateLiveTotal();
    const baseFare = Number(bookingDraft?.baseFareTotal) || 0;

    const sumBaseEl = document.getElementById("sum-base");
    const totalEl = document.getElementById("tot");

    if (sumBaseEl) sumBaseEl.textContent = `${fmt(baseFare)}`;
    if (totalEl) totalEl.textContent = `${fmt(total)}`;

    if (bookingDraft) {
        bookingDraft.totalPrice = total;
        sessionStorage.setItem("bookingDraft", JSON.stringify(bookingDraft));
    }
}

const accountNameEl = document.getElementById("account-name");
const accountTierEl = document.getElementById("account-tier");
const accountPointsEl = document.getElementById("account-points");
const accountInitialsEl = document.getElementById("account-initials");

async function populateAccountCard() {
    if (isSignedIn) {
        try {
            const res = await fetch(`/api/account-summary?userId=${encodeURIComponent(userId)}`);
            const data = await res.json();

            if (!res.ok) {
                throw new Error(data.error || "Failed to load account summary");
            }

            const fullName = `${data.firstName ?? firstName ?? ""} ${data.lastName ?? lastName ?? ""}`.trim();

            if (accountNameEl) accountNameEl.textContent = fullName || "Member";
            if (accountTierEl) accountTierEl.textContent = `${data.membershipTier} • ${data.membershipNumber}`;
            if (accountPointsEl) accountPointsEl.innerHTML = `<span style="color: white;">${data.loyaltyPoints.toLocaleString()}</span><small style="color: white;">Avios points</small>`;


            if (accountInitialsEl) accountInitialsEl.textContent = getInitials(fullName);
        } catch {
            const fallbackName = `${firstName ?? ""} ${lastName ?? ""}`.trim();
            accountNameEl.textContent = fallbackName || "Member";
            accountTierEl.textContent = "Member";
            accountPointsEl.textContent = "";
            accountInitialsEl.textContent = getInitials(fallbackName);
        }
    } else {
        const passengerName = getPassengerOneName();
        accountNameEl.textContent = passengerName;
        accountTierEl.textContent = "";
        accountPointsEl.textContent = "";
        accountInitialsEl.textContent = getInitials(passengerName);
    }
}

// log in / log out display
const welcomeMessage = document.getElementById("welcome-message");
const loginLink = document.getElementById("login-link");
const logoutButton = document.getElementById("logout-button");

if (sessionId && firstName) {
    if (welcomeMessage) welcomeMessage.textContent = `Welcome, ${firstName}`;
    if (loginLink) loginLink.style.display = "none";
    if (logoutButton) logoutButton.style.display = "inline-block";
}

if (logoutButton) {
    logoutButton.addEventListener("click", async () => {
        const currentSessionId = sessionStorage.getItem("sessionId");
        try {
            if (currentSessionId) {
                await fetch(`/api/auth/logout?sessionId=${encodeURIComponent(currentSessionId)}`, {
                    method: "POST"
                });
            }
        } catch (_) {}
        sessionStorage.clear();
        window.location.href = "/log_in";
    });
}

function formatRouteDate(date) {
    if (!date) return "From today onwards";
    const parsedDate = new Date(date);
    if (Number.isNaN(parsedDate.getTime())) return "From today onwards";
    return parsedDate.toLocaleDateString("en-GB", { day: "numeric", month: "long" });
}

function getSelectedOption(name) {
    const input = document.querySelector(`input[name="${name}"]:checked`);
    if (!input || !input.dataset.label) return null;
    return input.dataset.label;
}

function getSelectedExtras() {
    const extras = [];
    const inputs = document.querySelectorAll('input[type="checkbox"]:checked[data-label]');
    for (let i = 0; i < inputs.length; i++) {
        extras.push(inputs[i].dataset.label);
    }
    return extras;
}

function buildBookingHandoff(draft) {
    return {
        userId: draft.userId,
        flightId: draft.flightId,
        from: draft.from,
        to: draft.to,
        date: draft.date,
        passengerCount: draft.passengerCount,
        cabin: draft.cabin,
        passengers: draft.passengers,
        seatNumbers: draft.seatNumbers,
        addOns: draft.addOns,
        holdId: draft.holdId,
        holdExpiryTime: draft.holdExpiryTime,
        totalPrice: draft.totalPrice
    };
}

function saveSelections() {
    if (!bookingDraft) return null;

    const passengers = Array.isArray(bookingDraft.passengers) ? bookingDraft.passengers : [];
    const seatNumbers = Array.isArray(bookingDraft.seatNumbers) ? bookingDraft.seatNumbers : [];
    const baseFareTotal = Number(bookingDraft.baseFareTotal) || Number(bookingDraft.totalPrice) || Number(bookingDraft.price) || 0;

    bookingDraft = {
        ...bookingDraft,
        userId: typeof bookingDraft.userId === "number" ? bookingDraft.userId : null,
        flightId: bookingDraft.flightId ?? "",
        from: bookingDraft.from ?? "",
        to: bookingDraft.to ?? "",
        date: bookingDraft.date ?? "",
        passengerCount: passengers.length > 0 ? passengers.length : Math.max(1, Number(bookingDraft.passengerCount ?? 1)),
        cabin: bookingDraft.cabin ?? "Economy",
        passengers,
        seatNumbers,
        baseFareTotal,
        baggage: getSelectedOption("bag"),
        meal: getSelectedOption("meal"),
        insurance: getSelectedOption("ins"),
        extras: getSelectedExtras()
    };

    bookingDraft.addOns = {
        baggage: bookingDraft.baggage,
        meal: bookingDraft.meal,
        insurance: bookingDraft.insurance,
        extras: bookingDraft.extras
    };

    delete bookingDraft.baggage;
    delete bookingDraft.meal;
    delete bookingDraft.insurance;
    delete bookingDraft.extras;

    bookingDraft.holdId = bookingDraft.holdId ?? null;
    bookingDraft.holdExpiryTime = bookingDraft.holdExpiryTime ?? null;
    bookingDraft.totalPrice = calculateLiveTotal();

    sessionStorage.setItem("bookingDraft", JSON.stringify(bookingDraft));
    sessionStorage.setItem("pendingBookingRequest", JSON.stringify(buildBookingHandoff(bookingDraft)));

    if (bookingDraft.holdId !== null || bookingDraft.holdExpiryTime || bookingDraft.totalPrice !== null) {
        sessionStorage.setItem("holdData", JSON.stringify({
            holdId: bookingDraft.holdId,
            userId: bookingDraft.userId,
            flightId: bookingDraft.flightId,
            seatNumbers: bookingDraft.seatNumbers,
            expiryTime: bookingDraft.holdExpiryTime,
            totalPrice: bookingDraft.totalPrice
        }));
    }

    return bookingDraft;
}

function updateSelectionsSummary() {
    const lines = document.getElementById("lines");
    const passengerCount = getPassengerCount();
    const rows = [];

    const bagInput = document.querySelector('input[name="bag"]:checked');
    if (bagInput?.dataset.label) {
        rows.push({ label: bagInput.dataset.label, price: Number(bagInput.dataset.price) || 0 });
    }

    const mealInput = document.querySelector('input[name="meal"]:checked');
    if (mealInput?.dataset.label) {
        rows.push({ label: mealInput.dataset.label, price: Number(mealInput.dataset.price) || 0 });
    }

    const insInput = document.querySelector('input[name="ins"]:checked');
    if (insInput?.dataset.label) {
        rows.push({ label: insInput.dataset.label, price: Number(insInput.dataset.price) || 0 });
    }

    const extraInputs = document.querySelectorAll('input[type="checkbox"]:checked[data-label]');
    for (let i = 0; i < extraInputs.length; i++) {
        rows.push({ label: extraInputs[i].dataset.label, price: Number(extraInputs[i].dataset.price) || 0 });
    }

    lines.innerHTML = rows.map(({ label, price }) => `
        <div class="sr">
            <span>${label}</span>
            <span>${price > 0 ? `${fmt(price)} × ${passengerCount}` : "—"}</span>
        </div>
    `).join("");
}

// fill in route info + restore saved selections
if (!bookingDraft) {
    document.getElementById("route-title").textContent = "Booking details unavailable";
    document.getElementById("route-date").textContent = "Return to your flight search and try again.";
} else {
    const seatNumbers = Array.isArray(bookingDraft.seatNumbers) ? bookingDraft.seatNumbers : [];
    const seatLabel = seatNumbers.length > 0 ? seatNumbers.join(", ") : "Not selected";

    document.getElementById("route-title").textContent = `${bookingDraft.from} → ${bookingDraft.to}`;
    document.getElementById("route-date").textContent = formatRouteDate(bookingDraft.date);
    document.getElementById("ext-seat").textContent = `Seat${seatNumbers.length === 1 ? "" : "s"} ${seatLabel} reserved`;
    document.getElementById("sum-seat").textContent = `Seat${seatNumbers.length === 1 ? "" : "s"} ${seatLabel}`;
    document.getElementById("sum-cabin").textContent = `${bookingDraft.cabin ?? "Economy"} fare`;
    document.getElementById("sum-base").textContent = "Calculated at checkout";
    document.getElementById("tot").textContent = "Calculated at checkout";

    const savedAddOns = bookingDraft.addOns ?? {};

    if (savedAddOns.baggage) {
        const bagOption = document.querySelector(`input[name="bag"][data-label="${savedAddOns.baggage}"]`);
        if (bagOption) bagOption.checked = true;
    }

    if (savedAddOns.meal) {
        const mealOption = document.querySelector(`input[name="meal"][data-label="${savedAddOns.meal}"]`);
        if (mealOption) mealOption.checked = true;
    }

    if (savedAddOns.insurance) {
        const insuranceOption = document.querySelector(`input[name="ins"][data-label="${savedAddOns.insurance}"]`);
        if (insuranceOption) insuranceOption.checked = true;
    }

    if (Array.isArray(savedAddOns.extras)) {
        for (let i = 0; i < savedAddOns.extras.length; i++) {
            const extraOption = document.querySelector(`input[type="checkbox"][data-label="${savedAddOns.extras[i]}"]`);
            if (extraOption) extraOption.checked = true;
        }
    }
}

// listen for any input changes + update everything
const allInputs = document.querySelectorAll("input");
for (let i = 0; i < allInputs.length; i++) {
    allInputs[i].addEventListener("change", () => {
        saveSelections();
        updateSelectionsSummary();
        updateLiveTotalDisplay();
    });
}

// continue button - validate + go to payment page
continueButton.addEventListener("click", () => {
    handoffMessage.textContent = "";

    const updatedDraft = saveSelections();

    if (!updatedDraft) {
        handoffMessage.textContent = "Booking details are missing. Please restart your booking.";
        return;
    }

    const missingItems = [];

    if (typeof updatedDraft.userId !== "number") missingItems.push("a signed-in user");
    if (!updatedDraft.flightId) missingItems.push("a selected flight");
    if (!Array.isArray(updatedDraft.passengers) || updatedDraft.passengers.length === 0) missingItems.push("passenger details");
    if (!Array.isArray(updatedDraft.seatNumbers) || updatedDraft.seatNumbers.length === 0) missingItems.push("selected seats");
    if (updatedDraft.holdId === null || updatedDraft.holdId === undefined || updatedDraft.holdId === "") missingItems.push("a seat hold");
    if (!updatedDraft.holdExpiryTime) missingItems.push("a hold expiry time");

    if (missingItems.length > 0) {
        handoffMessage.textContent = `Complete ${missingItems.join(", ")} before continuing to payment.`;
        return;
    }

    window.location.href = "/payment";
});

// back btn -  go back to seatmap
backButton.addEventListener("click", () => {
    saveSelections();
    window.location.href = "/seatmap";
});

// kick everything off
populateAccountCard();
applyPricingDisplayMode();
saveSelections();
updateSelectionsSummary();
updateLiveTotalDisplay();

// refmt all the row prices on load using current currency
const rows = document.querySelectorAll(".row");
rows.forEach(row => {
    const input = row.querySelector("input[data-price]");
    const rp = row.querySelector(".rp");
    if (!input || !rp) return;
    if (rp.querySelector("s")) return;
    rp.textContent = fmt(parseFloat(input.dataset.price));
});