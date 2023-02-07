import { simpleDecoder, simpleEncoder } from "../../helper/encoding.cjs";
import { streamReadAllBuffer } from "../../helper/readableStreamHelper.cjs";
import { $rawDataToBody } from "./$rawDataToBody.cjs";
import type { $RawData } from "./const.cjs";
import type { Ipc } from "./ipc.cjs";

export class IpcBody {
  constructor(readonly rawBody: $RawData, ipc: Ipc) {
    this.#ipc = ipc;
  }
  #body?: {
    data: ReturnType<typeof $rawDataToBody>;
    u8a?: Uint8Array;
    stream?: ReadableStream<Uint8Array>;
    text?: string;
  };
  #ipc: Ipc;
  get body() {
    return this._initBody().data;
  }
  private _initBody() {
    if (this.#body === undefined) {
      const data = $rawDataToBody(this.rawBody, this.#ipc);
      this.#body = {
        data,
      };
      if (data instanceof ReadableStream) {
        this.#body.stream = data;
      } else if (data instanceof Uint8Array) {
        this.#body.u8a = data;
      } else if (typeof data === "string") {
        this.#body.text = data;
      }
    }
    return this.#body;
  }
  async u8a() {
    /// 首先要确保 body 已经被绑定上去
    const body = this._initBody();
    let body_u8a = body.u8a;
    if (body_u8a === undefined) {
      if (body.stream) {
        body_u8a = await streamReadAllBuffer(body.stream);
      } else if (body.text !== undefined) {
        body_u8a = simpleEncoder(body.text, "utf8");
      } else {
        throw new Error(`invalid body type`);
      }
      body.u8a = body_u8a;
    }
    return body_u8a;
  }
  async stream() {
    /// 首先要确保 body 已经被绑定上去
    const body = this._initBody();
    let body_stream = body.stream;
    if (body_stream === undefined) {
      body_stream = new Blob([await this.u8a()]).stream();
      body.stream = body_stream;
    }
    return body_stream;
  }
  async text() {
    const body = await this._initBody();
    let body_text = body.text;
    if (body_text === undefined) {
      body_text = simpleDecoder(await this.u8a(), "utf8");
      body.text = body_text;
    }
    return body_text;
  }
}
