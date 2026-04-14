(() => {
    "use strict";

    const registerEndpoint = "/api/auth/register";

    const form = document.getElementById("register-form");
    const message = document.getElementById("register-message");
    const submitButton = form?.querySelector('button[type="submit"]');

    function setMessage(text, state) {
        if (!message) {
            return;
        }

        message.textContent = text;
        message.dataset.state = state;
    }

    function buildPayload(formElement) {
        const formData = new FormData(formElement);

        return {
            firstName: (formData.get("firstName") ?? "").toString().trim(),
            lastName: (formData.get("lastName") ?? "").toString().trim(),
            email: (formData.get("email") ?? "").toString().trim(),
            password: (formData.get("password") ?? "").toString(),
            dateOfBirth: (formData.get("dateOfBirth") ?? "").toString()
        };
    }

    function validatePayload(payload) {
        if (!payload.firstName || !payload.lastName) {
            return "Please enter your first and last name.";
        }

        if (!payload.email) {
            return "Please enter your email address.";
        }

        if (!payload.password) {
            return "Please enter a password.";
        }

        if (payload.password.length < 8) {
            return "Password must be at least 8 characters long.";
        }

        return null;
    }

    async function submitRegistration(payload) {
        return fetch(registerEndpoint, {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify(payload)
        });
    }

    if (!form) {
        return;
    }

    form.addEventListener("submit", async (event) => {
        event.preventDefault();

        const payload = buildPayload(form);
        const validationError = validatePayload(payload);

        if (validationError) {
            setMessage(validationError, "error");
            return;
        }

        if (submitButton) {
            submitButton.disabled = true;
        }

        setMessage("Creating your account...", "info");

        try {
            const response = await submitRegistration(payload);

            if (!response.ok) {
                let errorMessage = "Unable to create your account.";

                const rawBody = await response.text();

                if (rawBody) {
                    try {
                        const responseBody = JSON.parse(rawBody);
                        errorMessage = responseBody.message ?? responseBody.error ?? errorMessage;
                    } catch {
                        errorMessage = rawBody;
                    }
                }

                throw new Error(errorMessage);
            }

            setMessage("Account created successfully. Redirecting to sign in...", "success");
            form.reset();

            window.setTimeout(() => {
                window.location.href = "/log_in";
            }, 1200);
        } catch (error) {
            setMessage(error.message || "Unable to create your account.", "error");
        } finally {
            if (submitButton) {
                submitButton.disabled = false;
            }
        }
    });
})();
