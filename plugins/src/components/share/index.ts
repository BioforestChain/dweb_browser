import { SharePlugin } from "./share.plugin.ts";


customElements.define('dweb-share', SharePlugin);

// 插入自定义标签
document.addEventListener('DOMContentLoaded', documentOnDOMContentLoaded);
function documentOnDOMContentLoaded() {
  const el = new SharePlugin();
  document.body.append(el);
  document.removeEventListener('DOMContentLoaded', documentOnDOMContentLoaded);
}

export * from "./share.type.ts"

export {
  SharePlugin
}

