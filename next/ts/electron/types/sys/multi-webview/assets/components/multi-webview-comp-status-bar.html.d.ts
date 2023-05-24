import { LitElement, PropertyValueMap } from "lit";
export declare class MultiWebviewCompStatusBar extends LitElement {
    _color: string;
    _style: string;
    _overlay: boolean;
    _visible: boolean;
    _height: string;
    _insets: {
        top: string;
        right: number;
        bottom: number;
        left: number;
    };
    _torchIsOpen: boolean;
    _webview_src: any;
    static styles: import("lit").CSSResult[];
    createBackgroundStyleMap(): {
        backgroundColor: string;
    };
    createContainerStyleMap(): {
        color: string;
    };
    protected updated(_changedProperties: PropertyValueMap<any> | Map<PropertyKey, unknown>): void;
    setHostStyle(): void;
    protected render(): unknown;
}
