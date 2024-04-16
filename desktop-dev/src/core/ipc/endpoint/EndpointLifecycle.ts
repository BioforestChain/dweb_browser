import { ENDPOINT_LIFECYCLE_STATE } from "../helper/const.ts";
import { ENDPOINT_MESSAGE_TYPE, EndpointMessage } from "./EndpointMessage.ts";

export class EndpointLifecycle extends EndpointMessage<ENDPOINT_MESSAGE_TYPE.LIFECYCLE> {
  constructor(readonly state: ENDPOINT_LIFECYCLE_STATE, readonly subProtocols = new Set<EndpointProtocol>()) {
    super(ENDPOINT_MESSAGE_TYPE.LIFECYCLE);
  }

  static init = (subProtocols = new Set<EndpointProtocol>()) =>
    new EndpointLifecycle(ENDPOINT_LIFECYCLE_STATE.INIT, subProtocols);

  static opening = (subProtocols = new Set<EndpointProtocol>()) =>
    new EndpointLifecycle(ENDPOINT_LIFECYCLE_STATE.OPENING, subProtocols);
  static opend = (subProtocols = new Set<EndpointProtocol>()) =>
    new EndpointLifecycle(ENDPOINT_LIFECYCLE_STATE.OPENED, subProtocols);

  static closing = (subProtocols = new Set<EndpointProtocol>()) =>
    new EndpointLifecycle(ENDPOINT_LIFECYCLE_STATE.OPENING, subProtocols);
  static closed = (subProtocols = new Set<EndpointProtocol>()) =>
    new EndpointLifecycle(ENDPOINT_LIFECYCLE_STATE.CLOSED, subProtocols);
}

/**如果此处增加协议，需要注意endpoint的转码*/
export const enum EndpointProtocol {
  Json,
  Cbor,
}
