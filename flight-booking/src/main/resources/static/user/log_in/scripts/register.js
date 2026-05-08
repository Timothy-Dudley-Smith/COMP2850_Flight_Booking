/**
 * Handles customer account creation on the registration page.
 *
 * The flow collects form data, validates the required fields locally, posts the
 * payload to the registration endpoint, and keeps all feedback inside the page.
 */
(() => {
    "use strict";

    const registerEndpoint = "/api/auth/register";

    const form = document.getElementById("register-form");
    const message = document.getElementById("register-message");
    const submitButton = form?.querySelector('button[type="submit"]');

    /**
     * Updates the inline feedback area with the latest validation, loading, or error state.
     */
    function setMessage(text, state) {
        if (!message) {
            return;
        }

        message.textContent = text;
        message.dataset.state = state;
    }

    /**
     * Reads the current form values and normalises them into the JSON payload expected by the API.
     */
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

    /**
     * Performs lightweight client-side validation before the registration request is sent.
     */
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

    /**
     * Sends the registration payload to the auth API and returns the raw fetch response for caller-side handling.
     */
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

        // Stop here when the form is incomplete so the user gets immediate feedback without an API call.
        if (validationError) {
            setMessage(validationError, "error");
            return;
        }

        if (submitButton) {
            submitButton.disabled = true;
        }

        setMessage("Creating your account...", "info");

        try {
            // The server owns final validation, so parse and surface any API error message directly to the page.
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
            // Covers both backend validation failures and lower-level network/request errors.
            setMessage(error.message || "Unable to create your account.", "error");
        } finally {
            // Re-enable the submit button after every outcome so the customer can retry if needed.
            if (submitButton) {
                submitButton.disabled = false;
            }
        }
    });
})();
