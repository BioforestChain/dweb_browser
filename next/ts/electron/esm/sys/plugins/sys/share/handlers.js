import { ReadableStreamIpc } from "../../../../core/ipc-web/ReadableStreamIpc.js";
import { IPC_ROLE } from "../../../../core/ipc/index.js";
/**
 * 设置状态
 * @param this
 * @param args
 * @param client_ipc
 * @param ipcRequest
 * @returns
 */
export async function share(args, client_ipc, ipcRequest) {
    const host = ipcRequest.parsed_url.host;
    const pathname = ipcRequest.parsed_url.pathname;
    const search = ipcRequest.parsed_url.search;
    const url = `file://mwebview.sys.dweb/plugin/${host}${pathname}${search}`;
    const result = await this.nativeFetch(url, {
        body: ipcRequest.body.raw,
        headers: ipcRequest.headers,
        method: ipcRequest.method,
    });
    return result;
}
export async function createStreamIpc(args, client_ipc, ipcRequest) {
    const readableStreamIpcToTestFromSysDweb = new ReadableStreamIpc(this, IPC_ROLE.SERVER);
    readableStreamIpcToTestFromSysDweb.bindIncomeStream(ipcRequest.body.stream());
    readableStreamIpcToTestFromSysDweb.onEvent(event => {
        console.log('event: ', event);
    });
    readableStreamIpcToTestFromSysDweb.onStream(stream => {
        console.log('stream: ', stream.binary);
    });
    return readableStreamIpcToTestFromSysDweb.stream;
}
