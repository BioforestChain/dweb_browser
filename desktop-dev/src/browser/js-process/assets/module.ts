//#region helper
export { isWebSocket } from "../../../core/helper/ipcRequestHelper.ts";
export type { $MMID, $MicroModuleManifest } from "../../../core/helper/types.ts";
export * from "../../../helper/PromiseOut.ts";
export * from "../../../helper/binaryHelper.ts";
export type { $JmmAppInstallManifest, $JmmAppManifest } from "../../jmm/types.ts";
export * from "../../../helper/$debounce.ts";
export * from "../../../helper/createSignal.ts";
export * from "../../../helper/encoding.ts";
export * from "../../../helper/mapHelper.ts";
export * from "../../../helper/stream/readableStreamHelper.ts";
//#endregion

//#region runtime types
export type * from "./worker/index.ts";
export type * from "./worker/std-dweb-core.ts";

export type * from "./worker/std-dweb-http.ts";
import type * as $Core from "./worker/std-dweb-core.ts";

export const { jsProcess, http, ipc, core } = navigator.dweb;
export const {
  FetchError,
  IPC_METHOD,
  Ipc,
  IpcBodySender,
  IpcEvent,
  //
  IpcHeaders,
  IpcRequest,
  IpcResponse,
  ReadableStreamIpc,
  ReadableStreamOut,
  //
  PureChannel,
  PureFrameType,
  PureFrame,
  PureTextFrame,
  PureBinaryFrame,
} = ipc;

export const { ServerUrlInfo, ServerStartResult } = http;
export type $ServerUrlInfo = InstanceType<typeof ServerUrlInfo>;
export type $ServerStartResult = InstanceType<typeof ServerStartResult>;

export type $Ipc = $Core.Ipc;
export type $IpcRequest = $Core.IpcRequest;
export type $IpcResponse = $Core.IpcResponse;
export type $IpcEvent = $Core.IpcEvent;
export type $IpcHeaders = $Core.IpcHeaders;
export type $ReadableStreamIpc = $Core.ReadableStreamIpc;
export type $ReadableStreamOut<T> = $Core.ReadableStreamOut<T>;

export type $FetchError = $Core.FetchError;
export type $PureFrameType = $Core.PureFrameType;
export type $PureChannel = $Core.PureChannel;
export type $PureTextFrame = $Core.PureTextFrame;
export type $PureBinaryFrame = $Core.PureBinaryFrame;
export type $PureFrame = $Core.$PureFrame;
//#endregion
