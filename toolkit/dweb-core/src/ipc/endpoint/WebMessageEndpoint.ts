import { setHelper } from "@dweb-browser/helper/fun/setHelper.ts";
import { $cborToEndpointMessage, $jsonToEndpointMessage } from "../helper/$messageToIpcMessage.ts";
import { CommonEndpoint } from "./CommonEndpoint.ts";
import { ENDPOINT_PROTOCOL } from "./EndpointLifecycle.ts";
import type { $EndpointRawMessage } from "./EndpointMessage.ts";
import { ENDPOINT_LIFECYCLE_STATE } from "./internal/EndpointLifecycle.ts";
declare global {
  interface Navigator {
    locks?: LockManager;
  }
}
export class WebMessageEndpoint extends CommonEndpoint {
  override toString(): string {
    return `WebMessageEndpoint#${this.debugId}`;
  }

  constructor(readonly port: MessagePort, debugId: string) {
    super(debugId);
  }

  override doStart() {
    const locks = navigator.locks;
    if (locks) {
      this.console.verbose("request-localSessionId", this.localSessionId);
      void locks.request(this.localSessionId, () => this.closedPo.promise);
      this.onLifecycle(async (lifecycle) => {
        const localState = lifecycle.state;
        if (localState.name === ENDPOINT_LIFECYCLE_STATE.OPENED) {
          const remoteLock = await locks.query();
          const remoteSessionId = [...setHelper.subtract(localState.sessionPair.split("~"), [this.localSessionId])][0];
          if (remoteLock.held?.find((h) => h.name === remoteSessionId)) {
            this.console.verbose("request-remoteSessionId", remoteSessionId);
            locks.request(remoteSessionId, { mode: "shared" }, () => {
              this.console.verbose("remoteSessionId-closed", remoteSessionId);
              this.close(`remote ipcEndpoint closed`);
            });
          }
        }
      });
    }
    this.port.addEventListener("message", (event) => {
      const rawData = event.data;
      this.console.verbose("in", rawData);
      let message: $EndpointRawMessage;
      if (this.protocol === ENDPOINT_PROTOCOL.CBOR && typeof rawData !== "string") {
        message = $cborToEndpointMessage(rawData);
      } else {
        message = $jsonToEndpointMessage(rawData);
      }
      // 发送消息到对方的endpoint
      this.endpointMsgChannel.send(message);
    });
    this.port.start();
    return super.doStart();
  }

  protected postTextMessage(data: string) {
    this.port.postMessage(data);
  }
  protected postBinaryMessage(data: Uint8Array) {
    this.port.postMessage(data);
  }
  protected override beforeClose = () => {};
}
