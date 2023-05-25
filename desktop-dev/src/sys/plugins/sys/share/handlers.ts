import { ReadableStreamIpc } from "../../../../core/ipc-web/ReadableStreamIpc.ts";
import { Ipc, IpcRequest, IPC_ROLE } from "../../../../core/ipc/index.ts";
import type { $Schema1, $Schema1ToType } from "../../../../helper/types.ts";
import type { ShareNMM } from "./share.main.ts";

/**
 * 设置状态
 * @param this
 * @param _args
 * @param _client_ipc
 * @param ipcRequest
 * @returns
 */
export async function share(
  this: ShareNMM,
  _args: $Schema1ToType<$Schema1>,
  _client_ipc: Ipc,
  ipcRequest: IpcRequest
) {
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

export async function createStreamIpc(
  this: ShareNMM,
  _args: $Schema1ToType<$Schema1>,
  _client_ipc: Ipc,
  ipcRequest: IpcRequest
) {
  const readableStreamIpcToTestFromSysDweb = new ReadableStreamIpc(
    this,
    IPC_ROLE.SERVER
  );
  readableStreamIpcToTestFromSysDweb.bindIncomeStream(ipcRequest.body.stream());
  readableStreamIpcToTestFromSysDweb.onEvent((event) => {
    console.log("event: ", event);
  });
  readableStreamIpcToTestFromSysDweb.onStream((stream) => {
    console.log("stream: ", (stream as any).binary);
  });

  return readableStreamIpcToTestFromSysDweb.stream;
}
