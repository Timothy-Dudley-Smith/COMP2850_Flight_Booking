(() => {
    "use strict";

    const API_URL = "/api/airports";
    const MAX_OPTIONS = 12;

    let airports = [];

    const normalize = (value) => (value || "").trim().toUpperCase();
    const labelFor = (a) => `${a.city}, ${a.country} (${a.code})`;

    async function loadAirports() {
        const response = await fetch(API_URL);
        if (!response.ok) throw new Error(`Failed to load airports: ${response.status}`);

        const rows = await response.json();
        if (!Array.isArray(rows)) throw new Error("Airport API did not return a list");

        airports = rows.filter((a) => a && a.code && a.name && a.city && a.country);
    }

    function findMatches(query) {
        const q = normalize(query);
        if (!q) return [];

        return airports
            .filter((a) =>
                normalize(a.code).startsWith(q) ||
                normalize(a.city).startsWith(q) ||
                normalize(a.name).startsWith(q)
            )
            .slice(0, MAX_OPTIONS);
    }

    function resolveCode(inputValue) {
        const typed = normalize(inputValue);
        if (!typed) return "";

        const byCode = airports.find((a) => normalize(a.code) === typed);
        if (byCode) return byCode.code;

        const byLabel = airports.find((a) => normalize(labelFor(a)) === typed);
        if (byLabel) return byLabel.code;

        return "";
    }

    function populateDatalist(datalist, matches) {
        datalist.innerHTML = "";
        matches.forEach((a) => {
            const option = document.createElement("option");
            option.value = labelFor(a);
            option.dataset.code = a.code;
            datalist.appendChild(option);
        });
    }

    function wireAirportField(visibleInput, hiddenName, datalistId) {
        const hiddenInput = document.createElement("input");
        hiddenInput.type = "hidden";
        hiddenInput.name = hiddenName;

        visibleInput.removeAttribute("name");
        visibleInput.setAttribute("autocomplete", "off");
        visibleInput.setAttribute("list", datalistId);
        visibleInput.insertAdjacentElement("afterend", hiddenInput);

        const datalist = document.createElement("datalist");
        datalist.id = datalistId;
        hiddenInput.insertAdjacentElement("afterend", datalist);

        const syncHidden = () => {
            hiddenInput.value = resolveCode(visibleInput.value);
        };

        visibleInput.addEventListener("input", () => {
            hiddenInput.value = "";
            const matches = findMatches(visibleInput.value);
            populateDatalist(datalist, matches);
        });

        visibleInput.addEventListener("change", syncHidden);
        visibleInput.addEventListener("blur", syncHidden);

        return { visibleInput, hiddenInput };
    }

    function enforceValidSelection(form, ...fields) {
        form.addEventListener("submit", (event) => {
            let valid = true;

            fields.forEach(({ visibleInput, hiddenInput }) => {
                if (!hiddenInput.value) {
                    hiddenInput.value = resolveCode(visibleInput.value);
                }

                if (!hiddenInput.value) {
                    visibleInput.setCustomValidity("Please choose an airport from the suggestions.");
                    valid = false;
                } else {
                    visibleInput.setCustomValidity("");
                }
            });

            if (!valid) {
                event.preventDefault();
                form.reportValidity();
            }
        });
    }

    async function init() {
        const form = document.querySelector("form.flight-bar");
        const fromInput = document.querySelector('input[name="from"]');
        const toInput = document.querySelector('input[name="to"]');

        if (!form || !fromInput || !toInput) return;

        try {
            await loadAirports();
        } catch (error) {
            console.error(error);
            return;
        }

        const fromField = wireAirportField(fromInput, "from", "airport-from-list");
        const toField = wireAirportField(toInput, "to", "airport-to-list");

        enforceValidSelection(form, fromField, toField);
    }

    document.addEventListener("DOMContentLoaded", init);
})();


//this file was created by codex