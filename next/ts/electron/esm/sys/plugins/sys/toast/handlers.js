/**
 * 设置状态
 * @param this
 * @param args
 * @param client_ipc
 * @param ipcRequest
 * @returns
 */
export async function show(args, client_ipc, ipcRequest) {
    const host = ipcRequest.parsed_url.host;
    const pathname = ipcRequest.parsed_url.pathname;
    const search = ipcRequest.parsed_url.search;
    const url = `file://mwebview.sys.dweb/plugin/${host}${pathname}${search}`;
    const result = await this.nativeFetch(url);
    return result;
}
