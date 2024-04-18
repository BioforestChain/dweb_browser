import { decode } from "cbor-x";
import { CommonEndpoint } from "./CommonEndpoint.ts";
import { ENDPOINT_PROTOCOL } from "./EndpointLifecycle.ts";
import type { $EndpointMessage } from "./EndpointMessage.ts";
export class WebMessageEndpoint extends CommonEndpoint {
  readonly debugId = `WME-${this.name}`;

  constructor(readonly port: MessagePort, private name?: string) {
    super();
    port.addEventListener("message", (event) => {
      let message: $EndpointMessage;

      if (this.protocol === ENDPOINT_PROTOCOL.CBOR) {
        message = decode(event.data);
      } else {
        message = JSON.parse(event.data);
      }

      this.endpointMsgChannel.send(message);
    });
    port.start();
  }

  protected postTextMessage(data: String) {
    this.port.postMessage(data);
  }
  protected postBinaryMessage(data: Uint8Array) {
    this.port.postMessage(data);
  }
  protected override beforeClose = () => {};
}
