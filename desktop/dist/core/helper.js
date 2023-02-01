"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.openNwWindow = exports.PromiseOut = exports.$serializeResultToResponse = exports.$deserializeRequestToParams = exports.$typeNameParser = void 0;
const ipc_1 = require("./ipc");
const $typeNameParser = (key, typeName2, value) => {
    let param;
    if (value === null) {
        if (typeName2.endsWith("?")) {
            throw new Error(`param type error: '${key}'.`);
        }
        else {
            param = undefined;
        }
    }
    else {
        const typeName1 = (typeName2.endsWith("?") ? typeName2.slice(0, -1) : typeName2);
        switch (typeName1) {
            case "number": {
                param = +value;
                break;
            }
            case "boolean": {
                param = value === "" ? false : Boolean(value.toLowerCase());
                break;
            }
            case "mmid": {
                if (value.endsWith(".dweb") === false) {
                    throw new Error(`param mmid type error: '${key}':'${value}'`);
                }
                param = value;
                break;
            }
            case "string": {
                param = value;
                break;
            }
            default:
                param = void 0;
        }
    }
    return param;
};
exports.$typeNameParser = $typeNameParser;
const $deserializeRequestToParams = (schema) => {
    return (request) => {
        const url = request.parsed_url;
        const params = {};
        for (const [key, typeName2] of Object.entries(schema)) {
            params[key] = (0, exports.$typeNameParser)(key, typeName2, url.searchParams.get(key));
        }
        return params;
    };
};
exports.$deserializeRequestToParams = $deserializeRequestToParams;
/**
 * @TODO 实现模式匹配
 */
const $serializeResultToResponse = (schema) => {
    return (request, result) => {
        return new ipc_1.IpcResponse(request.req_id, 200, JSON.stringify(result), {
            "Content-Type": "application/json",
        });
    };
};
exports.$serializeResultToResponse = $serializeResultToResponse;
class PromiseOut {
    constructor() {
        this.promise = new Promise((resolve, reject) => {
            this.resolve = resolve;
            this.reject = reject;
        });
    }
}
exports.PromiseOut = PromiseOut;
const openNwWindow = (url, options) => {
    return new Promise((resolve) => {
        nw.Window.open(url, options, resolve);
    });
};
exports.openNwWindow = openNwWindow;
