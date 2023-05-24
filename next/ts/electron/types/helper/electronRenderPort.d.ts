export declare const updateRenderPort: (port: MessagePort) => MessagePort;
export declare const updateRenderMessageListener: (target: MessagePort, method: keyof MessagePort, listener_index: number) => MessagePort;
export declare const resolveRenderMessageListener: <T extends EventListenerOrEventListenerObject | ((event: MessageEvent) => any)>(listener: T) => T;
export declare const updateRenderPostMessage: (target: MessagePort) => MessagePort;
