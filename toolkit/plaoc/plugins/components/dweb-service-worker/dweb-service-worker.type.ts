import type { $IpcRequestInit } from "@dweb-browser/core/ipc/index.ts";
import type { ServiceWorkerFetchEvent } from "./FetchEvent.ts";
import type { PlaocEvent } from "./IpcEvent.ts";

export interface DwebWorkerEventMap {
  pause: Event; // 监听应用暂停
  resume: Event; // 监听应用恢复
  fetch: ServiceWorkerFetchEvent;
  shortcut: PlaocEvent;
}

export enum eventHandle {
  shortcut = "shortcut",
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

export type $DwebRquestInit = {
  search?:
    | ConstructorParameters<typeof URLSearchParams>[0]
    // deno-lint-ignore no-explicit-any
    | Record<string, any>;
  /**是否需要激活对方 */
  activate?: boolean;
} & $IpcRequestInit;
