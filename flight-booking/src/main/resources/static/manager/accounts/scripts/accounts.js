(() => {
    "use strict";

    const accountsEndpoint = "/api/manager/users";
    const tableBody = document.getElementById("accounts-table-body");
    const searchInput = document.getElementById("account-search");
    const statusFilter = document.getElementById("account-status-filter");
    const refreshButton = document.getElementById("refresh-accounts");
    const accountsMessage = document.getElementById("accounts-message");

    const totalAccountsElement = document.getElementById("accounts-total");
    const activeAccountsElement = document.getElementById("accounts-active");
    const frozenAccountsElement = document.getElementById("accounts-frozen");
    const deletedAccountsElement = document.getElementById("accounts-deleted");
    const totalPointsElement = document.getElementById("accounts-total-points");

    let allAccounts = [];
    let busyUserId = null;
    const statusSortOrder = {
        ACTIVE: 0,
        FROZEN: 1,
        DELETED: 2
    };

    function escapeHtml(value) {
        return String(value ?? "")
            .replaceAll("&", "&amp;")
            .replaceAll("<", "&lt;")
            .replaceAll(">", "&gt;")
            .replaceAll("\"", "&quot;")
            .replaceAll("'", "&#39;");
    }

    function getSessionId() {
        return sessionStorage.getItem("sessionId")?.trim() ?? "";
    }

    function normaliseStatus(status) {
        return String(status ?? "UNKNOWN").trim().toUpperCase();
    }

    function setMessage(message, type = "") {
        if (!accountsMessage) {
            return;
        }

        accountsMessage.textContent = message;
        accountsMessage.className = "accounts-message";

        if (type) {
            accountsMessage.classList.add(type);
        }
    }

    function setSummaryValue(element, value) {
        if (!element) {
            return;
        }

        element.textContent = String(value);
    }

    function updateSummaryCards(accounts) {
        const totalAccounts = accounts.length;
        const activeAccounts = accounts.filter((account) => normaliseStatus(account.status) === "ACTIVE").length;
        const frozenAccounts = accounts.filter((account) => normaliseStatus(account.status) === "FROZEN").length;
        const deletedAccounts = accounts.filter((account) => normaliseStatus(account.status) === "DELETED").length;
        const totalPoints = accounts.reduce((sum, account) => sum + Number(account.loyaltyPoints ?? 0), 0);

        setSummaryValue(totalAccountsElement, totalAccounts);
        setSummaryValue(activeAccountsElement, activeAccounts);
        setSummaryValue(frozenAccountsElement, frozenAccounts);
        setSummaryValue(deletedAccountsElement, deletedAccounts);
        setSummaryValue(totalPointsElement, totalPoints.toLocaleString("en-GB"));
    }

    function getFilteredAccounts() {
        const query = searchInput?.value.trim().toLowerCase() ?? "";
        const selectedStatus = statusFilter?.value ?? "ALL";

        return allAccounts.filter((account) => {
            const fullName = `${account.firstName ?? ""} ${account.lastName ?? ""}`.trim().toLowerCase();
            const email = String(account.email ?? "").toLowerCase();
            const status = normaliseStatus(account.status);
            const matchesQuery = query === "" || fullName.includes(query) || email.includes(query);
            const matchesStatus = selectedStatus === "ALL" || status === selectedStatus;

            return matchesQuery && matchesStatus;
        });
    }

    function getStatusClass(status) {
        switch (normaliseStatus(status)) {
            case "ACTIVE":
                return "active";
            case "FROZEN":
                return "frozen";
            case "DELETED":
                return "deleted";
            default:
                return "unknown";
        }
    }

    function sortAccounts(accounts) {
        return [...accounts].sort((left, right) => {
            const leftStatus = statusSortOrder[normaliseStatus(left.status)] ?? 99;
            const rightStatus = statusSortOrder[normaliseStatus(right.status)] ?? 99;

            if (leftStatus !== rightStatus) {
                return leftStatus - rightStatus;
            }

            const leftName = `${left.lastName ?? ""} ${left.firstName ?? ""}`.trim().toLowerCase();
            const rightName = `${right.lastName ?? ""} ${right.firstName ?? ""}`.trim().toLowerCase();

            return leftName.localeCompare(rightName);
        });
    }

    function buildActionButtons(account) {
        const status = normaliseStatus(account.status);
        const userId = Number(account.userId);
        const isBusy = busyUserId === userId;

        if (status === "DELETED") {
            return `
                <button class="action-button restore" type="button" data-action="restore" data-user-id="${userId}" ${isBusy ? "disabled" : ""}>
                    Restore
                </button>
            `;
        }

        const freezeAction = status === "FROZEN"
            ? `
                <button class="action-button unfreeze" type="button" data-action="unfreeze" data-user-id="${userId}" ${isBusy ? "disabled" : ""}>
                    Unfreeze
                </button>
            `
            : `
                <button class="action-button freeze" type="button" data-action="freeze" data-user-id="${userId}" ${isBusy ? "disabled" : ""}>
                    Freeze
                </button>
            `;

        return `
            ${freezeAction}
            <button class="action-button delete" type="button" data-action="delete" data-user-id="${userId}" ${isBusy ? "disabled" : ""}>
                Delete
            </button>
        `;
    }

    function renderTable() {
        if (!tableBody) {
            return;
        }

        const filteredAccounts = getFilteredAccounts();

        if (filteredAccounts.length === 0) {
            tableBody.innerHTML = "<tr><td colspan=\"7\">No accounts match the current filters.</td></tr>";
            return;
        }

        tableBody.innerHTML = filteredAccounts.map((account) => {
            const userId = Number(account.userId);
            const status = normaliseStatus(account.status);
            const pointsDisabled = status === "DELETED" || busyUserId === userId;
            const firstName = escapeHtml(account.firstName ?? "");
            const lastName = escapeHtml(account.lastName ?? "");
            const email = escapeHtml(account.email ?? "");
            const role = escapeHtml(account.role ?? "");
            const statusLabel = escapeHtml(status);
            const points = Number(account.loyaltyPoints ?? 0).toLocaleString("en-GB");

            return `
                <tr>
                    <td class="user-cell">
                        <strong>${firstName} ${lastName}</strong>
                        <span>User ID: ${userId}</span>
                    </td>
                    <td class="email-cell">
                        <strong>${email}</strong>
                        <span>Primary contact email</span>
                    </td>
                    <td>${role}</td>
                    <td>
                        <span class="status-pill ${getStatusClass(status)}">${statusLabel}</span>
                    </td>
                    <td>
                        <span class="points-value">${points}</span>
                    </td>
                    <td>
                        <form class="points-form" data-user-id="${userId}">
                            <input
                                class="points-input"
                                type="number"
                                min="1"
                                step="1"
                                placeholder="Points"
                                data-points-input="${userId}"
                                ${pointsDisabled ? "disabled" : ""}
                            >
                            <button class="primary-button" type="submit" ${pointsDisabled ? "disabled" : ""}>
                                Add
                            </button>
                        </form>
                    </td>
                    <td class="actions-cell">
                        ${buildActionButtons(account)}
                    </td>
                </tr>
            `;
        }).join("");
    }

    async function fetchJsonOrText(response) {
        const text = await response.text();

        if (!text) {
            return "";
        }

        try {
            return JSON.parse(text);
        } catch {
            return text;
        }
    }

    async function loadAccounts(options = {}) {
        const { keepMessage = false, successMessage = "" } = options;
        const sessionId = getSessionId();

        if (!sessionId) {
            allAccounts = [];
            updateSummaryCards(allAccounts);
            renderTable();
            setMessage("You must be logged in as a manager to view account controls.", "error");
            return;
        }

        if (!keepMessage) {
            setMessage("Loading account data.");
        }

        try {
            refreshButton.disabled = true;

            const response = await fetch(`${accountsEndpoint}?sessionId=${encodeURIComponent(sessionId)}`);
            const result = await fetchJsonOrText(response);

            if (!response.ok || !Array.isArray(result)) {
                allAccounts = [];
                updateSummaryCards(allAccounts);
                renderTable();
                setMessage(typeof result === "string" && result ? result : "Unable to load manager accounts.", "error");
                return;
            }

            allAccounts = sortAccounts(result);
            updateSummaryCards(allAccounts);
            renderTable();
            setMessage(successMessage || `Showing ${getFilteredAccounts().length} account${getFilteredAccounts().length === 1 ? "" : "s"}.`, "success");
        } catch {
            allAccounts = [];
            updateSummaryCards(allAccounts);
            renderTable();
            setMessage("Unable to load manager accounts right now.", "error");
        } finally {
            refreshButton.disabled = false;
        }
    }

    async function runAccountAction(userId, action, message) {
        const sessionId = getSessionId();

        if (!sessionId) {
            setMessage("You must be logged in as a manager to manage accounts.", "error");
            return;
        }

        try {
            busyUserId = userId;
            renderTable();
            setMessage(`${message}...`);

            const response = await fetch(`/api/manager/users/${encodeURIComponent(userId)}/${action}?sessionId=${encodeURIComponent(sessionId)}`, {
                method: "POST"
            });

            const result = await fetchJsonOrText(response);

            if (!response.ok) {
                const errorMessage = typeof result === "string" && result ? result : `Unable to ${action} this account.`;
                setMessage(errorMessage, "error");
                return;
            }

            await loadAccounts({
                keepMessage: true,
                successMessage: "Account updated successfully."
            });
        } catch {
            setMessage(`Unable to ${action} this account right now.`, "error");
        } finally {
            busyUserId = null;
            renderTable();
        }
    }

    async function addPointsToAccount(userId, points) {
        const sessionId = getSessionId();

        if (!sessionId) {
            setMessage("You must be logged in as a manager to add points.", "error");
            return;
        }

        if (!Number.isInteger(points) || points <= 0) {
            setMessage("Enter a points value greater than 0.", "error");
            return;
        }

        try {
            busyUserId = userId;
            renderTable();
            setMessage("Adding loyalty points.");

            const response = await fetch(`/api/manager/users/${encodeURIComponent(userId)}/points?sessionId=${encodeURIComponent(sessionId)}`, {
                method: "POST",
                headers: {
                    "Content-Type": "application/json"
                },
                body: JSON.stringify({ points })
            });

            const result = await fetchJsonOrText(response);
            const wasSuccessful = result === true || result === "true" || result?.success === true;

            if (!response.ok || !wasSuccessful) {
                const errorMessage = typeof result === "string" && result && result !== "false"
                    ? result
                    : "Unable to add points to this account.";
                setMessage(errorMessage, "error");
                return;
            }

            await loadAccounts({
                keepMessage: true,
                successMessage: "Points added successfully."
            });
        } catch {
            setMessage("Unable to add points right now.", "error");
        } finally {
            busyUserId = null;
            renderTable();
        }
    }

    if (searchInput) {
        searchInput.addEventListener("input", () => {
            renderTable();
            setMessage(`Showing ${getFilteredAccounts().length} account${getFilteredAccounts().length === 1 ? "" : "s"}.`, "success");
        });
    }

    if (statusFilter) {
        statusFilter.addEventListener("change", () => {
            renderTable();
            setMessage(`Showing ${getFilteredAccounts().length} account${getFilteredAccounts().length === 1 ? "" : "s"}.`, "success");
        });
    }

    if (refreshButton) {
        refreshButton.addEventListener("click", () => {
            loadAccounts();
        });
    }

    if (tableBody) {
        tableBody.addEventListener("click", (event) => {
            const button = event.target.closest("[data-action]");

            if (!button) {
                return;
            }

            const action = button.getAttribute("data-action");
            const userId = Number(button.getAttribute("data-user-id"));

            if (!action || !Number.isFinite(userId)) {
                return;
            }

            if (action === "delete" && !window.confirm("Mark this account as deleted?")) {
                return;
            }

            runAccountAction(userId, action, action.charAt(0).toUpperCase() + action.slice(1));
        });

        tableBody.addEventListener("submit", (event) => {
            const form = event.target.closest(".points-form");

            if (!form) {
                return;
            }

            event.preventDefault();

            const userId = Number(form.getAttribute("data-user-id"));
            const input = form.querySelector(".points-input");
            const points = Number(input?.value);

            if (!Number.isFinite(userId)) {
                return;
            }

            addPointsToAccount(userId, points);

            if (input) {
                input.value = "";
            }
        });
    }

    loadAccounts();
})();
