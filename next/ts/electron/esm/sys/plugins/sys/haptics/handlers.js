export async function setHaptics(args, client_ipc, ipcRequest) {
    // const search = querystring.unescape(ipcRequest.url).split("?")[1]
    const host = ipcRequest.parsed_url.host;
    const pathname = ipcRequest.parsed_url.pathname;
    const search = ipcRequest.parsed_url.search;
    const url = `file://mwebview.sys.dweb/plugin/${host}${pathname}${search}&action=${pathname.slice(1)}`;
    const result = await this.nativeFetch(url);
    return true;
}
