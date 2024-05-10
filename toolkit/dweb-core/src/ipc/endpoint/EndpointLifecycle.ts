import type { OrderBy } from "@dweb-browser/helper/OrderBy.ts";
import { ENDPOINT_LIFECYCLE_STATE, ENDPOINT_PROTOCOL } from "./internal/EndpointLifecycle.ts";
import { ENDPOINT_MESSAGE_TYPE, endpointMessageBase } from "./internal/EndpointMessage.ts";
export { ENDPOINT_LIFECYCLE_STATE, ENDPOINT_PROTOCOL } from "./internal/EndpointLifecycle.ts";

export type $EndpointLifecycleState =
  | $EndpointLifecycleInit
  | $EndpointLifecycleOpening
  | $EndpointLifecycleOpened
  | $EndpointLifecycleClosing
  | $EndpointLifecycleClosed;
export type $EndpointLifecycleStateBase<S extends ENDPOINT_LIFECYCLE_STATE> = ReturnType<
  typeof endpointLifecycleStateBase<S>
>;
export const endpointLifecycleStateBase = <S extends ENDPOINT_LIFECYCLE_STATE>(name: S) => ({ name } as const);
export type $EndpointLifecycleInit = ReturnType<typeof endpointLifecycleInit>;
export type $EndpointLifecycleOpening = ReturnType<typeof endpointLifecycleOpening>;
export type $EndpointLifecycleOpened = ReturnType<typeof endpointLifecycleOpend>;
export type $EndpointLifecycleClosing = ReturnType<typeof endpointLifecycleClosing>;
export type $EndpointLifecycleClosed = ReturnType<typeof endpointLifecycleClosed>;

export const endpointLifecycleInit = () => endpointLifecycleStateBase(ENDPOINT_LIFECYCLE_STATE.INIT);
export const endpointLifecycleOpening = (subProtocols: Iterable<ENDPOINT_PROTOCOL>, sessionIds: Iterable<string>) =>
  ({
    ...endpointLifecycleStateBase(ENDPOINT_LIFECYCLE_STATE.OPENING),
    subProtocols: [...subProtocols],
    sessionIds: [...sessionIds],
  } as const);
export const endpointLifecycleOpend = (subProtocols: Iterable<ENDPOINT_PROTOCOL>, sessionId: string) =>
  ({
    ...endpointLifecycleStateBase(ENDPOINT_LIFECYCLE_STATE.OPENED),
    subProtocols: [...subProtocols],
    sessionId,
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

export type $EndpointLifecycle<T extends $EndpointLifecycleState = $EndpointLifecycleState> = ReturnType<
  typeof endpointLifecycle<T>
>;

const endpointLifecycle = <T extends $EndpointLifecycleState>(
  state: T,
  order: number = ENDPOINT_LIFECYCLE_DEFAULT_ORDER
) =>
  ({
    ...endpointMessageBase(ENDPOINT_MESSAGE_TYPE.LIFECYCLE),
    state,
    order,
  } as const satisfies OrderBy);

const ENDPOINT_LIFECYCLE_DEFAULT_ORDER = -1;
export const EndpointLifecycle = Object.assign(endpointLifecycle, {
  equals: (a: $EndpointLifecycle, b: $EndpointLifecycle) => {
    return EndpointLifecycle.stateEquals(a.state, b.state);
  },
  stateEquals: (a: $EndpointLifecycleState, b: $EndpointLifecycleState) => {
    if (a.name !== b.name) {
      return false;
    }
    if (a.name === ENDPOINT_LIFECYCLE_STATE.CLOSING) {
      return a.reason === (b as $EndpointLifecycleClosing).reason;
    }
    if (a.name === ENDPOINT_LIFECYCLE_STATE.CLOSED) {
      return a.reason === (b as $EndpointLifecycleClosed).reason;
    }
    if (a.name === ENDPOINT_LIFECYCLE_STATE.OPENING || a.name === ENDPOINT_LIFECYCLE_STATE.OPENED) {
      return JSON.stringify(a) === JSON.stringify(b);
    }
    return true;
  },
  DEFAULT_ORDER: ENDPOINT_LIFECYCLE_DEFAULT_ORDER,
  init: endpointLifecycleInit,
  opening: endpointLifecycleOpening,
  opend: endpointLifecycleOpend,
  closing: endpointLifecycleClosing,
  closed: endpointLifecycleClosed,
});
