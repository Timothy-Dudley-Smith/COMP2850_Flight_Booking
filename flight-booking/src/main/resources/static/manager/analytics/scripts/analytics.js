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

    const analyticsEndpoint = "/api/manager/analytics";
    const analyticsMessage = document.getElementById("analytics-message");
    const analyticsRetry = document.getElementById("analytics-retry");
    const totalReservations = document.getElementById("total-reservations");
    const upcomingFlightsCount = document.getElementById("upcoming-flights-count");
    const openSupportTickets = document.getElementById("open-support-tickets");
    const popularRoute = document.getElementById("popular-route");
    const peakBookingTime = document.getElementById("peak-booking-time");
    const bookingsPerFlightBody = document.getElementById("bookings-per-flight-body");
    const popularRoutesBody = document.getElementById("popular-routes-body");
    const bookingsPerFlightShowFewer = document.getElementById("bookings-per-flight-show-fewer");
    const bookingsPerFlightShowMore = document.getElementById("bookings-per-flight-show-more");
    const popularRoutesShowFewer = document.getElementById("popular-routes-show-fewer");
    const popularRoutesShowMore = document.getElementById("popular-routes-show-more");
    const hourlyBookingChart = document.getElementById("hourly-booking-chart");
    const hourlyBookingEmpty = document.getElementById("hourly-booking-empty");
    const initialReportLimit = 5;
    const reportStepSize = 25;
    let bookingsPerFlightRows = [];
    let popularRouteRows = [];
    let visibleBookingsPerFlightCount = initialReportLimit;
    let visiblePopularRoutesCount = initialReportLimit;

    function setText(element, value, fallback = "Not available") {
        if (!element) {
            return;
        }

        if (value === null || value === undefined || value === "") {
            element.textContent = fallback;
            return;
        }

        element.textContent = String(value);
    }

    function setAnalyticsMessage(message, tone = "") {
        if (!analyticsMessage) {
            return;
        }

        analyticsMessage.textContent = message ?? "";
        analyticsMessage.classList.remove("is-error", "is-success");

        if (tone === "error") {
            analyticsMessage.classList.add("is-error");
        } else if (tone === "success") {
            analyticsMessage.classList.add("is-success");
        }
    }

    function setRetryVisible(isVisible) {
        if (!analyticsRetry) {
            return;
        }

        analyticsRetry.hidden = !isVisible;
    }

    function getCount(value) {
        return value?.bookingCount ?? value?.count ?? value?.total ?? 0;
    }

    function formatRoute(route) {
        if (!route) {
            return null;
        }

        if (typeof route === "string") {
            return route;
        }

        const departure = route.departureAirport ?? route.from;
        const arrival = route.arrivalAirport ?? route.to;

        if (!departure || !arrival) {
            return null;
        }

        return `${departure} to ${arrival}`;
    }

    function formatRouteWithCount(route) {
        const label = formatRoute(route);
        if (!label) {
            return null;
        }

        const count = getCount(route);
        return typeof count === "number" ? `${label} (${count} bookings)` : label;
    }

    function formatPeakTime(value) {
        if (!value) {
            return null;
        }

        if (typeof value === "string") {
            return value;
        }

        const hour = value.hour ?? value.time ?? value.bookingHour;
        if (hour === null || hour === undefined || hour === "") {
            return null;
        }

        const count = getCount(value);
        const hourLabel = formatHourLabel(hour);
        return typeof count === "number" ? `${hourLabel} (${count} bookings)` : hourLabel;
    }

    function setTableMessage(tbody, colspan, message) {
        if (!tbody) {
            return;
        }

        tbody.innerHTML = `<tr><td colspan="${colspan}">${message}</td></tr>`;
    }

    function updateReportControls(showFewerButton, showMoreButton, rows, visibleCount) {
        if (!showFewerButton || !showMoreButton) {
            return;
        }

        if (!Array.isArray(rows) || rows.length <= initialReportLimit) {
            showFewerButton.hidden = true;
            showMoreButton.hidden = true;
            return;
        }

        showFewerButton.hidden = visibleCount <= initialReportLimit;
        showMoreButton.hidden = visibleCount >= rows.length;
    }

    function renderBookingsPerFlight(rows) {
        if (!Array.isArray(rows) || rows.length === 0) {
            setTableMessage(bookingsPerFlightBody, 4, "No bookings-per-flight data yet.");
            updateReportControls(bookingsPerFlightShowFewer, bookingsPerFlightShowMore, rows, visibleBookingsPerFlightCount);
            return;
        }

        bookingsPerFlightRows = rows;
        visibleBookingsPerFlightCount = Math.min(
            Math.max(visibleBookingsPerFlightCount, initialReportLimit),
            rows.length
        );
        const visibleRows = rows.slice(0, visibleBookingsPerFlightCount);

        bookingsPerFlightBody.innerHTML = visibleRows.map((row) => {
            const route = formatRoute(row) ?? "Not available";
            const date = row.date ?? "Not available";
            const count = getCount(row);

            return `
                <tr>
                    <td>${row.flightId ?? "Not available"}</td>
                    <td>${route}</td>
                    <td>${date}</td>
                    <td>${count}</td>
                </tr>
            `;
        }).join("");

        updateReportControls(
            bookingsPerFlightShowFewer,
            bookingsPerFlightShowMore,
            rows,
            visibleBookingsPerFlightCount
        );
    }

    function renderPopularRoutes(rows, fallbackRoute) {
        if (Array.isArray(rows) && rows.length > 0) {
            popularRouteRows = rows;
            visiblePopularRoutesCount = Math.min(
                Math.max(visiblePopularRoutesCount, initialReportLimit),
                rows.length
            );
            const visibleRows = rows.slice(0, visiblePopularRoutesCount);

            popularRoutesBody.innerHTML = visibleRows.map((row) => `
                <tr>
                    <td>${formatRoute(row) ?? "Not available"}</td>
                    <td>${getCount(row)}</td>
                </tr>
            `).join("");

            updateReportControls(
                popularRoutesShowFewer,
                popularRoutesShowMore,
                rows,
                visiblePopularRoutesCount
            );
            return;
        }

        if (fallbackRoute) {
            popularRouteRows = [fallbackRoute];
            popularRoutesBody.innerHTML = `
                <tr>
                    <td>${formatRoute(fallbackRoute) ?? fallbackRoute}</td>
                    <td>${typeof fallbackRoute === "object" ? getCount(fallbackRoute) : "Not available"}</td>
                </tr>
            `;
            updateReportControls(popularRoutesShowFewer, popularRoutesShowMore, popularRouteRows, visiblePopularRoutesCount);
            return;
        }

        popularRouteRows = [];
        setTableMessage(popularRoutesBody, 2, "No popular-route data yet.");
        updateReportControls(popularRoutesShowFewer, popularRoutesShowMore, popularRouteRows, visiblePopularRoutesCount);
    }

    function formatHourLabel(hour) {
        const numericHour = Number(hour);
        if (!Number.isFinite(numericHour)) {
            return "Not available";
        }

        return `${String(numericHour).padStart(2, "0")}:00`;
    }

    function normaliseBookingsByHour(rows) {
        const buckets = Array.from({ length: 24 }, (_, hour) => ({
            hour,
            count: 0
        }));

        if (!Array.isArray(rows)) {
            return buckets;
        }

        rows.forEach((row) => {
            const hour = Number(row.hour ?? row.time ?? row.bookingHour);
            const count = Number(row.count ?? row.bookingCount ?? row.total ?? 0);

            if (Number.isInteger(hour) && hour >= 0 && hour <= 23 && Number.isFinite(count)) {
                buckets[hour].count = count;
            }
        });

        return buckets;
    }

    function findPeakHour(rows) {
        const buckets = normaliseBookingsByHour(rows);
        const peak = buckets.reduce((currentPeak, row) => {
            return row.count > currentPeak.count ? row : currentPeak;
        }, buckets[0]);

        return peak.count > 0 ? peak : null;
    }

    function renderBookingsByHour(rows) {
        if (!hourlyBookingChart) {
            return;
        }

        const buckets = normaliseBookingsByHour(rows);
        const maxCount = Math.max(...buckets.map((row) => row.count), 0);

        hourlyBookingChart.innerHTML = "";

        buckets.forEach((row) => {
            const item = document.createElement("div");
            item.className = "hourly-bar-item";

            const bar = document.createElement("div");
            bar.className = "hourly-bar";
            bar.style.height = maxCount > 0 ? `${Math.max((row.count / maxCount) * 100, 4)}%` : "4%";
            bar.title = `${formatHourLabel(row.hour)}: ${row.count} booking${row.count === 1 ? "" : "s"}`;

            const value = document.createElement("span");
            value.className = "hourly-bar-value";
            value.textContent = String(row.count);

            const label = document.createElement("span");
            label.className = "hourly-bar-label";
            label.textContent = String(row.hour).padStart(2, "0");

            bar.appendChild(value);
            item.appendChild(bar);
            item.appendChild(label);
            hourlyBookingChart.appendChild(item);
        });

        if (hourlyBookingEmpty) {
            hourlyBookingEmpty.textContent = maxCount > 0
                ? "Each bar shows confirmed bookings created during that hour."
                : "No hourly booking data yet.";
        }
    }

    function clearBookingsByHour(message) {
        if (hourlyBookingChart) {
            hourlyBookingChart.innerHTML = "";
        }

        if (hourlyBookingEmpty) {
            hourlyBookingEmpty.textContent = message;
        }
    }

    function renderAnalytics(data) {
        const topRoute = data.mostPopularRoute ?? data.popularRoute ?? data.popularRoutes?.[0];
        const bookingsByHour = data.bookingsPerHour ?? data.bookingsByHour ?? data.peakBookingTimes;
        const topPeakTime = data.peakBookingTime ?? findPeakHour(bookingsByHour);

        setText(totalReservations, data.totalBookings ?? data.totalReservations ?? 0);
        setText(upcomingFlightsCount, data.upcomingFlights ?? data.upcomingFlightCount ?? 0);
        setText(openSupportTickets, data.openTickets ?? data.openSupportTickets ?? 0);
        setText(popularRoute, formatRouteWithCount(topRoute), "No route data yet");
        setText(peakBookingTime, formatPeakTime(topPeakTime), "Needs booking timestamps");

        renderBookingsPerFlight(data.bookingsPerFlight);
        renderPopularRoutes(data.popularRoutes, topRoute);
        renderBookingsByHour(bookingsByHour);
    }

    function describeAnalyticsError(error) {
        if (error?.status) {
            return `Unable to load analytics right now (HTTP ${error.status}).`;
        }

        if (error instanceof SyntaxError) {
            return "Unable to load analytics because the server returned invalid data.";
        }

        if (error instanceof TypeError) {
            return "Unable to load analytics right now. Check the server is running and try again.";
        }

        return error?.message || "Unable to load analytics right now.";
    }

    function renderAnalyticsUnavailable(error) {
        bookingsPerFlightRows = [];
        popularRouteRows = [];
        visibleBookingsPerFlightCount = initialReportLimit;
        visiblePopularRoutesCount = initialReportLimit;

        setText(totalReservations, "Unavailable");
        setText(upcomingFlightsCount, "Unavailable");
        setText(openSupportTickets, "Unavailable");
        setText(popularRoute, "Unavailable");
        setText(peakBookingTime, "Unavailable");

        setTableMessage(bookingsPerFlightBody, 4, "Unable to load bookings-per-flight data.");
        setTableMessage(popularRoutesBody, 2, "Unable to load popular-route data.");
        updateReportControls(bookingsPerFlightShowFewer, bookingsPerFlightShowMore, [], visibleBookingsPerFlightCount);
        updateReportControls(popularRoutesShowFewer, popularRoutesShowMore, [], visiblePopularRoutesCount);
        clearBookingsByHour("Unable to load hourly booking data.");
        setAnalyticsMessage(describeAnalyticsError(error), "error");
        setRetryVisible(true);
    }

    function renderLoadingState() {
        setAnalyticsMessage("Loading analytics...");
        setRetryVisible(false);
    }

    async function loadAnalytics() {
        renderLoadingState();

        try {
            const response = await fetch(analyticsEndpoint);

            if (!response.ok) {
                const error = new Error(`Analytics request failed with status ${response.status}`);
                error.status = response.status;
                throw error;
            }

            let data;
            try {
                data = await response.json();
            } catch (parseError) {
                throw new SyntaxError("Analytics response was not valid JSON.");
            }

            renderAnalytics(data);
            setAnalyticsMessage("");
            setRetryVisible(false);
        } catch (error) {
            renderAnalyticsUnavailable(error);
            console.warn(error);
        }
    }

    if (bookingsPerFlightShowMore) {
        bookingsPerFlightShowMore.addEventListener("click", () => {
            visibleBookingsPerFlightCount += reportStepSize;
            renderBookingsPerFlight(bookingsPerFlightRows);
        });
    }

    if (bookingsPerFlightShowFewer) {
        bookingsPerFlightShowFewer.addEventListener("click", () => {
            visibleBookingsPerFlightCount = Math.max(initialReportLimit, visibleBookingsPerFlightCount - reportStepSize);
            renderBookingsPerFlight(bookingsPerFlightRows);
        });
    }

    if (popularRoutesShowMore) {
        popularRoutesShowMore.addEventListener("click", () => {
            visiblePopularRoutesCount += reportStepSize;
            renderPopularRoutes(popularRouteRows);
        });
    }

    if (popularRoutesShowFewer) {
        popularRoutesShowFewer.addEventListener("click", () => {
            visiblePopularRoutesCount = Math.max(initialReportLimit, visiblePopularRoutesCount - reportStepSize);
            renderPopularRoutes(popularRouteRows);
        });
    }

    if (analyticsRetry) {
        analyticsRetry.addEventListener("click", () => {
            loadAnalytics();
        });
    }

    loadAnalytics();
})();
