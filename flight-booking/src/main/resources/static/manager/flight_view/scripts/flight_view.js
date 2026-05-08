(async () => {
    /** Protects the flight manager tools behind a valid manager session before data or forms are used. */
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

    /** Flight-view coordinates flight loading, airport creation, filtering, and row-level removal. */
    const allFlightsEndpoint = "/api/flights?date=";
    const createFlightEndpoint = "/api/manager/flight_view";
    const allAirportsEndpoint = "/api/airports";
    const createAirportEndpoint = sessionId
        ? `/api/manager/airports?sessionId=${encodeURIComponent(sessionId)}`
        : "/api/manager/airports";
    const deleteFlightEndpoint = (flightId) => `/api/manager/flights/${encodeURIComponent(flightId)}`;
    const message = document.getElementById("manager-message");
    const form = document.getElementById("add-flight-form");
    const airportForm = document.getElementById("add-airport-form");
    const airportCodeInput = document.getElementById("airport-code");
    const tableBody = document.getElementById("upcoming-flights-body");
    const filterSummary = document.getElementById("flight-filter-summary");
    const filterButtons = Array.from(document.querySelectorAll("[data-flight-filter]"));
    const filterLabels = {
        upcoming: "upcoming",
        past: "past",
        all: "all"
    };

    let allFlights = [];
    let activeFilter = "upcoming";
    let existingAirportCodes = new Set();

    /** Updates the shared manager message area after each load, submit, or delete action. */
    function setMessage(text, state) {
        if (!message) {
            return;
        }

        message.textContent = text;
        message.className = state && state !== "info" ? state : "";
    }

    /** Keeps the filter summary aligned with the currently visible flight subset. */
    function setFilterSummary(text) {
        if (!filterSummary) {
            return;
        }

        filterSummary.textContent = text;
    }

    /** Formats numeric prices for the flight table. */
    function formatPrice(value) {
        if (typeof value !== "number" || Number.isNaN(value)) {
            return "Not available";
        }

        return `£${value.toFixed(2)}`;
    }

    /** Renders a single empty-state row when no flights match the current conditions. */
    function renderEmptyRow(text) {
        if (!tableBody) {
            return;
        }

        tableBody.innerHTML = `
            <tr>
                <td colspan="9">${text}</td>
            </tr>
        `;
    }

    /** Appends a plain-text cell to a generated flight row. */
    function appendCell(row, text) {
        const cell = document.createElement("td");
        cell.textContent = text;
        row.appendChild(cell);
    }

    /** Standardizes airport codes so validation and duplicate checks use uppercase IATA values. */
    function normalizeAirportCode(value) {
        return value.trim().toUpperCase();
    }

    /** Trims free-text airport fields and collapses extra whitespace before submission. */
    function normalizeTextField(value) {
        return value.trim().replace(/\s+/g, " ");
    }

    /** Creates a stable local date string for comparing flight dates against today. */
    function toLocalDateString(date) {
        const year = date.getFullYear();
        const month = String(date.getMonth() + 1).padStart(2, "0");
        const day = String(date.getDate()).padStart(2, "0");
        return `${year}-${month}-${day}`;
    }

    /** Combines each flight's date and departure time so past/upcoming filtering uses one comparison point. */
    function parseFlightDepartureDateTime(flight) {
        if (!flight || typeof flight.date !== "string" || !flight.date) {
            return null;
        }

        const time = typeof flight.departureTime === "string" && flight.departureTime
            ? flight.departureTime
            : "00:00";

        const dateTime = new Date(`${flight.date}T${time}`);
        return Number.isNaN(dateTime.getTime()) ? null : dateTime;
    }

    /** Determines whether a flight belongs in the past filter or the upcoming filter. */
    function isPastFlight(flight) {
        const departureDateTime = parseFlightDepartureDateTime(flight);

        if (departureDateTime) {
            return departureDateTime.getTime() < Date.now();
        }

        if (typeof flight?.date !== "string") {
            return false;
        }

        return flight.date < toLocalDateString(new Date());
    }

    /** Sorts flights chronologically before the active filter decides whether to reverse them. */
    function compareFlightsAscending(leftFlight, rightFlight) {
        const leftDateTime = parseFlightDepartureDateTime(leftFlight);
        const rightDateTime = parseFlightDepartureDateTime(rightFlight);

        if (!leftDateTime && !rightDateTime) {
            return 0;
        }

        if (!leftDateTime) {
            return 1;
        }

        if (!rightDateTime) {
            return -1;
        }

        return leftDateTime.getTime() - rightDateTime.getTime();
    }

    /** Applies the active filter to the cached flights list before table rendering. */
    function getFilteredFlights() {
        const filteredFlights = allFlights.filter((flight) => {
            if (activeFilter === "past") {
                return isPastFlight(flight);
            }

            if (activeFilter === "upcoming") {
                return !isPastFlight(flight);
            }

            return true;
        });

        filteredFlights.sort(compareFlightsAscending);

        if (activeFilter === "past") {
            filteredFlights.reverse();
        }

        return filteredFlights;
    }

    /** Chooses the empty-state copy that matches the current filter tab. */
    function getEmptyFlightText() {
        if (activeFilter === "past") {
            return "No past flights found.";
        }

        if (activeFilter === "all") {
            return "No flights found.";
        }

        return "No upcoming flights found.";
    }

    /** Summarizes how many flights the table is currently showing for the selected filter. */
    function getSummaryText(flightCount) {
        if (activeFilter === "past") {
            return flightCount === 1 ? "Showing 1 past flight." : `Showing ${flightCount} past flights.`;
        }

        if (activeFilter === "all") {
            return flightCount === 1 ? "Showing 1 flight." : `Showing ${flightCount} flights.`;
        }

        return flightCount === 1 ? "Showing 1 upcoming flight." : `Showing ${flightCount} upcoming flights.`;
    }

    /** Syncs the filter button active state with the current in-memory filter value. */
    function updateFilterButtons() {
        filterButtons.forEach((button) => {
            const buttonFilter = button.dataset.flightFilter;
            const isActive = buttonFilter === activeFilter;
            button.classList.toggle("active", isActive);
            button.setAttribute("aria-pressed", isActive ? "true" : "false");
        });
    }

    /** Reads JSON or plain-text API errors so the message banner can show a useful explanation. */
    async function readResponseMessage(response, fallbackMessage) {
        const rawBody = await response.text();

        if (!rawBody) {
            return fallbackMessage;
        }

        try {
            const parsed = JSON.parse(rawBody);
            return parsed.message ?? parsed.error ?? rawBody;
        } catch {
            return rawBody;
        }
    }

    /** Removes a flight after confirmation, then reloads the table from the server. */
    async function removeFlight(flightId, buttonElement) {
        if (!flightId) {
            setMessage("Flight ID is missing, so this flight cannot be removed.", "error");
            return;
        }

        const confirmed = window.confirm(`Remove flight ${flightId}?`);
        if (!confirmed) {
            return;
        }

        buttonElement.disabled = true;
        setMessage(`Removing flight ${flightId}...`, "info");

        try {
            const response = await fetch(deleteFlightEndpoint(flightId), {
                method: "DELETE"
            });

            if (!response.ok) {
                const errorMessage = await readResponseMessage(response, "Failed to remove flight.");
                throw new Error(errorMessage);
            }

            const successMessage = await readResponseMessage(response, "Flight removed successfully.");
            setMessage(successMessage, "success");
            await loadFlights();
        } catch (error) {
            setMessage(error.message || "Failed to remove flight.", "error");
            buttonElement.disabled = false;
            console.error(error);
        }
    }

    /** Rebuilds the flights table from the filtered data set and wires each remove button. */
    function renderFlights(flights) {
        if (!tableBody) {
            return;
        }

        if (!Array.isArray(flights) || flights.length === 0) {
            renderEmptyRow(getEmptyFlightText());
            setFilterSummary(getSummaryText(0));
            return;
        }

        tableBody.innerHTML = "";

        flights.forEach((flight) => {
            const row = document.createElement("tr");

            appendCell(row, flight.flightId ?? "Not available");
            appendCell(row, flight.departureAirport ?? "Not available");
            appendCell(row, flight.arrivalAirport ?? "Not available");
            appendCell(row, flight.date ?? "Not available");
            appendCell(row, flight.departureTime ?? "Not available");
            appendCell(row, flight.arrivalTime ?? "Not available");
            appendCell(row, flight.length != null ? String(flight.length) : "Not available");
            appendCell(row, formatPrice(flight.price));

            const actionCell = document.createElement("td");
            const removeButton = document.createElement("button");
            removeButton.type = "button";
            removeButton.textContent = "Remove";
            removeButton.className = "remove-flight-button";
            removeButton.addEventListener("click", () => {
                removeFlight(flight.flightId, removeButton);
            });
            actionCell.appendChild(removeButton);
            row.appendChild(actionCell);

            tableBody.appendChild(row);
        });

        setFilterSummary(getSummaryText(flights.length));
    }

    /** Re-renders the table whenever the filter changes or the full flights list is refreshed. */
    function applyActiveFilter() {
        updateFilterButtons();
        renderFlights(getFilteredFlights());
    }

    /** Loads all flights from the API, caches them locally, and renders the selected filter view. */
    async function loadFlights() {
        setMessage("Loading flights...", "info");

        try {
            const response = await fetch(allFlightsEndpoint);

            if (!response.ok) {
                throw new Error(`Request failed with status ${response.status}`);
            }

            const flights = await response.json();
            allFlights = Array.isArray(flights) ? flights : [];
            applyActiveFilter();

            const suffix = allFlights.length === 1 ? "" : "s";
            setMessage(`Loaded ${allFlights.length} flight${suffix}.`, "success");
        } catch (error) {
            allFlights = [];
            updateFilterButtons();
            renderEmptyRow("Failed to load flights.");
            setFilterSummary("Unable to show flights right now.");
            setMessage("Failed to load flights.", "error");
            console.error(error);
        }
    }

    /** Loads existing airport codes so duplicate airport creation can be blocked client-side first. */
    async function loadAirportCodes() {
        try {
            const response = await fetch(allAirportsEndpoint);

            if (!response.ok) {
                throw new Error(`Request failed with status ${response.status}`);
            }

            const airports = await response.json();
            const codes = Array.isArray(airports)
                ? airports
                    .map((airport) => typeof airport?.code === "string" ? normalizeAirportCode(airport.code) : "")
                    .filter(Boolean)
                : [];

            existingAirportCodes = new Set(codes);
        } catch (error) {
            console.error("Failed to load airport codes.", error);
        }
    }

    /** Converts the add-flight form fields into the request body expected by the manager API. */
    function buildFlightPayload(formElement) {
        const formData = new FormData(formElement);

        return {
            flightId: (formData.get("flightId") ?? "").toString().trim(),
            departureAirport: (formData.get("departureAirport") ?? "").toString().trim(),
            arrivalAirport: (formData.get("arrivalAirport") ?? "").toString().trim(),
            date: (formData.get("date") ?? "").toString(),
            departureTime: (formData.get("departureTime") ?? "").toString(),
            arrivalTime: (formData.get("arrivalTime") ?? "").toString(),
            length: Number(formData.get("length")),
            price: Number(formData.get("price"))
        };
    }

    /** Converts the add-airport form into a normalized payload for the airport endpoint. */
    function buildAirportPayload(formElement) {
        const formData = new FormData(formElement);

        return {
            code: normalizeAirportCode((formData.get("code") ?? "").toString()).slice(0, 3),
            name: normalizeTextField((formData.get("name") ?? "").toString()),
            city: normalizeTextField((formData.get("city") ?? "").toString()),
            country: normalizeTextField((formData.get("country") ?? "").toString())
        };
    }

    /** Performs lightweight airport validation before the request is sent to the backend. */
    function validateAirportPayload(payload) {
        if (!payload.code) {
            return "Enter an airport code.";
        }

        if (!/^[A-Z]{3}$/.test(payload.code)) {
            return "Airport code must be exactly 3 letters.";
        }

        if (!payload.name) {
            return "Enter the airport name.";
        }

        if (!payload.city) {
            return "Enter the airport city.";
        }

        if (!payload.country) {
            return "Enter the airport country.";
        }

        if (existingAirportCodes.has(payload.code)) {
            return `Airport code ${payload.code} already exists.`;
        }

        return null;
    }

    /** Submits a new flight, resets the form, and refreshes the visible flights table. */
    async function submitFlight(formElement) {
        const payload = buildFlightPayload(formElement);

        setMessage("Submitting flight...", "info");

        const response = await fetch(createFlightEndpoint, {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify(payload)
        });

        if (!response.ok) {
            throw new Error(`Request failed with status ${response.status}`);
        }

        formElement.reset();
        setMessage("Flight submitted successfully.", "success");
        await loadFlights();
    }

    /** Submits a new airport and updates the local code cache once it succeeds. */
    async function submitAirport(formElement) {
        const payload = buildAirportPayload(formElement);
        const validationError = validateAirportPayload(payload);

        if (validationError) {
            throw new Error(validationError);
        }

        setMessage(`Adding airport ${payload.code}...`, "info");

        const response = await fetch(createAirportEndpoint, {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify(payload)
        });

        if (!response.ok) {
            const errorMessage = await readResponseMessage(response, "Failed to add airport.");
            throw new Error(errorMessage);
        }

        existingAirportCodes.add(payload.code);
        formElement.reset();
        setMessage(`Airport ${payload.code} added successfully.`, "success");
    }

    /** Filter buttons switch between upcoming, past, and all flights without another fetch. */
    filterButtons.forEach((button) => {
        button.addEventListener("click", () => {
            const selectedFilter = button.dataset.flightFilter;

            if (!selectedFilter || !(selectedFilter in filterLabels) || selectedFilter === activeFilter) {
                return;
            }

            activeFilter = selectedFilter;
            applyActiveFilter();
        });
    });

    /** The add-flight form is handled in JavaScript so status messaging and refresh happen inline. */
    if (form) {
        form.addEventListener("submit", async (event) => {
            event.preventDefault();

            try {
                await submitFlight(form);
            } catch (error) {
                setMessage("Failed to submit flight.", "error");
                console.error(error);
            }
        });
    }

    /** Airport codes are uppercased as the manager types to match IATA expectations. */
    if (airportCodeInput) {
        airportCodeInput.addEventListener("input", () => {
            airportCodeInput.value = normalizeAirportCode(airportCodeInput.value).slice(0, 3);
        });
    }

    /** The add-airport form uses the same inline message area as the flight form. */
    if (airportForm) {
        airportForm.addEventListener("submit", async (event) => {
            event.preventDefault();

            try {
                await submitAirport(airportForm);
            } catch (error) {
                setMessage(error.message || "Failed to add airport.", "error");
                console.error(error);
            }
        });
    }

    /** Airport codes load first so duplicate validation is ready before managers submit the form. */
    await loadAirportCodes();
    /** Flights then load into the table using the default upcoming filter. */
    loadFlights();
})();
