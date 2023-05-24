"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.$deserializeRequestToParams = void 0;
const _typeNameParser_js_1 = require("./$typeNameParser.js");
const $deserializeRequestToParams = (schema) => {
    return (request) => {
        const url = request.parsed_url;
        const params = {};
        for (const [key, typeName2] of Object.entries(schema)) {
            params[key] = (0, _typeNameParser_js_1.$typeNameParser)(key, typeName2, url.searchParams.get(key));
        }
        return params;
    };
};
exports.$deserializeRequestToParams = $deserializeRequestToParams;
