import { ToastPlugin } from "./toast.plugin.ts";



customElements.define('dweb-toast', ToastPlugin)

// 插入自定义标签
document.addEventListener('DOMContentLoaded', documentOnDOMContentLoaded);
function documentOnDOMContentLoaded() {
  const el = new ToastPlugin();
  document.body.append(el);
  document.removeEventListener('DOMContentLoaded', documentOnDOMContentLoaded);
}


export {
  ToastPlugin
}
