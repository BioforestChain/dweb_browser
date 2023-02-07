import { decode } from "@msgpack/msgpack";
import { IPC_DATA_TYPE, type $IpcMessage } from "../ipc/const.cjs";
import type { Ipc } from "../ipc/ipc.cjs";
import { IpcRequest } from "../ipc/IpcRequest.cjs";
import { IpcResponse } from "../ipc/IpcResponse.cjs";
import { IpcStreamData } from "../ipc/IpcStreamData.cjs";
import { IpcStreamEnd } from "../ipc/IpcStreamEnd.cjs";
import { IpcStreamPull } from "../ipc/IpcStreamPull.cjs";

type $JSON<T> = {
  [key in keyof T]: T[key] extends Function ? never : T[key];
};

export const $messageToIpcMessage = (
  data: $JSON<$IpcMessage> | "close" | Uint8Array,
  ipc: Ipc
) => {
  if (data instanceof Uint8Array) {
    data = decode(data) as $JSON<$IpcMessage>;
  }
  let message: undefined | $IpcMessage | "close";

  if (data === "close") {
    message = data;
  } else if (data.type === IPC_DATA_TYPE.REQUEST) {
    message = new IpcRequest(
      data.req_id,
      data.method,
      data.url,
      data.rawBody,
      data.headers,
      ipc
    );
  } else if (data.type === IPC_DATA_TYPE.RESPONSE) {
    message = new IpcResponse(
      data.req_id,
      data.statusCode,
      data.rawBody,
      data.headers,
      ipc
    );
  } else if (data.type === IPC_DATA_TYPE.STREAM_DATA) {
    message = new IpcStreamData(data.stream_id, data.data);
  } else if (data.type === IPC_DATA_TYPE.STREAM_PULL) {
    message = new IpcStreamPull(data.stream_id, data.desiredSize);
  } else if (data.type === IPC_DATA_TYPE.STREAM_END) {
    message = new IpcStreamEnd(data.stream_id);
  }
  return message;
};
