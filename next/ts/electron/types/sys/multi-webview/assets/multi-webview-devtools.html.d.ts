import { LitElement } from "lit";
import { Webview } from "./multi-webview.js";
export declare class MultiWebviewDevtools extends LitElement {
    customWebview: Webview | undefined;
    closing: boolean;
    zIndex: Number;
    scale: Number;
    opacity: Number;
    customWebviewId: Number;
    static styles: import("lit").CSSResult[];
    onDomReady(event: Event): void;
    onDestroy(): void;
    render(): import("lit").TemplateResult<1>;
}
