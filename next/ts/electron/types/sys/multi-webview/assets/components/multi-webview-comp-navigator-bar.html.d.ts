import { LitElement, PropertyValueMap } from "lit";
export declare class MultiWebviewCompNavigationBar extends LitElement {
    static styles: import("lit").CSSResult[];
    _color: string;
    _style: string;
    _overlay: boolean;
    _visible: boolean;
    _insets: {
        top: number;
        right: number;
        bottom: number;
        left: number;
    };
    _webview_src: any;
    protected updated(_changedProperties: PropertyValueMap<any> | Map<PropertyKey, unknown>): void;
    createBackgroundStyleMap(): {
        backgroundColor: string;
    };
    createContainerStyleMap(): {
        color: string;
    };
    setHostStyle(): void;
    back(): void;
    home(): void;
    menu(): void;
    render(): import("lit").TemplateResult<1>;
}
