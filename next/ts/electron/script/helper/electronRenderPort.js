"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.updateRenderPostMessage = exports.resolveRenderMessageListener = exports.updateRenderMessageListener = exports.updateRenderPort = void 0;
const updateRenderPort = (port) => {
    (0, exports.updateRenderMessageListener)(port, "addEventListener", 1);
    (0, exports.updateRenderMessageListener)(port, "removeEventListener", 1);
    (0, exports.updateRenderPostMessage)(port);
    return port;
};
exports.updateRenderPort = updateRenderPort;
const updateRenderMessageListener = (target, method, listener_index) => {
    const source_method = target[method];
    target[method] = function (...args) {
        args[listener_index] = (0, exports.resolveRenderMessageListener)(args[listener_index]);
        return source_method.apply(this, args);
    };
    return target;
};
exports.updateRenderMessageListener = updateRenderMessageListener;
const wm_renderListener = new WeakMap();
const resolveRenderMessageListener = (listener) => {
    if (typeof listener === "object") {
        listener.handleEvent = (0, exports.resolveRenderMessageListener)(listener.handleEvent);
        return listener;
    }
    let resolveListener = wm_renderListener.get(listener);
    if (resolveListener === undefined) {
        resolveListener = function (event) {
            JSON.stringify(event.data, function resolver(key, value) {
                if (Array.isArray(value) && value[0] === "#PORT#") {
                    this[key] = event.ports[value[1]];
                    return null;
                }
                return value;
            });
            return listener.call(this, event);
        };
        /// 双向绑定
        wm_renderListener.set(listener, resolveListener);
        wm_renderListener.set(resolveListener, listener);
    }
    return resolveListener;
};
exports.resolveRenderMessageListener = resolveRenderMessageListener;
const updateRenderPostMessage = (target) => {
    const postMessage = target.postMessage;
    target.postMessage = function (message, transfer) {
        if (Array.isArray(transfer)) {
            JSON.stringify(message, function replacer(key, value) {
                if (value && typeof value === "object" && "postMessage" in value) {
                    const index = transfer.indexOf(value);
                    if (index !== -1) {
                        this[key] = ["#PORT#", index];
                        return null;
                    }
                }
                return value;
            });
            postMessage.call(this, message, transfer);
        }
        else if (transfer) {
            postMessage.call(this, message, transfer);
        }
        else {
            postMessage.call(this, message);
        }
    };
    return target;
};
exports.updateRenderPostMessage = updateRenderPostMessage;
