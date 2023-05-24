import { LitElement } from "lit";
export declare class MultiWebviewCompVirtualKeyboard extends LitElement {
    static styles: import("lit").CSSResult[];
    _elContainer: HTMLDivElement | undefined;
    _visible: boolean;
    _overlay: boolean;
    _navigation_bar_height: number;
    timer: unknown;
    requestId: number;
    insets: {
        left: number;
        top: number;
        right: number;
        bottom: number;
    };
    maxHeight: number;
    row1Keys: string[];
    row2Keys: string[];
    row3Keys: string[];
    row4Keys: string[];
    setHostStyle(): void;
    firstUpdated(): void;
    setCSSVar(): this;
    repeatGetKey(item: string): string;
    createElement(classname: string, key: string): HTMLDivElement;
    createElementForRow3(classNameSymbol: string, classNameAlphabet: string, key: string): HTMLDivElement;
    createElementForRow4(classNameSymbol: string, classNameSpace: string, classNameSearch: string, key: string): HTMLDivElement;
    transitionstart(): void;
    transitionend(): void;
    protected render(): unknown;
}
