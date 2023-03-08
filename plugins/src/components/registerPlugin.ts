import type { BasePlugin } from "./basePlugin.ts";

// deno-lint-ignore no-explicit-any
const hiJackCapacitorPlugin = (window as any).Capacitor?.Plugins

export const registerWebPlugin = (plugin: BasePlugin): void => {
  if (hiJackCapacitorPlugin) {
    new Proxy(hiJackCapacitorPlugin, {
      get(_target, key) {
        if (key === plugin.proxy) {
          return plugin
        }
      }
    })
  }
}

