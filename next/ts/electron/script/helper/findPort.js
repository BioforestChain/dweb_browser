"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.findPort = exports.isPortInUse = void 0;
const net_1 = require("net");
const isPortInUse = (try_port, server = (0, net_1.createServer)()) => {
    return new Promise((resolve) => {
        server
            .once("error", (err) => {
            return resolve(false);
        })
            .once("listening", () => {
            const port = try_port ?? server.address().port;
            server
                .once("close", function () {
                resolve(port);
            })
                .close();
        })
            .listen(try_port);
    });
};
exports.isPortInUse = isPortInUse;
const findPort = async (favorite_ports = []) => {
    const server = (0, net_1.createServer)();
    for (const favorite_port of favorite_ports) {
        if (await (0, exports.isPortInUse)(favorite_port, server)) {
            return favorite_port;
        }
    }
    const fallback_port = await (0, exports.isPortInUse)(undefined, server);
    if (fallback_port) {
        return fallback_port;
    }
    throw new Error("fail to get useable port");
};
exports.findPort = findPort;
