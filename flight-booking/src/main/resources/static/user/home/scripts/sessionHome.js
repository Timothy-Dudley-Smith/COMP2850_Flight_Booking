/**
 * Syncs the shared customer header with the current browser session.
 *
 * The script updates welcome text, swaps login/logout controls, and shows the
 * manager-only support shortcut when the stored role allows it.
 */
(() => {
    "use strict";

    const welcomeMessage = document.getElementById("welcome-message");
    const loginLink = document.getElementById("login-link");
    const logoutButton = document.getElementById("logout-button");
    const supportLink = document.getElementById("support-link");

    const sessionId = sessionStorage.getItem("sessionId");
    const firstName = sessionStorage.getItem("firstName");

    /**
     * Apply the signed-in navigation state only when the minimum session details are present.
     */
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

        const role = sessionStorage.getItem("role");
        if (supportLink) {
            if (role === "MANAGER") {
                supportLink.style.display = "inline-block";
            } else {
                supportLink.style.display = "none";
            }
        }
    }

    /**
     * Attempt an API logout, then clear locally cached session data so every page
     * using the shared header returns to the signed-out state.
     */
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

            if (supportLink) {
                supportLink.style.display = "none";
            }

            window.location.href = "/log_in";
        });
    }
})();
