(() => {
    "use strict";

    /** Handles the guest add-ons step by restoring booking state, recalculating totals, and preparing checkout handoff data. */
    const GUEST_SEAT_PRICE = 20;
    const GUEST_WIFI_PRICE = 8.99;

    const bookingDraftRaw = sessionStorage.getItem("bookingDraft");
    let bookingDraft = bookingDraftRaw ? JSON.parse(bookingDraftRaw) : null;

    const continueButton = document.getElementById("continue-button");
    const backButton = document.getElementById("back-button");
    const handoffMessage = document.getElementById("handoff-message");
    const guestEmailInput = document.getElementById("guest-email");
    const EMAIL_REGEX = /^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/;

    if (bookingDraft && typeof bookingDraft.baseFareTotal !== "number") {
        bookingDraft.baseFareTotal = Number(bookingDraft.totalPrice) || 0;
    }

    /** Formats the selected flight date for the route summary banner. */
    function formatRouteDate(date) {
        if (!date) {
            return "From today onwards";
        }

        const parsedDate = new Date(date);
        if (Number.isNaN(parsedDate.getTime())) {
            return "From today onwards";
        }

        return parsedDate.toLocaleDateString("en-GB", {
            day: "numeric",
            month: "long"
        });
    }

    /** Uses the first passenger as the display name for the guest overview card. */
    function getPassengerOneName() {
        if (!bookingDraft || !Array.isArray(bookingDraft.passengers) || bookingDraft.passengers.length === 0) {
            return "Guest traveller";
        }

        const passengerOne = bookingDraft.passengers[0];
        const first = passengerOne.firstName || "";
        const last = passengerOne.lastName || "";
        return `${first} ${last}`.trim() || "Guest traveller";
    }

    /** Generates the small initials badge shown beside the guest name. */
    function getInitials(name) {
        if (!name) {
            return "--";
        }

        const parts = name.trim().split(/\s+/);
        if (parts.length === 1) {
            return parts[0].slice(0, 2).toUpperCase();
        }

        return `${parts[0][0] ?? ""}${parts[1][0] ?? ""}`.toUpperCase();
    }

    function getSelectedOption(name) {
        const input = document.querySelector(`input[name="${name}"]:checked`);
        if (!input || !input.dataset.label) {
            return null;
        }

        return input.dataset.label;
    }

    function getSelectedRadioPrice(name) {
        const input = document.querySelector(`input[name="${name}"]:checked`);
        return input ? Number(input.dataset.price || 0) : 0;
    }

    function getSelectedExtras() {
        const extras = [];
        const inputs = document.querySelectorAll('input[type="checkbox"]:checked[data-label]');

        for (let i = 0; i < inputs.length; i++) {
            extras.push(inputs[i].dataset.label);
        }

        return extras;
    }

    function getCheckedExtrasTotal() {
        const inputs = document.querySelectorAll('input[type="checkbox"]:checked[data-price]');
        let total = 0;

        for (let i = 0; i < inputs.length; i++) {
            total += Number(inputs[i].dataset.price) || 0;
        }

        return total;
    }

    function getGuestEmailValue() {
        return (guestEmailInput?.value || "").trim();
    }

    function isGuestEmailValid(email) {
        return EMAIL_REGEX.test(email);
    }

    /** Creates the simplified payload that later payment and booking APIs consume. */
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
            totalPrice: draft.totalPrice,
            guestEmail: draft.guestEmail,
            bookingType: "guest"
        };
    }

    function getPassengerCount() {
        const passengers = Array.isArray(bookingDraft?.passengers) ? bookingDraft.passengers : [];
        const passengerCount = passengers.length > 0
            ? passengers.length
            : Math.max(1, Number(bookingDraft?.passengerCount ?? 1));

        return passengerCount;
    }

    /** Adds the base fare, reserved seat, wi-fi, and current add-on selections into one total. */
    function calculateLiveTotal() {
        const baseFare = Number(bookingDraft?.baseFareTotal) || 0;
        const passengerCount = getPassengerCount();

        const baggagePrice = getSelectedRadioPrice("bag");
        const mealPrice = getSelectedRadioPrice("meal");
        const insurancePrice = getSelectedRadioPrice("ins");
        const extrasPrice = getCheckedExtrasTotal();

        const addOnsTotalPerPassenger =
            baggagePrice +
            mealPrice +
            insurancePrice +
            extrasPrice +
            GUEST_SEAT_PRICE +
            GUEST_WIFI_PRICE;

        return baseFare + (addOnsTotalPerPassenger * passengerCount);
    }

    /** Rebuilds the summary rows so the sidebar matches the saved add-on choices. */
    function updateSelectionsSummary() {
        const lines = document.getElementById("lines");
        const selections = [];

        if (bookingDraft?.addOns?.baggage) {
            selections.push(bookingDraft.addOns.baggage);
        }

        if (bookingDraft?.addOns?.meal) {
            selections.push(bookingDraft.addOns.meal);
        }

        if (bookingDraft?.addOns?.insurance) {
            selections.push(bookingDraft.addOns.insurance);
        }

        if (Array.isArray(bookingDraft?.addOns?.extras)) {
            for (let i = 0; i < bookingDraft.addOns.extras.length; i++) {
                selections.push(bookingDraft.addOns.extras[i]);
            }
        }

        lines.innerHTML = selections.map((selection) => `
            <div class="sr">
                <span>${selection}</span>
                <b>Selected</b>
            </div>
        `).join("");
    }

    /** Normalizes the current inputs and persists them to session storage for the next booking step. */
    function saveSelections() {
        if (!bookingDraft) {
            return null;
        }

        const passengers = Array.isArray(bookingDraft.passengers) ? bookingDraft.passengers : [];
        const seatNumbers = Array.isArray(bookingDraft.seatNumbers) ? bookingDraft.seatNumbers : [];

        bookingDraft = {
            ...bookingDraft,
            flightId: bookingDraft.flightId ?? "",
            from: bookingDraft.from ?? "",
            to: bookingDraft.to ?? "",
            date: bookingDraft.date ?? "",
            guestEmail: getGuestEmailValue(),
            passengerCount: passengers.length > 0 ? passengers.length : Math.max(1, Number(bookingDraft.passengerCount ?? 1)),
            cabin: bookingDraft.cabin ?? "Economy",
            passengers,
            seatNumbers,
            baseFareTotal: Number(bookingDraft.baseFareTotal) || 0,
            addOns: {
                baggage: getSelectedOption("bag"),
                meal: getSelectedOption("meal"),
                insurance: getSelectedOption("ins"),
                extras: getSelectedExtras()
            },
            bookingType: "guest"
        };

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
                totalPrice: bookingDraft.totalPrice,
                guestEmail: bookingDraft.guestEmail
            }));
        }

        return bookingDraft;
    }

    /** Refreshes the fare breakdown after any add-on selection changes. */
    function updateLiveTotalDisplay() {
        const baseFare = Number(bookingDraft?.baseFareTotal) || 0;
        const total = calculateLiveTotal();

        document.getElementById("sum-base").textContent = fmt(baseFare);
        document.getElementById("sum-seat-price").textContent =
            `${fmt(GUEST_SEAT_PRICE)} × ${getPassengerCount()}`;

        document.getElementById("sum-wifi-price").textContent =
            `${fmt(GUEST_WIFI_PRICE)} × ${getPassengerCount()}`;
        document.getElementById("tot").textContent = fmt(total);
    }

    /** Reapplies previously chosen add-ons when the user revisits this page. */
    function restoreSavedSelections() {
        const savedAddOns = bookingDraft?.addOns ?? {};

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

    /** Populates the page from the booking draft and disables progression if the draft is missing. */
    function initializePage() {
        if (!bookingDraft) {
            document.getElementById("route-title").textContent = "Booking details unavailable";
            document.getElementById("route-date").textContent = "Return to your flight search and try again.";
            continueButton.disabled = true;
            return;
        }

        const passengerName = getPassengerOneName();
        const seatNumbers = Array.isArray(bookingDraft.seatNumbers) ? bookingDraft.seatNumbers : [];
        const seatLabel = seatNumbers.length > 0 ? seatNumbers.join(", ") : "Not selected";

        document.getElementById("guest-name").textContent = passengerName;
        document.getElementById("guest-initials").textContent = getInitials(passengerName);
        document.getElementById("route-title").textContent = `${bookingDraft.from} → ${bookingDraft.to}`;
        document.getElementById("route-date").textContent = formatRouteDate(bookingDraft.date);
        document.getElementById("ext-seat").textContent = `Seat${seatNumbers.length === 1 ? "" : "s"} ${seatLabel} reserved`;
        document.getElementById("sum-seat").textContent = `Seat${seatNumbers.length === 1 ? "" : "s"} ${seatLabel}`;
        document.getElementById("sum-cabin").textContent = `${bookingDraft.cabin ?? "Economy"} fare`;
        if (guestEmailInput) {
            guestEmailInput.value = bookingDraft.guestEmail || "";
        }

        restoreSavedSelections();
        saveSelections();
        updateSelectionsSummary();
        updateLiveTotalDisplay();
    }

    /** Every add-on input updates the persisted draft and sidebar in real time. */
    const allInputs = document.querySelectorAll("input");
    for (let i = 0; i < allInputs.length; i++) {
        allInputs[i].addEventListener("change", () => {
            saveSelections();
            updateSelectionsSummary();
            updateLiveTotalDisplay();
        });
    }

    /** Continue validates the guest handoff data before moving into payment. */
    continueButton.addEventListener("click", () => {
        handoffMessage.textContent = "";

        const updatedDraft = saveSelections();
        if (!updatedDraft) {
            handoffMessage.textContent = "Booking details are missing. Please restart your booking.";
            return;
        }

        const missingItems = [];

        if (!updatedDraft.flightId) missingItems.push("a selected flight");
        if (!Array.isArray(updatedDraft.passengers) || updatedDraft.passengers.length === 0) missingItems.push("passenger details");
        if (!Array.isArray(updatedDraft.seatNumbers) || updatedDraft.seatNumbers.length === 0) missingItems.push("selected seats");
        if (updatedDraft.holdId === null || updatedDraft.holdId === undefined || updatedDraft.holdId === "") missingItems.push("a seat hold");
        if (!updatedDraft.holdExpiryTime) missingItems.push("a hold expiry time");
        if (!updatedDraft.guestEmail) missingItems.push("a confirmation email");

        if (missingItems.length > 0) {
            handoffMessage.textContent = `Complete ${missingItems.join(", ")} before continuing to payment.`;
            return;
        }

        if (!isGuestEmailValid(updatedDraft.guestEmail)) {
            handoffMessage.textContent = "Enter a valid email address for your booking confirmation.";
            guestEmailInput?.focus();
            return;
        }

        window.location.href = "/payment";
    });

    /** Back preserves the current selections before returning to seat selection. */
    backButton.addEventListener("click", () => {
        saveSelections();
        window.location.href = "/seatmap";
    });

    initializePage();
})();
