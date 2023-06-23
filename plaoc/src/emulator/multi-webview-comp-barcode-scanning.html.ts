import { css, LitElement, PropertyValueMap } from "lit";
import { customElement, query } from "lit/decorators.js";
import { html } from "lit/static-html.js";

const TAG = "multi-webview-comp-barcode-scanning";

@customElement(TAG)
export class MultiWebviewCompBarcodeScanning extends LitElement {
  static override styles = createAllCSS();
  @query("input") elInput: HTMLInputElement | undefined | null;

  protected override firstUpdated(
    _changedProperties: PropertyValueMap<any> | Map<PropertyKey, unknown>
  ): void {
    if (this.elInput === undefined || this.elInput === null)
      throw new Error(`this.elInput === undefined || this.elInput === null`);
    this.elInput.click();
  }

  onChange(e: Event) {
    const elInput = e.target as HTMLInputElement;
    console.log("onChange");
    // const file = elInput.files?[0];
  }

  protected override render() {
    return html` <input type="file" @change=${this.onChange} /> `;
  }
}

function createAllCSS() {
  return [
    css`
      :host {
        position: fixed;
        left: 0px;
        top: 0px;
        width: 0px;
        height: 0px;
        overflow: hidden;
      }
    `,
  ];
}

declare global {
  interface HTMLElementTagNameMap {
    [TAG]: MultiWebviewCompBarcodeScanning;
  }
}
