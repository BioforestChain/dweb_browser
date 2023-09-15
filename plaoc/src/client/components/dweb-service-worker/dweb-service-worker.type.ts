import { ServiceWorkerFetchEvent } from "./FetchEvent.ts";

export interface DwebWorkerEventMap {
  updatefound: Event; // 更新或重启的时候触发
  pause: Event; // 监听应用暂停
  resume: Event; // 监听应用恢复
  fetch: ServiceWorkerFetchEvent;
  // onFetch: FetchEvent;
}


export interface BFSMetaData {
  id: string;
  server: MainServer; // 打开应用地址
  name: string; // 应用名称
  short_name: string; // 应用副标题
  icon: string; // 应用图标
  images: string[]; // 应用截图
  description: string; // 应用描述
  author: string[]; // 开发者，作者
  version: string; // 应用版本
  categories: string[]; // 关键词
  home: string; // 首页地址
  size: string; // 应用大小
  fileHash: string;
  permissions: string[];
  plugins: string[];
  release_date: string; // 发布时间
}

interface MainServer {
  /**
   * 应用文件夹的目录
   */
  root: string;
  /**
   * 入口文件
   */
  entry: string;
}

export class IpcRequest {
  constructor(
    readonly req_id: string,
    readonly url: string,
    readonly method: IPC_METHOD,
    readonly headers: Headers,
    readonly body: $BodyData,
    readonly metaBody: {
      data: string;
      metaId: string;
      senderUid: number;
      type: number;
    }
  ) {}
}

export abstract class IpcBody {
  constructor(readonly _bodyHub: BodyHub) {}
}

export class BodyHub {
  constructor(readonly data: $BodyData) {}
}
export type $BodyData = Uint8Array | ReadableStream<Uint8Array> | string;

export const enum IPC_METHOD {
  GET = "GET",
  POST = "POST",
  PUT = "PUT",
  DELETE = "DELETE",
  OPTIONS = "OPTIONS",
  TRACE = "TRACE",
  PATCH = "PATCH",
  PURGE = "PURGE",
  HEAD = "HEAD",
}
