import { BasePlugin } from "../basePlugin.ts";

export class ConfigPlugin extends BasePlugin {
  tagName = "dweb-config";
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
    void this.getPublicUrl();
  }

  private _getPublicUrl() {
    return this.fetchApi("/public-url").text();
  }
  getPublicUrl() {
    return (BasePlugin.public_url = this._getPublicUrl());
  }
  get public_url() {
    return BasePlugin.public_url;
  }
}
export const configPlugin = new ConfigPlugin();
// const _ = ConfigPlugin satisfies $BasePlugin;
