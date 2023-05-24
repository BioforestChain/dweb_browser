import type { MessagePortMain } from "electron";
export declare const updateMainMessageListener: (target: MessagePortMain, method: keyof MessagePortMain, listener_index: number) => Electron.MessagePortMain;
export declare const updateMainPostMessage: (target: MessagePortMain) => Electron.MessagePortMain;
