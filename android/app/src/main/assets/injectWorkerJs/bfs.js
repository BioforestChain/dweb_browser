function BFSInstallApp(path) {
    return globalThis.bfs.BFSInstallApp(path)
}

function BFSGetConnectChannel(url) {
    return globalThis.bfs.BFSGetConnectChannel(url)
}

function BFSPostConnectChannel(url, cmd, buf) {
    return globalThis.bfs.BFSPostConnectChannel(url, cmd, buf)
}

const BFSOriginFetch = fetch;

globalThis.fetch = (origin, option) => {
    if (origin.startsWith("file://") && origin.includes(".dweb")) {
        return BFSGetConnectChannel(origin)
    }
    return BFSOriginFetch(origin, option)
}