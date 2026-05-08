/** Creates a support ticket from the current form values and reports the result inline on the page. */
async function createTicket(event) {
    event.preventDefault();

    const result = document.getElementById("result");

    /** Build the API payload directly from the current form controls. */
    const ticketData = {
        bookingId: parseInt(document.getElementById("bookingId").value),
        customerName: document.getElementById("customerName").value,
        customerEmail: document.getElementById("customerEmail").value,
        requestType: document.getElementById("requestType").value,
        message: document.getElementById("message").value
    };

    try {
        // Submit the support request to the backend ticket endpoint.
        const response = await fetch("/api/tickets", {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify(ticketData)
        });

        const responseText = await response.text();

        if (response.ok) {
            // Clear the form after a successful submission so the user can start a new request cleanly.
            result.textContent = "Support request submitted successfully.";
            document.getElementById("ticketForm").reset();
        } else {
            // Surface any backend message directly when the request is rejected.
            result.textContent = responseText || "Failed to submit support request.";
        }
    } catch (error) {
        console.error("Failed to submit support request.", error);
        result.textContent = "Could not submit your request right now. Please try again.";
    }
}

/** Wire the form submit event to the async ticket creation flow. */
document.getElementById("ticketForm").addEventListener("submit", createTicket);


// nav bar login/logout state
const sessionId = sessionStorage.getItem("sessionId");
const userId = sessionStorage.getItem("userId");
const firstName = sessionStorage.getItem("firstName");

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