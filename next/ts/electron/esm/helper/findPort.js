import { createServer } from "net";
export const isPortInUse = (try_port, server = createServer()) => {
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
export const findPort = async (favorite_ports = []) => {
    const server = createServer();
    for (const favorite_port of favorite_ports) {
        if (await isPortInUse(favorite_port, server)) {
            return favorite_port;
        }
    }
    const fallback_port = await isPortInUse(undefined, server);
    if (fallback_port) {
        return fallback_port;
    }
    throw new Error("fail to get useable port");
};
