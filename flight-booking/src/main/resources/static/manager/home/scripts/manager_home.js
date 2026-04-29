(async () => {
    const sessionId = sessionStorage.getItem("sessionId");
    if (!sessionId) {
        window.location.href = "/log_in";
        return;
    }

    const sessionCheck = await fetch(`/api/auth/session?sessionId=${encodeURIComponent(sessionId)}`);
    const sessionData = await sessionCheck.json();
    if (!sessionCheck.ok || sessionData.role !== "MANAGER") {
        window.location.href = "/log_in";
        return;
    }

    "use strict";


    const todayLabel = document.getElementById("today-label");

    const sendManagerEmailButton = document.getElementById("send-manager-email-btn");
    const managerEmailTo = document.getElementById("manager-email-to");
    const managerEmailSubject = document.getElementById("manager-email-subject");
    const managerEmailMessage = document.getElementById("manager-email-message");
    const managerEmailStatus = document.getElementById("manager-email-status");

    if (sendManagerEmailButton) {
        sendManagerEmailButton.addEventListener("click", async () => {
            const sessionId = sessionStorage.getItem("sessionId");
            const toEmail = managerEmailTo.value.trim();
            const subject = managerEmailSubject.value.trim();
            const message = managerEmailMessage.value.trim();

            managerEmailStatus.textContent = "";

            if (!sessionId) {
                managerEmailStatus.textContent = "You must be logged in as a manager.";
                return;
            }

            if (toEmail === "") {
                managerEmailStatus.textContent = "Enter a customer email.";
                return;
            }

            if (subject === "") {
                managerEmailStatus.textContent = "Enter a subject.";
                return;
            }

            if (message === "") {
                managerEmailStatus.textContent = "Enter a message.";
                return;
            }

            try {
                sendManagerEmailButton.disabled = true;

                const response = await fetch(`/api/manager/send-email?sessionId=${encodeURIComponent(sessionId)}`, {
                    method: "POST",
                    headers: {
                        "Content-Type": "application/json"
                    },
                    body: JSON.stringify({
                        toEmail,
                        subject,
                        message
                    })
                });

                const text = await response.text();
                let result = null;

                try {
                    result = text ? JSON.parse(text) : null;
                } catch {
                    result = null;
                }

                if (!response.ok || !result?.success) {
                    managerEmailStatus.textContent = result?.message ?? text ?? "Failed to send email.";
                    return;
                }

                managerEmailStatus.textContent = "Email sent successfully.";
                managerEmailTo.value = "";
                managerEmailSubject.value = "";
                managerEmailMessage.value = "";
                loadSentEmails();
            } catch {
                managerEmailStatus.textContent = "Unable to send email right now.";
            } finally {
                sendManagerEmailButton.disabled = false;
            }
        });
    }

    const createPromoButton = document.getElementById("create-promo-btn");
    const promoCreateCode = document.getElementById("promo-create-code");
    const promoCreateType = document.getElementById("promo-create-type");
    const promoCreateValue = document.getElementById("promo-create-value");
    const promoCreateStatus = document.getElementById("promo-create-status");

    if (createPromoButton) {
        createPromoButton.addEventListener("click", async () => {
            const sessionId = sessionStorage.getItem("sessionId");
            const code = promoCreateCode.value.trim();
            const discountType = promoCreateType.value;
            const discountValue = Number(promoCreateValue.value);

            promoCreateStatus.textContent = "";

            if (!sessionId) {
                promoCreateStatus.textContent = "You must be logged in as a manager.";
                return;
            }

            if (code === "") {
                promoCreateStatus.textContent = "Enter a promo code.";
                return;
            }

            if (!discountType) {
                promoCreateStatus.textContent = "Select a discount type.";
                return;
            }

            if (!Number.isFinite(discountValue) || discountValue <= 0) {
                promoCreateStatus.textContent = "Enter a valid discount value.";
                return;
            }

            try {
                createPromoButton.disabled = true;

                const response = await fetch(`/api/manager/promo-codes?sessionId=${encodeURIComponent(sessionId)}`, {
                    method: "POST",
                    headers: {
                        "Content-Type": "application/json"
                    },
                    body: JSON.stringify({
                        code,
                        discountType,
                        discountValue
                    })
                });

                const text = await response.text();
                let result = null;

                try {
                    result = text ? JSON.parse(text) : null;
                } catch {
                    result = null;
                }

                if (!response.ok || !result?.success) {
                    promoCreateStatus.textContent = result?.message ?? text ?? "Failed to create promo code.";
                    return;
                }

                promoCreateStatus.textContent = "Promo code created successfully.";
                promoCreateCode.value = "";
                promoCreateType.value = "PERCENTAGE";
                promoCreateValue.value = "";
            } catch {
                promoCreateStatus.textContent = "Unable to create promo code right now.";
            } finally {
                createPromoButton.disabled = false;
            }
        });
    }

    if (todayLabel) {
        const today = new Date();
        todayLabel.textContent = today.toLocaleDateString("en-GB", {
            weekday: "long",
            day: "numeric",
            month: "long",
            year: "numeric"
        });
    }

    let sentEmails = [];
    let emailHistoryVisible = false;

    async function loadSentEmails() {
        const list = document.getElementById("sent-email-list");
        if (!list) return;

        const response = await fetch("/api/manager/sent-emails");
        sentEmails = await response.json();

        renderSentEmails();
    }

    const toggleEmailHistoryButton = document.getElementById("toggle-email-history-btn");
    const emailHistorySearch = document.getElementById("email-history-search");
    const sentEmailList = document.getElementById("sent-email-list");

    if (toggleEmailHistoryButton && sentEmailList && emailHistorySearch) {
        toggleEmailHistoryButton.addEventListener("click", () => {
            emailHistoryVisible = !emailHistoryVisible;

            sentEmailList.style.display = emailHistoryVisible ? "block" : "none";
            emailHistorySearch.style.display = emailHistoryVisible ? "block" : "none";

            toggleEmailHistoryButton.textContent = emailHistoryVisible
                ? "Hide Email History"
                : "Show Email History";

            if (emailHistoryVisible) {
                renderSentEmails();
            }
        });

        emailHistorySearch.addEventListener("input", renderSentEmails);
    }

    function renderSentEmails() {
        const list = document.getElementById("sent-email-list");
        const searchInput = document.getElementById("email-history-search");
        if (!list) return;

        const searchValue = searchInput?.value.trim().toLowerCase() ?? "";

        const filteredEmails = sentEmails.filter(email => {
            if (searchValue === "") return true;
            return email.toEmail.toLowerCase().includes(searchValue);
        });

        if (filteredEmails.length === 0) {
            list.innerHTML = "<p>No matching sent emails found.</p>";
            return;
        }

        list.innerHTML = `
            <h3>Sent Email History</h3>
            ${filteredEmails.map(email => `
                <div style="border: 1px solid #ddd; border-radius: 10px; padding: 14px; margin-bottom: 12px; background: #f8fafc;">
                    <p><strong>To:</strong> ${email.toEmail}</p>
                    <p><strong>Subject:</strong> ${email.subject}</p>
                    <p><strong>Message:</strong> ${email.message}</p>
                    <p><strong>Sent at:</strong> ${email.sentAt}</p>
                </div>
            `).join("")}
        `;
    }
loadSentEmails();
})();
