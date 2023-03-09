import type { BasePlugin } from "./basePlugin.ts";

// deno-lint-ignore no-explicit-any
// const hiJackCapacitorPlugin = (window as any).Capacitor.Plugins

// export const registerWebPlugin = (plugin: BasePlugin): void => {
//   new Proxy(hiJackCapacitorPlugin, {
//     get(_target, key) {
//       if (key === plugin.proxy) {
//         return plugin
//       }
//     }
//   })
// }

/**
 * 声明一个 Plugins 类型用来保存全部的插件
 */
class Plugins{
  map = new Map()
  registerWebPlugin = (plugin: BasePlugin) => {
    this.map.set(plugin.proxy, plugin);
  }
}

/**
 * 实例化
 */
const plugins = new Plugins();

/**
 * 初始化定义 window.Capacitor 属性
 */
(window as any).Capacitor ? "" : (window as any).Capacitor = { Plugins: {}};

/**
 * 把 window.Capacitor.Plugins 代理到 Plugins 实例上
 */
(window as any).Capacitor.Plugins = new Proxy({}, {
  get(_target, proxy, receiver){
    return plugins.map.get(proxy);
  }
})

/**
 * 暴露注册插件的方法
 */
export const registerWebPlugin = plugins.registerWebPlugin

