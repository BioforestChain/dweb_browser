"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.httpMethodCanOwnBody = void 0;
const httpMethodCanOwnBody = (method) => {
    return (method !== "GET" &&
        method !== "HEAD" &&
        method !== "TRACE" &&
        method !== "OPTIONS");
};
exports.httpMethodCanOwnBody = httpMethodCanOwnBody;
