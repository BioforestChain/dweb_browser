import { IPC_MESSAGE_TYPE, ipcMessageBase } from "./internal/IpcMessage.ts";

export type $IpcError = ReturnType<typeof IpcError>;

export const IpcError = Object.assign(
  (errorCode: number, message?: string) =>
    ({
      ...ipcMessageBase(IPC_MESSAGE_TYPE.ERROR),
      errorCode,
      message,
    } as const),
  {
    internalServer: (message: string) => IpcError(500, message),
  }
);
