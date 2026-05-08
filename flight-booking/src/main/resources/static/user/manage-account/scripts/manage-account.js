(() => {
    "use strict";

    /** This page is only available to signed-in users, so missing session data redirects back to login. */
    const sessionId = sessionStorage.getItem("sessionId");
    const userId = sessionStorage.getItem("userId");

    if (!sessionId || !userId) {
        window.location.href = "/log_in";
        return;
    }

    /** Loads loyalty totals and tier progress, then updates the account summary and milestone UI. */
    async function loadAccountData() {
        const res = await fetch(`/api/account-summary?userId=${encodeURIComponent(userId)}`);
        if (!res.ok) return;
        const data = await res.json();
        document.getElementById("membership-number").textContent = data.membershipNumber;
        document.getElementById("membership-tier").textContent = data.membershipTier;
        document.getElementById("loyalty-points").textContent = data.loyaltyPoints.toLocaleString();

        // grab the users current points
        const points = data.loyaltyPoints;

// the points needed for each tier
        const silverThreshold = 10000;
        const goldThreshold = 20000;
        const platinumThreshold = 35000;

// remove active styling from all tier cards first
        document.querySelectorAll(".tier-card").forEach(c => c.classList.remove("active-tier"));

// these will be filled in depending on which tier the user is in
        let progressPct = 0;
        let progressText = "";

// check which tier the user is in and set the progress accordingly
        if (points >= platinumThreshold) {

            // user is platinum -- highest tier so progress is full
            document.getElementById("tier-platinum").classList.add("active-tier");
            progressPct = 100;
            progressText = "You have reached Platinum — the highest tier.";

        } else if (points >= goldThreshold) {

            // user is gold -- work out how far through to platinum they are
            document.getElementById("tier-gold").classList.add("active-tier");
            const pointsIntoGold = points - goldThreshold;
            const pointsNeededForPlatinum = platinumThreshold - goldThreshold;
            const goldProgress = (pointsIntoGold / pointsNeededForPlatinum) * 33;
            progressPct = 66 + goldProgress;
            progressText = `${(platinumThreshold - points).toLocaleString()} points to Platinum`;

        } else if (points >= silverThreshold) {

            // user is silver -- work out how far through to gold they are
            document.getElementById("tier-silver").classList.add("active-tier");
            const pointsIntoSilver = points - silverThreshold;
            const pointsNeededForGold = goldThreshold - silverThreshold;
            const silverProgress = (pointsIntoSilver / pointsNeededForGold) * 33;
            progressPct = 33 + silverProgress;
            progressText = `${(goldThreshold - points).toLocaleString()} points to Gold`;

        } else {

            // user has no tier yet -- work out how far through to silver they are
            progressPct = (points / silverThreshold) * 33;
            progressText = `${(silverThreshold - points).toLocaleString()} points to Silver`;

        }

// update the progress bar width and the text below it
        document.getElementById("tier-progress-fill").style.width = progressPct + "%";
        document.getElementById("tier-progress-text").textContent = progressText;
    }

    /** Prefills the editable profile form with the latest server-side user details. */
    async function loadUserDetails() {
        const res = await fetch(`/api/user/details?userId=${encodeURIComponent(userId)}`);
        if (!res.ok) return;
        const data = await res.json();
        document.getElementById("firstName").value = data.firstName;
        document.getElementById("lastName").value = data.lastName;
        document.getElementById("dateOfBirth").value = data.dateOfBirth;
        document.getElementById("email").value = data.email;
    }

    /** Fetches previously booked flights and renders simple summary cards into the bookings area. */
    async function loadUserBookings() {

        // fetch the bookings from the server for this user
        const response = await fetch(`/api/user/bookings?userId=${encodeURIComponent(userId)}`);

        // if somthing went wrong with the request just stop
        if (!response.ok) return;

        // convert the response to json so we can use it
        const bookings = await response.json();

        // find the container on the page where we will put the bookings
        const container = document.getElementById("bookings-list");

        // if the user has no bookings show a message and stop
        if (bookings.length === 0) {
            container.innerHTML = "<p class='no-bookings'>No bookings found.</p>";
            return;
        }

        // loop through each booking and build a html card for it
        const allCards = [];

        for (const b of bookings) {

            // build the html for this booking card
            const card = `
            <div class="booking-card">
                <div class="booking-route">
                    <span class="booking-airport">${b.departureAirport}</span>
                    <span class="booking-arrow">→</span>
                    <span class="booking-airport">${b.arrivalAirport}</span>
                </div>
                <div class="booking-meta">
                    <span>${b.date}</span>
                    <span>${b.departureTime} – ${b.arrivalTime}</span>
                    <span class="booking-ref">Ref: ${b.bookingId}</span>
                </div>
            </div>
        `;

            allCards.push(card);
        }

        // join all the cards together and put them in the container
        container.innerHTML = allCards.join("");
    }

    /** Initial page load fetches account stats, profile values, and booking history in parallel. */
    loadAccountData();
    loadUserBookings();
    loadUserDetails();

    /** Saving the form submits one update request and reflects the result in the inline status message. */
    document.getElementById("edit-form").addEventListener("submit", async (e) => {
        e.preventDefault();
        const msg = document.getElementById("save-message");
        const btn = document.querySelector(".login-btn");

        btn.disabled = true;
        msg.textContent = "";
        msg.removeAttribute("data-state");

        const body = {
            userId: parseInt(userId),
            firstName: document.getElementById("firstName").value.trim(),
            lastName: document.getElementById("lastName").value.trim(),
            dateOfBirth: document.getElementById("dateOfBirth").value,
            email: document.getElementById("email").value.trim(),
        };

        try {
            const res = await fetch("/api/user/update", {
                method: "PUT",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify(body)
            });

            if (res.ok) {
                // Keep the shared navigation greeting in sync with the updated profile data.
                msg.textContent = "Changes saved successfully.";
                msg.dataset.state = "success";
                sessionStorage.setItem("firstName", body.firstName);
                sessionStorage.setItem("lastName", body.lastName);
            } else {
                msg.textContent = "Something went wrong. Please try again.";
                msg.dataset.state = "error";
            }
        } catch (_) {
            msg.textContent = "Network error. Please try again.";
            msg.dataset.state = "error";
        }

        btn.disabled = false;
    });
})();

// Made with the assistance of Claude Sonnit 4.8
