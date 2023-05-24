import { LitElement, PropertyValueMap } from "lit";
export declare class MultiWebviewCompBarcodeScanning extends LitElement {
    static styles: import("lit").CSSResult[];
    elInput: HTMLInputElement | undefined | null;
    protected firstUpdated(_changedProperties: PropertyValueMap<any> | Map<PropertyKey, unknown>): void;
    onChange(e: Event): void;
    protected render(): import("lit").TemplateResult;
}
