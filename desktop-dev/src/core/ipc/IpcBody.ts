import { simpleDecoder, simpleEncoder } from "../../helper/encoding.ts";
import { streamReadAllBuffer } from "../../helper/stream/readableStreamHelper.ts";
import type { IpcBodySender } from "./IpcBodySender.ts";
import type { MetaBody } from "./MetaBody.ts";
import { Ipc } from "./ipc.ts";

export abstract class IpcBody {
  static CACHE = new (class {
    /**
     * 任意的 RAW 背后都会有一个 IpcBodySender/IpcBodyReceiver
     * 将它们缓存起来，那么使用这些 RAW 确保只拿到同一个 IpcBody，这对 RAW-Stream 很重要，流不可以被多次打开读取
     */
    raw_ipcBody_WMap = new WeakMap<Uint8Array | ReadableStream, IpcBody>();
    /**
     * 每一个 metaBody 背后，都会有第一个 接收者IPC，这直接定义了它的应该由谁来接收这个数据，
     * 其它的 IPC 即便拿到了这个 metaBody 也是没有意义的，除非它是 INLINE
     */
    metaId_receiverIpc_Map = new Map<string, Ipc>();

    /**
     * 每一个 metaBody 背后，都会有一个 IpcBodySender,
     * 这里主要是存储 流，因为它有明确的 open/close 生命周期
     */
    metaId_ipcBodySender_Map = new Map<string, IpcBodySender>();
  })();

  abstract readonly metaBody: MetaBody;
  protected abstract _bodyHub: BodyHub;
  get raw() {
    return this._bodyHub.data;
  }

  async u8a() {
    /// 首先要确保 body 已经被绑定上去
    const bodyHub = this._bodyHub;
    let body_u8a = bodyHub.u8a;
    if (body_u8a === undefined) {
      if (bodyHub.stream) {
        body_u8a = await streamReadAllBuffer(bodyHub.stream);
      } else if (bodyHub.text !== undefined) {
        body_u8a = simpleEncoder(bodyHub.text, "utf8");
      } else {
        throw new Error(`invalid body type`);
      }
      bodyHub.u8a = body_u8a;
      IpcBody.CACHE.raw_ipcBody_WMap.set(body_u8a, this);
    }
    return body_u8a;
  }
  async stream() {
    /// 首先要确保 body 已经被绑定上去
    const bodyHub = this._bodyHub;
    let body_stream = bodyHub.stream;
    if (body_stream === undefined) {
      body_stream = new Blob([await this.u8a()]).stream();
      bodyHub.stream = body_stream;
      IpcBody.CACHE.raw_ipcBody_WMap.set(body_stream, this);
    }
    return body_stream;
  }
  async text() {
    const bodyHub = this._bodyHub;
    let body_text = bodyHub.text;
    if (body_text === undefined) {
      body_text = simpleDecoder(await this.u8a(), "utf8");
      bodyHub.text = body_text;
    }
    return body_text;
  }
}

export class BodyHub {
  constructor(readonly data: $BodyData) {
    if (typeof data === "string") {
      this.text = data;
    } else if (data instanceof ReadableStream) {
      this.stream = data;
    } else {
      this.u8a = data;
    }
  }
  u8a?: Uint8Array;
  stream?: ReadableStream<Uint8Array>;
  text?: string;
}
export type $BodyData = Uint8Array | ReadableStream<Uint8Array> | string;
