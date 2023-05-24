import * as dntShim from "../_dnt.shims.js";
const URL_BASE = "document" in dntShim.dntGlobalThis
    ? document.baseURI
    : "location" in dntShim.dntGlobalThis &&
        (location.protocol === "http:" ||
            location.protocol === "https:" ||
            location.protocol === "file:" ||
            location.protocol === "chrome-extension:")
        ? location.href
        : "file:///";
export const parseUrl = (url, base = URL_BASE) => {
    return new URL(url, base);
};
export const updateUrlOrigin = (url, new_origin) => {
    const { origin, href } = parseUrl(url);
    return new URL(new_origin + href.slice(origin.length));
};
export const buildUrl = (url, ext) => {
    if (ext.pathname !== undefined) {
        url.pathname = ext.pathname;
    }
    if (ext.search) {
        if (ext.search instanceof URLSearchParams) {
            url.search = ext.search.toString();
        }
        else if (typeof ext.search === "string") {
            url.search = ext.search.toString();
        }
        else {
            url.search = new URLSearchParams(Object.entries(ext.search).map(([key, value]) => {
                return [
                    key,
                    typeof value === "string" ? value : JSON.stringify(value),
                ];
            })).toString();
        }
    }
    return url;
};
