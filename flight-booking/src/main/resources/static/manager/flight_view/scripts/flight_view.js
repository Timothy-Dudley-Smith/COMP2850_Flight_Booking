(() => {
    "use strict";

    const flightsEndpoint = "/api/manager/flights";
    const createFlightEndpoint = "/api/manager/flight_view";
    const message = document.getElementById("manager-message");
    const form = document.getElementById("add-flight-form");
    const tableBody = document.getElementById("upcoming-flights-body");

    function setMessage(text, state) {
        if (!message) {
            return;
        }

        message.textContent = text;
        message.dataset.state = state;
    }

    function formatPrice(value) {
        if (typeof value !== "number" || Number.isNaN(value)) {
            return "Not available";
        }

        return `£${value.toFixed(2)}`;
    }

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

    function appendCell(row, text) {
        const cell = document.createElement("td");
        cell.textContent = text;
        row.appendChild(cell);
    }

    function renderFlights(flights) {
        if (!tableBody) {
            return;
        }

        if (!Array.isArray(flights) || flights.length === 0) {
            renderEmptyRow("No upcoming flights found.");
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
            removeButton.disabled = true;
            removeButton.textContent = "Remove";
            actionCell.appendChild(removeButton);
            row.appendChild(actionCell);

            tableBody.appendChild(row);
        });
    }

    async function loadFlights() {
        setMessage("Loading upcoming flights...", "info");

        try {
            const response = await fetch(flightsEndpoint);

            if (!response.ok) {
                throw new Error(`Request failed with status ${response.status}`);
            }

            const flights = await response.json();
            renderFlights(flights);

            if (Array.isArray(flights) && flights.length > 0) {
                const suffix = flights.length === 1 ? "" : "s";
                setMessage(`Loaded ${flights.length} upcoming flight${suffix}.`, "success");
            } else {
                setMessage("No upcoming flights found.", "info");
            }
        } catch (error) {
            renderEmptyRow("Failed to load flights.");
            setMessage("Failed to load upcoming flights.", "error");
            console.error(error);
        }
    }

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

    loadFlights();
})();
