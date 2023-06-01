import { BasePlugin } from "../base/BasePlugin.ts";

export class ConfigPlugin extends BasePlugin {
  readonly tagName = "dweb-config";
  constructor() {
    super("internal");
  }
  getInternalUrl() {
    return BasePlugin.internal_url;
  }
  setInternalUrl(url: string) {
    try {
      return (BasePlugin.internal_url = url);
    } finally {
      this.init_public_url();
    }
  }
  private _first = false;
  private init_public_url() {
    if (this._first) {
      return;
    }
    this._first = true;
    void this.updatePublicUrl();
  }

  private async _getPublicUrl() {
    try {
      const public_url = await this.fetchApi("/public-url", {
        base: BasePlugin.internal_url,
      }).text();
      BasePlugin.internal_url_useable = true;
      return public_url;
    } catch (_err) {
      BasePlugin.internal_url_useable = false;
      return "";
    }
  }
  updatePublicUrl() {
    let get_public_url = this._getPublicUrl();
    if (BasePlugin.public_url === "") {
      get_public_url = get_public_url.then((new_public_url) => {
        if (new_public_url !== "" || BasePlugin.public_url === get_public_url) {
          BasePlugin.public_url = new_public_url;
        }
        return new_public_url;
      });
      BasePlugin.public_url = get_public_url;
    } else {
      get_public_url.then((new_public_url) => {
        if (new_public_url !== "") {
          BasePlugin.public_url = new_public_url;
        }
      });
    }
    return get_public_url;
  }
  setPublicUrl(url: string) {
    return (BasePlugin.public_url = url);
  }
  get public_url() {
    return BasePlugin.public_url;
  }
}
export const configPlugin = new ConfigPlugin();
// const _ = ConfigPlugin satisfies $BasePlugin;
