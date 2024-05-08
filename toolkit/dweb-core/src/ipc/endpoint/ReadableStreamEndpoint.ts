import type { $PromiseMaybe } from "@dweb-browser/helper/$PromiseMaybe.ts";
import { Channel } from "@dweb-browser/helper/Channel.ts";
import { simpleDecoder } from "@dweb-browser/helper/encoding.ts";
import { streamRead } from "@dweb-browser/helper/stream/readableStreamHelper.ts";
import { $cborToEndpointMessage, $jsonToEndpointMessage } from "../helper/$messageToIpcMessage.ts";
import { CommonEndpoint } from "./CommonEndpoint.ts";
import { ENDPOINT_PROTOCOL } from "./EndpointLifecycle.ts";
import type { $EndpointRawMessage } from "./EndpointMessage.ts";

export class ReadableStreamEndpoint extends CommonEndpoint {
  override toString() {
    return `ReadableStreamEndpoint#${this.debugId}`;
  }

  constructor(debugId: string) {
    super(debugId);
  }

  #input = new Channel<Uint8Array>();

  /** 这是输出流，给外部读取用的 */
  get stream() {
    return this.#input.stream;
  }

  // 外部绑定流
  #incomne_stream?: ReadableStream<Uint8Array>;
  /**
   * 输入流要额外绑定
   * 注意，非必要不要 await 这个promise
   */
  async bindIncomeStream(stream: $PromiseMaybe<ReadableStream<Uint8Array>>) {
    if (this.#incomne_stream !== undefined) {
      throw new Error(`${this.debugId} in come stream alreay binded.`);
    }
    this.#incomne_stream = await stream;
    if (this.isClose) {
      console.error("already closed");
    }
    const reader = streamRead(this.#incomne_stream);
    (async () => {
      await this.awaitOpen("then-bindIncomeStream");
      for await (const data of reader) {
        let message: $EndpointRawMessage;

        if (this.protocol === ENDPOINT_PROTOCOL.CBOR) {
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
    this.#input.send(new TextEncoder().encode(data));
  }
  protected postBinaryMessage(data: Uint8Array) {
    this.#input.send(data);
  }
  //#endregion

  //#region close

  protected override beforeClose = () => {
    this.#input.closeWrite();
  };
  // 彻底关闭
  protected override afterClosed = () => {
    this.#input.close();
  };
  //#endregion
}
