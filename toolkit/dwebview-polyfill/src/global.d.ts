declare const webkit: {
  messageHandlers: Record<"websocket", { postMessage(data: unknown): Promise<unknown> }> &
    Record<"favicons", { postMessage(data: unknown): void }>;
};
