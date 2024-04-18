import { $cborToEndpointMessage, $jsonToEndpointMessage } from "../helper/$messageToIpcMessage.ts";
import { CommonEndpoint } from "./CommonEndpoint.ts";
import { ENDPOINT_PROTOCOL } from "./EndpointLifecycle.ts";
import type { $EndpointRawMessage } from "./EndpointMessage.ts";
export class WebMessageEndpoint extends CommonEndpoint {
  override toString(): string {
    return `WebMessageEndpoint#${this.debugId}`;
  }

  constructor(readonly port: MessagePort, debugId: string) {
    super(debugId);
    port.addEventListener("message", (event) => {
      let message: $EndpointRawMessage;

      if (this.protocol === ENDPOINT_PROTOCOL.CBOR) {
        message = $cborToEndpointMessage(event.data);
      } else {
        message = $jsonToEndpointMessage(event.data);
      }

      this.endpointMsgChannel.send(message);
    });
    port.start();
  }

  override doStart() {
    this.port.start();
    return super.doStart();
  }

  protected postTextMessage(data: String) {
    this.port.postMessage(data);
  }
  protected postBinaryMessage(data: Uint8Array) {
    this.port.postMessage(data);
  }
  protected override beforeClose = () => {};
}
