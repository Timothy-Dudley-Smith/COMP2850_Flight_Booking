(() => {
    "use strict";

    const welcomeMessage = document.getElementById("welcome-message");
    const loginLink = document.getElementById("login-link");
    const logoutButton = document.getElementById("logout-button");

    const sessionId = sessionStorage.getItem("sessionId");
    const firstName = sessionStorage.getItem("firstName");

    if (sessionId && firstName) {
        if (welcomeMessage) {
            welcomeMessage.textContent = `Welcome, ${firstName}`;
        }

        if (loginLink) {
            loginLink.style.display = "none";
        }

        if (logoutButton) {
            logoutButton.style.display = "inline-block";
        }
    }

    if (logoutButton) {
        logoutButton.addEventListener("click", async () => {
            const sessionId = sessionStorage.getItem("sessionId");

            try {
                if (sessionId) {
                    await fetch(`/api/auth/logout?sessionId=${encodeURIComponent(sessionId)}`, {
                        method: "POST"
                    });
                }
            } catch (_) {
            }

            sessionStorage.removeItem("sessionId");
            sessionStorage.removeItem("userId");
            sessionStorage.removeItem("userEmail");
            sessionStorage.removeItem("firstName");
            sessionStorage.removeItem("lastName");
            sessionStorage.removeItem("role");
            sessionStorage.removeItem("holdId");
            sessionStorage.removeItem("bookingId");

            window.location.href = "/log_in";
        });
    }
})();