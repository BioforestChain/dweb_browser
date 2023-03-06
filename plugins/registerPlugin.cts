import type { BasePlugin } from "./basePlugin.js";

const hiJackCapacitorPlugin = (window as any).Capacitor.Plugins

export const registerWebPlugin = (plugin: BasePlugin): void => {
  new Proxy(hiJackCapacitorPlugin, {
    get(target, key) {
      if (key === plugin.proxy) {
        return plugin
      }
    }
  })

}

