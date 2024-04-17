/**消息类型 */
export const enum ENDPOINT_MESSAGE_TYPE {
  LIFECYCLE = "life",
  IPC = "ipc",
}

export type $EndpointMessageBase<T extends ENDPOINT_MESSAGE_TYPE> = ReturnType<typeof endpointMessageBase<T>>;
export const endpointMessageBase = <T extends ENDPOINT_MESSAGE_TYPE>(type: T) => ({ type } as const);
