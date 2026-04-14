(() => {
    "use strict";

    const loginEndpoint = "/api/auth/login";

    const form = document.getElementById("email-form");
    const emailInput = document.getElementById("email");
    const passwordInput = document.getElementById("password");
    const message = document.getElementById("login-message");

    function setMessage(text, state) {
        if (!message) return;
        message.textContent = text;
        message.dataset.state = state;
    }

    if (!form) {
        return;
    }

    form.addEventListener("submit", async (event) => {
        event.preventDefault();

        const email = (emailInput?.value ?? "").trim();
        const password = passwordInput?.value ?? "";

        if (!email || !password) {
            setMessage("Please enter your email and password.", "error");
            return;
        }

        try {
            const response = await fetch(loginEndpoint, {
                method: "POST",
                headers: {
                    "Content-Type": "application/json"
                },
                body: JSON.stringify({ email, password })
            });

            const rawBody = await response.text();
            let data = {};

            if (rawBody) {
                try {
                    data = JSON.parse(rawBody);
                } catch {
                    throw new Error(rawBody);
                }
            }

            if (!response.ok || !data.success) {
                throw new Error(data.error || data.message || "Unable to sign in.");
            }

            sessionStorage.setItem("sessionId", data.sessionId);
            sessionStorage.setItem("userId", data.userId);
            sessionStorage.setItem("userEmail", data.email);
            sessionStorage.setItem("firstName", data.firstName);
            sessionStorage.setItem("lastName", data.lastName);
            sessionStorage.setItem("role", data.role);

            setMessage("Login successful. Redirecting...", "success");

            window.setTimeout(() => {
                window.location.href = "/";
            }, 800);

        } catch (error) {
            setMessage(error.message || "Unable to sign in.", "error");
        }
    });
})();