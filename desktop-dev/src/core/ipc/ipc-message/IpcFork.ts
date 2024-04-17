import { $CommonAppManifest } from "../../types.ts";
import { IPC_MESSAGE_TYPE, ipcMessageBase } from "./internal/IpcMessage.ts";

export type $IpcFork = ReturnType<typeof ipcFork>;
/**
 * 这里会告知 fork的发起者 是否是自动启动，以及自启动的原因
 * 接受者可以用来参考，但无需遵循一致，唯一需要一致的只有 pid
 */
const ipcFork = (
  pid: number,
  autoStart: boolean,
  locale: $CommonAppManifest,
  remote: $CommonAppManifest,
  startReason?: string
) =>
  ({
    ...ipcMessageBase(IPC_MESSAGE_TYPE.FORK),
    pid,
    autoStart,
    locale,
    remote,
    startReason,
  } as const);
