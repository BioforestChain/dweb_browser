import { SplashScreenPlugin } from "./splash.plugin.ts";


customElements.define('dweb-splash', SplashScreenPlugin)

// 插入自定义标签
document.addEventListener('DOMContentLoaded', documentOnDOMContentLoaded);
function documentOnDOMContentLoaded() {
  const el = new SplashScreenPlugin();
  document.body.append(el);
  document.removeEventListener('DOMContentLoaded', documentOnDOMContentLoaded);
}

export * from "./splash.type.ts"

export {
  SplashScreenPlugin
}
