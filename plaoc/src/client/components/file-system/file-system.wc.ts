import { fileSystemPlugin } from "./file-system.plugin.ts";

export class HTMLDwebFileSystemElement extends HTMLElement {
  readonly plugin = fileSystemPlugin;

  get writeFile() {
    return fileSystemPlugin.writeFile;
  }
  get getUri() {
    return fileSystemPlugin.getUri;
  }
}

customElements.define(fileSystemPlugin.tagName, HTMLDwebFileSystemElement);
