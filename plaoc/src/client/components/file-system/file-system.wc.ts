import { fileSystemPlugin } from "./file-system.plugin.ts";

export class HTMLDwebFileSystemElement extends HTMLElement {
  static readonly tagName = "dweb-file-system";
  readonly plugin = fileSystemPlugin;

  get writeFile() {
    return fileSystemPlugin.writeFile;
  }
  get getUri() {
    return fileSystemPlugin.getUri;
  }
}

customElements.define(HTMLDwebFileSystemElement.tagName, HTMLDwebFileSystemElement);
declare global {
  interface HTMLElementTagNameMap {
    [HTMLDwebFileSystemElement.tagName]: HTMLDwebFileSystemElement;
  }
}
