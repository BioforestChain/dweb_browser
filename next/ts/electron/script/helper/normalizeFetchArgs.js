"use strict";
/** 将 fetch 的参数进行标准化解析 */
Object.defineProperty(exports, "__esModule", { value: true });
exports.normalizeFetchArgs = void 0;
const urlHelper_js_1 = require("./urlHelper.js");
const normalizeFetchArgs = (url, init) => {
    let _parsed_url;
    let _request_init = init;
    if (typeof url === "string") {
        _parsed_url = (0, urlHelper_js_1.parseUrl)(url);
    }
    else if (url instanceof Request) {
        _parsed_url = (0, urlHelper_js_1.parseUrl)(url.url);
        _request_init = url;
    }
    else if (url instanceof URL) {
        _parsed_url = url;
    }
    if (_parsed_url === undefined) {
        throw new Error(`no found url for fetch`);
    }
    const parsed_url = _parsed_url;
    const request_init = _request_init ?? {};
    return {
        parsed_url,
        request_init,
    };
};
exports.normalizeFetchArgs = normalizeFetchArgs;
