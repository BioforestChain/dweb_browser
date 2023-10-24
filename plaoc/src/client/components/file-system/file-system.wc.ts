import { cacheGetter } from "../../helper/cacheGetter.ts";
import { fileSystemPlugin } from "./file-system.plugin.ts";

export class HTMLDwebFileSystemElement extends HTMLElement {
  static readonly tagName = "dweb-file-system";
  readonly plugin = fileSystemPlugin;

  @cacheGetter()
  get writeFile() {
    return this.plugin.writeFile;
  }

  @cacheGetter()
  get getUri() {
    return this.plugin.getUri;
  }

  @cacheGetter()
  get savePictures() {
    return this.plugin.savePictures
  }

}

if (!customElements.get(HTMLDwebFileSystemElement.tagName)) {
customElements.define(HTMLDwebFileSystemElement.tagName, HTMLDwebFileSystemElement);
}
declare global {
  interface HTMLElementTagNameMap {
    [HTMLDwebFileSystemElement.tagName]: HTMLDwebFileSystemElement;
  }
}
