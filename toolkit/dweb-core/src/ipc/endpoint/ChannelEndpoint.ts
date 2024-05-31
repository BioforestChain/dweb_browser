import { Channel } from "@dweb-browser/helper/Channel.ts";
import { simpleDecoder, simpleEncoder } from "@dweb-browser/helper/encoding.ts";
import { streamRead } from "@dweb-browser/helper/stream/readableStreamHelper.ts";
import { $cborToEndpointMessage, $jsonToEndpointMessage } from "../helper/$messageToIpcMessage.ts";
import { CommonEndpoint } from "./CommonEndpoint.ts";
import { ENDPOINT_PROTOCOL } from "./EndpointLifecycle.ts";
import type { $EndpointRawMessage } from "./EndpointMessage.ts";

export class ChannelEndpoint extends CommonEndpoint {
  override toString() {
    return `ChannelEndpoint#${this.debugId}`;
  }

  constructor(debugId: string, incomne?: ReadableStream<Uint8Array>) {
    super(debugId);
    this.#bindIncomeStream(incomne);
  }
  /**输出流 */
  #outgoing = new Channel<Uint8Array>();
  /** 这是输出流，给外部读取用的 */
  get stream() {
    return this.#outgoing.stream;
  }
  /**输入流 */
  #incomne_stream = new Channel<Uint8Array | string>();

  /**对接输入流 */
  send(value: string | Uint8Array) {
    this.#incomne_stream.send(value);
  }
  /**
   * 输入流要额外绑定
   */
  #bindIncomeStream(incomne?: ReadableStream<Uint8Array | string>) {
    if (incomne == undefined) {
      incomne = this.#incomne_stream.stream;
    }
    const reader = streamRead(incomne);
    (async () => {
      for await (const data of reader) {
        let message: $EndpointRawMessage;
        if (typeof data === "string") {
          message = $jsonToEndpointMessage(data);
        } else if (this.protocol === ENDPOINT_PROTOCOL.CBOR) {
          message = $cborToEndpointMessage(data);
        } else {
          message = $jsonToEndpointMessage(simpleDecoder(data, "utf8"));
        }
        this.endpointMsgChannel.send(message);
      }
      /// 输入流结束，输出流也要一并关闭
      this.close();
    })();
  }

  //#region postMessage

  protected postTextMessage(data: string) {
    this.#outgoing.send(simpleEncoder(data, "utf8"));
  }
  protected postBinaryMessage(data: Uint8Array) {
    this.#outgoing.send(data);
  }
  //#endregion

  //#region close

  protected override beforeClose = () => {
    // 关闭写入流
    this.#incomne_stream.close();
  };
  // 彻底关闭
  protected override afterClosed = () => {
    this.#outgoing.close();
  };
  //#endregion
}
