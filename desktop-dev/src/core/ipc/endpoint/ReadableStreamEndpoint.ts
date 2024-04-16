import { decode } from "cbor-x";
import { simpleDecoder } from "../../../helper/encoding";
import { streamRead } from "../../../helper/stream/readableStreamHelper";
import { Channel } from "../../helper/Channel";
import { $PromiseMaybe } from "../../helper/types";
import { $JSON } from "../helper/$messageToIpcMessage";
import { CommonEndpoint } from "./CommonEndpoint";
import { EndpointProtocol } from "./EndpointLifecycle";
import { $EndpointMessage, EndpointIpcMessage } from "./EndpointMessage";

export class ReadableStreamEndpoint extends CommonEndpoint {
  debugId = "ReadableStreamEndpoint";

  constructor(name?: string) {
    super();
    this.debugId += name;
  }

  #input = new Channel<string | Uint8Array>();

  /** 这是输出流，给外部读取用的 */
  get stream() {
    return this.#input.stream;
  }

  signal = new AbortSignal();

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
        let packMessage: $EndpointMessage;
        if (this.protocol === EndpointProtocol.Cbor) {
          packMessage = decode(data);
        } else {
          packMessage = JSON.parse(simpleDecoder(data, "utf8")) as $JSON<EndpointIpcMessage>;
        }
        this.endpointMsgChannel.send(packMessage);
      }
    })();

    /// 输入流结束，输出流也要一并关闭
    this.close();
  }

  //#region postMessage

  protected postTextMessage(data: string) {
    this.#input.send(data);
  }
  protected postBinaryMessage(data: Uint8Array) {
    this.#input.send(data);
  }

  //#region close

  protected beforeClose = () => {
    this.#input.closeWrite();
  };
  // 彻底关闭
  protected afterClosed = () => {
    this.#input.close();
  };
}
