/** 将 request 参数解构 成 ipcRequest 的参数 */
export declare const $readRequestAsIpcRequest: (request_init: RequestInit) => Promise<{
    method: string;
    body: "" | Uint8Array | ReadableStream<Uint8Array>;
    headers: Record<string, string>;
}>;
