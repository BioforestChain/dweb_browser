import { LitElement } from "lit";
export declare class MultiWebviewCompToast extends LitElement {
    static styles: import("lit").CSSResult[];
    static properties: {
        _beforeEntry: {
            state: boolean;
        };
    };
    _message: string;
    _duration: string;
    _position: string;
    _beforeEntry: boolean;
    firstUpdated(): void;
    transitionend(e: TransitionEvent): void;
    protected render(): import("lit").TemplateResult<1>;
}
