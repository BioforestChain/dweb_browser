import type { BasePlugin } from "./components/basePlugin.js";

const hiJackCapacitorPlugin = (window as any).Capacitor.Plugin

export const registerWebPlugin = (plugin: BasePlugin): void => {
  new Proxy((window as any).Capacitor.Plugins, {
    get() {

    }
  })

}

