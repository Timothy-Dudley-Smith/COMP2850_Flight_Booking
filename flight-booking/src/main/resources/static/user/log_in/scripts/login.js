/**
 * Handles the account login flow for the authentication page.
 *
 * The script validates the visible login form, submits credentials to the auth API,
 * then continues into the OTP verification step when the backend requires a second factor.
 */
(() => {
    "use strict";

    const form = document.getElementById("email-form");
    const emailInput = document.getElementById("email");
    const passwordInput = document.getElementById("password");
    const message = document.getElementById("login-message");
    const otpSection = document.getElementById("otp-section");
    const otpInput = document.getElementById("otp-input");
    const otpMessage = document.getElementById("otp-message");
    const otpSubmitBtn = document.getElementById("otp-submit-btn");

    let pendingEmail = null;

    /**
     * Writes the latest status or error message into the supplied feedback element.
     */
    function setMsg(el, text, state) {
        if (!el) return;
        el.textContent = text;
        el.dataset.state = state;
    }

    if (!form) return;

    form.addEventListener("submit", async (event) => {
        event.preventDefault();

        const email = (emailInput?.value ?? "").trim();
        const password = passwordInput?.value ?? "";

        // Block empty submissions before making an API request.
        if (!email || !password) {
            setMsg(message, "Please enter your email and password.", "error");
            return;
        }

        try {
            // Submit credentials to the primary auth endpoint and inspect the response body for API errors.
            const response = await fetch("/api/auth/login", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ email, password })
            });

            const rawBody = await response.text();
            let data = {};
            if (rawBody) {
                try { data = JSON.parse(rawBody); } catch { throw new Error(rawBody); }
            }

            if (!response.ok) throw new Error(data.error || data.message || "Unable to sign in.");

            // When the backend requires OTP, keep the email in memory for the follow-up verification call.
            if (data.otpRequired) {
                pendingEmail = email;
                form.classList.add("hidden");
                otpSection.classList.remove("hidden");
            }

        } catch (error) {
            // Show the backend message when available so validation and auth failures stay visible in-page.
            setMsg(message, error.message || "Unable to sign in.", "error");
        }
    });

    otpSubmitBtn?.addEventListener("click", async () => {
        const otp = (otpInput?.value ?? "").trim();

        // Avoid the verify request until the customer has entered a code.
        if (!otp) {
            setMsg(otpMessage, "Please enter your code.", "error");
            return;
        }

        try {
            // Complete the two-step login flow by sending the stored email and typed OTP to the verify endpoint.
            const response = await fetch("/api/auth/verify-otp", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ email: pendingEmail, otp })
            });

            const rawBody = await response.text();
            let data = {};
            if (rawBody) {
                try { data = JSON.parse(rawBody); } catch { throw new Error(rawBody); }
            }

            if (!response.ok || !data.success) throw new Error(data.error || data.message || "Invalid code.");

            // Persist the returned session and user details so the rest of the frontend can update its signed-in UI.
            sessionStorage.setItem("sessionId", data.sessionId);
            sessionStorage.setItem("userId", data.userId);
            sessionStorage.setItem("userEmail", data.email);
            sessionStorage.setItem("firstName", data.firstName);
            sessionStorage.setItem("lastName", data.lastName);
            sessionStorage.setItem("role", data.role);
            sessionStorage.setItem("user", JSON.stringify({ userId: Number(data.userId) }));

            setMsg(otpMessage, "Login successful. Redirecting...", "success");
            window.setTimeout(() => { window.location.href = "/"; }, 800);

        } catch (error) {
            // Keep verification failures in the OTP panel so the customer can retry without leaving the step.
            setMsg(otpMessage, error.message || "Invalid code.", "error");
        }
    });
})();
