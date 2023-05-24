import type { MessageEvent as MessageEventMain, MessagePortMain } from "electron";
export declare const postMainMesasgeToRenderMessage: (from_port: MessagePortMain, to_port: MessagePort, event: MessageEventMain) => void;
export declare const postRenderMessageToMainMesasge: (from_port: MessagePort, to_port: MessagePortMain, event: MessageEvent) => void;
export declare const MainPortToRenderPort: (main_port: MessagePortMain) => MessagePort;
export declare const RenderPortToMainPort: (render_port: MessagePort) => Electron.MessagePortMain;
