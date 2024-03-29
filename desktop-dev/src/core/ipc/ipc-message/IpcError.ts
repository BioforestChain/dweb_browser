import { IPC_MESSAGE_TYPE, ipcMessageBase } from "./internal/IpcMessage.ts";

export type $IpcError = ReturnType<typeof ipcError>;

export const ipcError = Object.assign(
  (errorCode: number, message?: string) =>
    ({
      ...ipcMessageBase(IPC_MESSAGE_TYPE.ERROR),
      errorCode,
      message,
    } as const),
  {
    internalServer: (message: string) => ipcError(500, message),
  }
);
