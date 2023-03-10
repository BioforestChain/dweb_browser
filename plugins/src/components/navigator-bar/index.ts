import { Navigatorbar } from "./navigator-bar.ts";

export { NavigationBarPluginEvents } from "./navigator.events.ts"


customElements.define('dweb-navigator', Navigatorbar);

// 插入自定义标签
document.addEventListener('DOMContentLoaded', documentOnDOMContentLoaded);
function documentOnDOMContentLoaded() {
  const el = new Navigatorbar();
  document.body.append(el);
  document.removeEventListener('DOMContentLoaded', documentOnDOMContentLoaded);
}

export * from './navigator.type.ts';

export {
  Navigatorbar
}
