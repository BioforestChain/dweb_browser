import { LitElement, PropertyValueMap } from "lit";
export declare class MultiWebviewCompShare extends LitElement {
    static styles: import("lit").CSSResult[];
    _title: string;
    _text: string;
    _link: string;
    _src: string;
    protected firstUpdated(_changedProperties: PropertyValueMap<any> | Map<PropertyKey, unknown>): void;
    cancel(): void;
    render(): import("lit").TemplateResult<1>;
}
