/**
 * Shared currency helper used by booking and loyalty pages.
 *
 * Pages store the selected currency in localStorage, then call these functions
 * when rendering prices back into the DOM.
 */
const EXCHANGE_RATES = {
    GBP: 1,
    USD: 1.27,
    EUR: 1.18,
    AUD: 1.96,
    JPY: 192,
};

const DEFAULT_CURRENCY = "GBP";
const STORAGE_KEY = "currency";

/**
 * Reads the user's saved currency preference and falls back to GBP for first-time visitors.
 */
function getSelectedCurrency() {
    return localStorage.getItem(STORAGE_KEY) || DEFAULT_CURRENCY;
}

/**
 * Persists the currency selected by page-level dropdowns so later screens use the same setting.
 */
function setSelectedCurrency(currencyCode) {
    localStorage.setItem(STORAGE_KEY, currencyCode);
}

/**
 * Converts a base GBP amount into the active currency and returns a display-ready string.
 *
 * The rest of the frontend passes GBP values into this helper before inserting text into the page.
 */
function fmt(amountInGBP) {
    const currency = getSelectedCurrency();
    const rate = EXCHANGE_RATES[currency];
    const convertedAmount = amountInGBP * rate;

    const formatter = new Intl.NumberFormat("en-GB", {
        style: "currency",
        currency: currency,
    });
    return formatter.format(convertedAmount);
}
