function BFSInstallApp(path) {
    return globalThis.bfs.BFSInstallApp(path)
}

function BFSGetConnectChannel(url) {
    return globalThis.bfs.getConnectChannel(url)
}

function BFSPostConnectChannel(url, cmd, buf) {
    return globalThis.bfs.postConnectChannel(url, cmd, buf)
}

const BFSOriginFetch = fetch;

globalThis.fetch = (origin, option) => {
    if (origin.startsWith("file://")) {
        return BFSGetConnectChannel(origin)
    }
    return BFSOriginFetch(origin, option)
}
