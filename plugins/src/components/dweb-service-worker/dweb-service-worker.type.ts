import { FetchEvent, OnFetchEvent } from './FetchEvent.ts';
export interface DwebWorkerEventMap {
  updatefound: Event, // 更新或重启的时候触发
  fetch: FetchEvent,
  onFetch: OnFetchEvent
}

export interface UpdateControllerMap {
  start: Event, // 监听启动
  progress: string, // 进度每秒触发一次
  end: Event, // 结束
  cancel: Event, // 取消
}



export interface PluginListenerHandle {
  remove: () => Promise<void>;
}

export interface WindowListenerHandle {
  registered: boolean;
  windowEventName: string;
  pluginEventName: string;
  // deno-lint-ignore no-explicit-any
  handler: (event: any) => void;
}

// deno-lint-ignore no-explicit-any
export type ListenerCallback<K> = (args: K) => any
