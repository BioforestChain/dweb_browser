import { X_PLAOC_QUERY } from "../../../server/const.ts";
import { BasePlugin } from "../base/BasePlugin.ts";

export class ConfigPlugin extends BasePlugin {
  constructor() {
    super("internal.dweb");
    if (typeof location === "object") {
      this.initConfig();
    }
  }
  async initConfig() {
    const internalUrl = await BasePlugin.getInternalUrl(X_PLAOC_QUERY.API_INTERNAL_URL)
    internalUrl && this.setInternalUrl(internalUrl);
  }

  getInternalUrl() {
    return BasePlugin.internal_url;
  }
  setInternalUrl(url: string) {
    try {
      return (BasePlugin.internal_url = url);
    } finally {
      // this.init_public_url();
    }
  }
  get public_url() {
    return BasePlugin.public_url;
  }
}
export const configPlugin = new ConfigPlugin();
