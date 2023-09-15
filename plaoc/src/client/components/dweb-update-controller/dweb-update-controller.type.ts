export interface UpdateControllerMap {
  start: Event; // 监听启动
  progress: string; // 进度每秒触发一次
  end: Event; // 结束
  cancel: Event; // 取消
}
