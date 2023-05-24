const wm_mainListener = new WeakMap();
const resolveMainMessageListener = (listener) => {
    let resolveListener = wm_mainListener.get(listener);
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
        wm_mainListener.set(listener, resolveListener);
    }
    return resolveListener;
};
export const updateMainMessageListener = (target, method, listener_index) => {
    const source_method = target[method];
    target[method] = function (...args) {
        args[listener_index] = resolveMainMessageListener(args[listener_index]);
        return source_method.apply(this, args);
    };
    return target;
};
export const updateMainPostMessage = (target) => {
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
