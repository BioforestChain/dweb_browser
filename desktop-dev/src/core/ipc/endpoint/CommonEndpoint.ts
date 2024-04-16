import { encode } from "cbor-x";
import { StateSignal } from "../../../helper/StateSignal.ts";
import { Channel } from "../../helper/Channel.ts";
import { ENDPOINT_LIFECYCLE_STATE } from "../../index.ts";
import { EndpointLifecycle, EndpointProtocol } from "./EndpointLifecycle.ts";
import { $EndpointMessage, ENDPOINT_MESSAGE_TYPE, EndpointIpcMessage } from "./EndpointMessage.ts";
import { IpcEndpoint } from "./IpcEndpoint.ts";
export abstract class CommonEndpoint extends IpcEndpoint {
  /**
   * 默认使用 Json 这种最通用的协议
   * 在一开始的握手阶段会强制使用
   */
  #protocol = EndpointProtocol.Json;
  get protocol() {
    return this.#protocol;
  }

  /**
   * 单讯息通道
   */
  protected endpointMsgChannel = new Channel<$EndpointMessage>();

  #lifecycleRemoteMutableFlow = new StateSignal(EndpointLifecycle.init());

  readonly lifecycleRemoteFlow = this.#lifecycleRemoteMutableFlow.asReadyonly();

  /**初始化支持的协议 */
  protected override getLocaleSubProtocols() {
    return new Set([EndpointProtocol.Json, EndpointProtocol.Cbor]);
  }

  /**向远端发送声明周期 */
  protected override sendLifecycleToRemote(state: EndpointLifecycle) {
    console.log("lifecycle-out", this, state);
    if (EndpointProtocol.Cbor === this.protocol) {
      return this.postBinaryMessage(encode(state));
    }
    if (EndpointProtocol.Json === this.protocol) {
      return this.postTextMessage(JSON.stringify(state));
    }
  }

  /**
   * 使用协商的结果来进行接下来的通讯
   */
  override async doStart(): Promise<void> {
    this.lifecycleLocaleFlow.listen((state) => {
      if (state.state === ENDPOINT_LIFECYCLE_STATE.OPENED) {
        if (state.subProtocols.has(EndpointProtocol.Cbor)) {
          this.#protocol = EndpointProtocol.Cbor;
        }
      }
    });
    (async () => {
      for await (const endpointMessage of this.endpointMsgChannel) {
        switch (endpointMessage.type) {
          case ENDPOINT_MESSAGE_TYPE.IPC: {
            const producer = this.getIpcMessageProducer(endpointMessage.pid);
            producer.trySend(endpointMessage.ipcMessage);
            break;
          }
          case ENDPOINT_MESSAGE_TYPE.LIFECYCLE: {
            this.#lifecycleRemoteMutableFlow.emit(endpointMessage);
            break;
          }
        }
      }
    })();
  }

  /**
   * 发送 EndpointIpcMessage
   */
  override async postIpcMessage(msg: EndpointIpcMessage) {
    await this.awaitOpen("then-postIpcMessage");
    if (EndpointProtocol.Json === this.#protocol) {
      return this.postTextMessage(JSON.stringify(msg));
    }
    if (EndpointProtocol.Cbor === this.#protocol) {
      return this.postBinaryMessage(encode(msg));
    }
  }

  /**
   * 发送文本类型的消息
   */
  protected abstract postTextMessage(data: String): void;

  /**
   * 发送二进制类型的消息
   */
  protected abstract postBinaryMessage(data: Uint8Array): void;
}
