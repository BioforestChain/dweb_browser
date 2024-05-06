import { Channel } from "@dweb-browser/helper/Channel.ts";
import { StateSignal } from "@dweb-browser/helper/StateSignal.ts";
import { encode } from "cbor-x";
import {
  $endpointMessageToCbor,
  $endpointMessageToJson,
  $normalizeIpcMessage,
} from "../helper/$messageToIpcMessage.ts";
import type { $EndpointIpcMessage } from "./EndpointIpcMessage.ts";
import {
  ENDPOINT_LIFECYCLE_STATE,
  ENDPOINT_PROTOCOL,
  endpointLifecycle,
  endpointLifecycleInit,
  type $EndpointLifecycle,
} from "./EndpointLifecycle.ts";
import { ENDPOINT_MESSAGE_TYPE, type $EndpointRawMessage } from "./EndpointMessage.ts";
import { IpcEndpoint } from "./IpcEndpoint.ts";
export abstract class CommonEndpoint extends IpcEndpoint {
  /**
   * 默认使用 Json 这种最通用的协议
   * 在一开始的握手阶段会强制使用
   */
  #protocol = ENDPOINT_PROTOCOL.JSON;
  get protocol() {
    return this.#protocol;
  }

  /**
   * 单讯息通道
   */
  protected endpointMsgChannel = new Channel<$EndpointRawMessage>();

  #lifecycleRemoteMutableFlow = new StateSignal<$EndpointLifecycle>(
    endpointLifecycle(endpointLifecycleInit()),
    endpointLifecycle.equals
  );

  readonly lifecycleRemoteFlow = this.#lifecycleRemoteMutableFlow.asReadyonly();

  /**初始化支持的协议 */
  protected override getLocaleSubProtocols() {
    return new Set([ENDPOINT_PROTOCOL.JSON]); //ENDPOINT_PROTOCOL.CBOR
  }

  /**向远端发送声明周期 */
  protected override sendLifecycleToRemote(state: $EndpointLifecycle) {
    this.console.log("lifecycle-out", state);
    if (ENDPOINT_PROTOCOL.CBOR === this.protocol) {
      return this.postBinaryMessage(encode(state));
    }
    if (ENDPOINT_PROTOCOL.JSON === this.protocol) {
      return this.postTextMessage(JSON.stringify(state));
    }
  }

  /**
   * 使用协商的结果来进行接下来的通讯
   */
  // deno-lint-ignore require-await
  override async doStart(): Promise<void> {
    this.lifecycleLocaleFlow.listen((lifecycle) => {
      // 如果连接成功，就开始对接协议
      if (lifecycle.state.name === ENDPOINT_LIFECYCLE_STATE.OPENED) {
        if (lifecycle.state.subProtocols.includes(ENDPOINT_PROTOCOL.CBOR)) {
          this.#protocol = ENDPOINT_PROTOCOL.CBOR;
        }
      }
    });
    // 分发消息或者声明周期
    (async () => {
      for await (const endpointMessage of this.endpointMsgChannel) {
        switch (endpointMessage.type) {
          case ENDPOINT_MESSAGE_TYPE.IPC: {
            const producer = this.getIpcMessageProducer(endpointMessage.pid);
            const ipc = await producer.ipcPo.promise;
            void producer.producer.trySend($normalizeIpcMessage(endpointMessage.ipcMessage, ipc));
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
  override async postIpcMessage(msg: $EndpointIpcMessage) {
    await this.awaitOpen("then-postIpcMessage");
    switch (this.#protocol) {
      case ENDPOINT_PROTOCOL.JSON:
        this.postTextMessage($endpointMessageToJson(msg));
        break;
      case ENDPOINT_PROTOCOL.CBOR:
        this.postBinaryMessage($endpointMessageToCbor(msg));
        break;
    }
  }

  /**
   * 发送文本类型的消息
   */
  protected abstract postTextMessage(data: string): void;

  /**
   * 发送二进制类型的消息
   */
  protected abstract postBinaryMessage(data: Uint8Array): void;
}
