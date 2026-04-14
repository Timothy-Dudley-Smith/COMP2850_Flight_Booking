(() => {
    "use strict";

    const loginEndpoint = "/api/auth/login";

    const form = document.getElementById("email-form");
    const emailInput = document.getElementById("email");
    const passwordInput = document.getElementById("password");

    if (!form) {
        return;
    }

    form.addEventListener("submit", async (event) => {
        event.preventDefault();

        const email = (emailInput?.value ?? "").trim();
        const password = passwordInput?.value ?? "";

        if (!email || !password) {
            alert("Please enter your email and password.");
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

            const text = await response.text();
            let data;
            try { data = JSON.parse(text); } catch (_) {}

            if (!response.ok) {
                throw new Error(data?.message ?? data?.error ?? text ?? "Unable to sign in.");
            }

            sessionStorage.setItem("user", JSON.stringify(data));
            window.location.href = "/";

        } catch (error) {
            alert(error.message || "Unable to sign in.");
        }
    });
})();