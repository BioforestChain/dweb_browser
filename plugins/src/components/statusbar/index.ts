import { StatusbarPlugin } from "./statusbar.plugin.ts";


customElements.define('dweb-statusbar', StatusbarPlugin)

// 插入自定义标签
document.addEventListener('DOMContentLoaded', documentOnDOMContentLoaded);
function documentOnDOMContentLoaded() {
  const el = new StatusbarPlugin();
  document.body.append(el);
  document.removeEventListener('DOMContentLoaded', documentOnDOMContentLoaded);
}


export {
  StatusbarPlugin
}
