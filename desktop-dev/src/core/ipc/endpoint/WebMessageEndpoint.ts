import { decode } from "cbor-x";
import { CommonEndpoint } from "./CommonEndpoint.ts";
import { EndpointProtocol } from "./EndpointLifecycle.ts";
import { $EndpointMessage } from "./EndpointMessage.ts";
export class WebMessageEndpoint extends CommonEndpoint {
  debugId = "WebMessageEndpoint";

  constructor(readonly port: MessagePort, name?: string) {
    super();
    this.debugId += name;
    port.addEventListener("message", (event) => {
      let message: $EndpointMessage;
      if (this.protocol === EndpointProtocol.Cbor) {
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
