import { $MicroModuleManifest } from "../../types.ts";
import type { IpcClientRequest } from "../ipc-message/IpcRequest.ts";
import type { Ipc } from "../ipc.ts";

// export type $IpcOptions = {
//   /**是否自动启动，可以在这个启动之前做一些动作默认不传递为自动启动 */
//   autoStart?: false;
//   /**远程的模块是谁*/
//   remote: $MicroModuleManifest;
//   /**当endpoint为Worker的时候需要传递*/
//   port?: MessagePort;
//   /**当endpoint为Kotlin的时候需要传递*/
//   channel?: MessagePort;
// };

// /**IpcPool*/
// export type $OnIpcPool = (
//   /// 这里只会有两种类型的数据
//   message: IpcPoolPack,
//   ipc: Ipc
// ) => unknown;

// export type $OnIpcMessage = (
//   /// 这里只会有两种类型的数据
//   message: $IpcMessage,
//   ipc: Ipc
// ) => unknown;

// export type $OnIpcRequestMessage = (
//   /// 这里只会有两种类型的数据
//   message: IpcRequest,
//   ipc: Ipc
// ) => unknown;
// export type $OnIpcEventMessage = (
//   /// 这里只会有两种类型的数据
//   message: IpcEvent,
//   ipc: Ipc
// ) => unknown;
// export type $OnIpcStreamMessage = (
//   /// 这里只会有两种类型的数据
//   message: $IpcStreamMessage,
//   ipc: Ipc
// ) => unknown;

// export type $OnIpcLifeCycleMessage = (
//   /// 这里只会有两种类型的数据
//   message: IpcLifeCycle,
//   ipc: Ipc
// ) => unknown;

// export type $OnIpcErrorMessage = (
//   /// 这里只会有两种类型的数据
//   message: IpcError,
//   ipc: Ipc
// ) => unknown;
