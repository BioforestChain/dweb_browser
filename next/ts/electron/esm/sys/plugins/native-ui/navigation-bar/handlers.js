/**
 * 获取状态
 * @param this
 * @param args
 * @param client_ipc
 * @param ipcRequest
 * @returns
 */
export async function getState(args, client_ipc, ipcRequest) {
    // const search = querystring.unescape(ipcRequest.url).split("?")[1]
    const host = ipcRequest.parsed_url.host;
    const pathname = ipcRequest.parsed_url.pathname;
    const search = ipcRequest.parsed_url.search;
    const url = `file://mwebview.sys.dweb/plugin/${host}${pathname}${search}`;
    const result = await this.nativeFetch(url);
    return result;
}
/**
 * 设置状态
 * @param this
 * @param args
 * @param client_ipc
 * @param ipcRequest
 * @returns
 */
export async function setState(args, client_ipc, ipcRequest) {
    const host = ipcRequest.parsed_url.host;
    const pathname = ipcRequest.parsed_url.pathname;
    const search = ipcRequest.parsed_url.search;
    const url = `file://mwebview.sys.dweb/plugin/${host}${pathname}${search}`;
    const result = await this.nativeFetch(url);
    return result;
}
/**
 * 开启监听
 * @param this
 * @param args
 * @param clientIpc
 * @param request
 * @returns
 */
export async function startObserve(args, clientIpc, request) {
    const host = request.headers.get("host");
    if (host === null) {
        throw new Error('host === null');
        debugger;
    }
    this.observesState.set(host, true);
    return true;
}
/**
 * 停止 监听
 * @param this
 * @param args
 * @param clientIpc
 * @param request
 * @returns
 */
export async function stopObserve(args, clientIpc, request) {
    const host = request.headers.get('host');
    if (host === null) {
        throw new Error('host === null');
        debugger;
    }
    this.observesState.set(host, false);
    return true;
}
