import { LitElement } from "lit";
import { Webview } from "./multi-webview.js";
import WebviewTag = Electron.WebviewTag;
export declare class MultiWebViewContent extends LitElement {
    static styles: import("lit").CSSResult[];
    customWebview: Webview | undefined;
    closing: Boolean;
    zIndex: Number;
    scale: Number;
    opacity: Number;
    customWebviewId: Number;
    src: String;
    preload: String;
    statusbarHidden: boolean;
    elWebview: WebviewTag | undefined;
    onDomReady(event: Event): void;
    webviewDidStartLoading(e: Event): void;
    onAnimationend(event: AnimationEvent): void;
    /**
     * 向内部的 webview 的内容执行 code
     * @param code
     */
    executeJavascript: (code: string) => void;
    onPluginNativeUiLoadBase(e: Event): void;
    getWebviewTag(): WebviewTag | undefined;
    render(): import("lit").TemplateResult<1>;
}
export interface CustomEventDomReadyDetail {
    customWebview: Webview;
    event: Event;
    from: EventTarget & WebviewTag;
}
export interface CustomEventAnimationendDetail {
    customWebview: Webview;
    event: AnimationEvent;
    from: EventTarget | null;
}
