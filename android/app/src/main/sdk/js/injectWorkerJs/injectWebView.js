onmessage = function (e) {
    port = e.ports[0];
    port.onmessage = function (f) {
    console.log("backWebView#onmessage",f.data)
        parse(f.data);
    }
}