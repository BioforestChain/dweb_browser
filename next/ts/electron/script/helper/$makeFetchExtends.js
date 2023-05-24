"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.fetchExtends = void 0;
const _makeFetchBaseExtends_js_1 = require("./$makeFetchBaseExtends.js");
const _makeFetchStreamExtends_js_1 = require("./$makeFetchStreamExtends.js");
exports.fetchExtends = {
    ..._makeFetchBaseExtends_js_1.fetchBaseExtends,
    ..._makeFetchStreamExtends_js_1.fetchStreamExtends,
};
