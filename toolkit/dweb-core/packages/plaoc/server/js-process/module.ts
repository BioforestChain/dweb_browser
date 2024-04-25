//#region helper
export { isWebSocket } from "@dweb-browser/core/helper";
export type { $MMID, $MicroModuleManifest } from "@dweb-browser/core/helper";
export * from "@dweb-browser/core/helper/PromiseOut.ts";
export * from "@dweb-browser/core/helper/createSignal.ts";
export * from "@dweb-browser/core/helper/decorator/$debounce.ts";
export * from "@dweb-browser/core/helper/decorator/$once.ts";
export * from "@dweb-browser/core/helper/encoding.ts";
export * from "@dweb-browser/core/helper/fun/binaryHelper.ts";
export * from "@dweb-browser/core/helper/fun/mapHelper.ts";
export * from "@dweb-browser/core/helper/stream/readableStreamHelper.ts";
export type { $JmmAppInstallManifest, $JmmAppManifest } from "./types.ts";
//#endregion

//#region runtime types
export type * from "@dweb-browser/core/worker-process/worker/index.ts";
export * from "@dweb-browser/core/worker-process/worker/std-dweb-core.ts";
export type * from "@dweb-browser/core/worker-process/worker/std-dweb-http.ts";

import type * as $Core from "@dweb-browser/core/worker-process/worker/std-dweb-core.ts";

export const { jsProcess, http, ipc, core } = navigator.dweb;
export const {
  FetchError,
  Ipc,
  IpcBodySender,
  IpcEvent,
  //
  IpcHeaders,
  IpcClientRequest,
  IpcResponse,
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
export type $IpcEvent = $Core.$IpcEvent;
export type $IpcHeaders = $Core.IpcHeaders;
export type $ReadableStreamOut<T> = $Core.ReadableStreamOut<T>;

export type $FetchError = $Core.FetchError;
export type $PureFrameType = $Core.PureFrameType;
export type $PureChannel = $Core.PureChannel;
export type $PureTextFrame = $Core.PureTextFrame;
export type $PureBinaryFrame = $Core.PureBinaryFrame;
export type $PureFrame = $Core.$PureFrame;
//#endregion
