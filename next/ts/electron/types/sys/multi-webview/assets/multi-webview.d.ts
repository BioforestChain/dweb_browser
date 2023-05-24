import WebviewTag = Electron.WebviewTag;
declare class Webview {
    readonly id: number;
    readonly src: string;
    constructor(id: number, src: string);
    webContentId: number;
    webContentId_devTools: number;
    private _api;
    get api(): WebviewTag;
    doReady(value: WebviewTag): void;
    private _api_po;
    ready(): Promise<WebviewTag>;
    closing: boolean;
    state: {
        zIndex: number;
        openingIndex: number;
        closingIndex: number;
        scale: number;
        opacity: number;
    };
}
export { Webview };
