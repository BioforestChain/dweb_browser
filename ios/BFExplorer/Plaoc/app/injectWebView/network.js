function BFSInstallApp (path) {
    window.webkit.messageHandlers.InstallBFS.postMessage({path:path});
}

function getConnectChannel(data) {
    console.log("swift#getConnectChannel:",data)
    window.webkit.messageHandlers.getConnectChannel.postMessage({param:data});
}

function postConnectChannel(strPath, cmd, buffer) {
    console.log("swift#postConnectChannel: ", strPath, " cmd: ", cmd, " buffer: ", buffer)
    window.webkit.messageHandlers.postConnectChannel.postMessage({strPath:strPath, cmd:cmd, buffer:buffer})
}
