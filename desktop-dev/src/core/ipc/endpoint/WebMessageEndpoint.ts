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
      const rawData = event.data;
      let message: $EndpointRawMessage;
      if (this.protocol === ENDPOINT_PROTOCOL.CBOR && typeof rawData !== "string") {
        message = $cborToEndpointMessage(rawData);
      } else {
        message = $jsonToEndpointMessage(rawData);
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
