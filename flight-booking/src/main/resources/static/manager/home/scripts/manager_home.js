(() => {
    "use strict";

    const todayLabel = document.getElementById("today-label");

    if (todayLabel) {
        const today = new Date();
        todayLabel.textContent = today.toLocaleDateString("en-GB", {
            weekday: "long",
            day: "numeric",
            month: "long",
            year: "numeric"
        });
    }
})();
