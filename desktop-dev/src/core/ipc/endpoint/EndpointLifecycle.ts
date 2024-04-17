import { ENDPOINT_LIFECYCLE_STATE, ENDPOINT_PROTOCOL } from "./internal/EndpointLifecycle";
import { $EndpointMessageBase, ENDPOINT_MESSAGE_TYPE, endpointMessageBase } from "./internal/EndpointMessage";
export { ENDPOINT_LIFECYCLE_STATE, ENDPOINT_PROTOCOL } from "./internal/EndpointLifecycle";

export interface $EndpointLifecycle extends $EndpointMessageBase<ENDPOINT_MESSAGE_TYPE.LIFECYCLE> {
  readonly state: $EndpointLifecycleState;
}
export const endpointLifecycle = <T extends $EndpointLifecycleState>(state: T) =>
  ({
    ...endpointMessageBase(ENDPOINT_MESSAGE_TYPE.LIFECYCLE),
    state,
  } as const);

export type $EndpointLifecycleState =
  | $EndpointLifecycleInit
  | $EndpointLifecycleOpening
  | $EndpointLifecycleOpened
  | $EndpointLifecycleOpening
  | $EndpointLifecycleClosed;
export type $EndpointLifecycleStateBase<S extends ENDPOINT_LIFECYCLE_STATE> = ReturnType<
  typeof endpointLifecycleStateBase<S>
>;
export const endpointLifecycleStateBase = <S extends ENDPOINT_LIFECYCLE_STATE>(state: S) => ({ state } as const);
export type $EndpointLifecycleInit = ReturnType<typeof endpointLifecycleInit>;
export type $EndpointLifecycleOpening = ReturnType<typeof endpointLifecycleOpening>;
export type $EndpointLifecycleOpened = ReturnType<typeof endpointLifecycleOpend>;
export type $EndpointLifecycleClosing = ReturnType<typeof endpointLifecycleClosing>;
export type $EndpointLifecycleClosed = ReturnType<typeof endpointLifecycleClosed>;

export const endpointLifecycleInit = () => endpointLifecycleStateBase(ENDPOINT_LIFECYCLE_STATE.INIT);
export const endpointLifecycleOpening = (subProtocols: Array<ENDPOINT_PROTOCOL>) =>
  ({
    ...endpointLifecycleStateBase(ENDPOINT_LIFECYCLE_STATE.OPENING),
    subProtocols,
  } as const);
export const endpointLifecycleOpend = (subProtocols: Array<ENDPOINT_PROTOCOL>) =>
  ({
    ...endpointLifecycleStateBase(ENDPOINT_LIFECYCLE_STATE.OPENED),
    subProtocols,
  } as const);
export const endpointLifecycleClosing = (reason?: string) =>
  ({
    ...endpointLifecycleStateBase(ENDPOINT_LIFECYCLE_STATE.CLOSING),
    reason,
  } as const);
export const endpointLifecycleClosed = (reason?: string) =>
  ({
    ...endpointLifecycleStateBase(ENDPOINT_LIFECYCLE_STATE.CLOSED),
    reason,
  } as const);
