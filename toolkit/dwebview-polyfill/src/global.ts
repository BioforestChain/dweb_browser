declare global {
  interface IosWebkit {
    messageHandlers?: Record<string, { postMessage: (data: unknown) => void }> &
      Record<"websocket", { postMessage(data: unknown): Promise<unknown> }>;
  }
  const webkit: undefined | IosWebkit;
  interface Window {
    webkit?: IosWebkit;
  }
}
