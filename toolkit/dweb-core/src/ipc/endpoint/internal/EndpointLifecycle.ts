/**endpoint 生命周期 */
export const enum ENDPOINT_LIFECYCLE_STATE {
  INIT = "init",
  OPENING = "opening",
  OPENED = "opened",
  CLOSING = "closing",
  CLOSED = "closed",
}

/**如果此处增加协议，需要注意endpoint的转码*/
export const enum ENDPOINT_PROTOCOL {
  JSON = "JSON",
  CBOR = "CBOR",
}

export type $EndpointLifecycleStateBase<S extends ENDPOINT_LIFECYCLE_STATE> = ReturnType<
  typeof endpointLifecycleStateBase<S>
>;
export const endpointLifecycleStateBase = <S extends ENDPOINT_LIFECYCLE_STATE>(state: S) => ({ state } as const);
