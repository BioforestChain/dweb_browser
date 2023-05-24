"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.$isMatchReq = void 0;
const $isMatchReq = (matcher, pathname, method = "GET") => {
    return ((matcher.method ?? "GET") === method
        && (matcher.matchMode === "full"
            ? pathname === matcher.pathname
            : matcher.matchMode === "prefix" ? pathname.startsWith(matcher.pathname) : false));
};
exports.$isMatchReq = $isMatchReq;
