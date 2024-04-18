import type { $EndpointIpcMessage } from "./EndpointIpcMessage.ts";
import type { $EndpointLifecycle } from "./EndpointLifecycle.ts";

export { ENDPOINT_MESSAGE_TYPE } from "./internal/EndpointMessage.ts";

export type $EndpointMessage = $EndpointIpcMessage | $EndpointLifecycle;
