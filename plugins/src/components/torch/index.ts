import { TorchPlugin } from "./torch.plugin.ts";



customElements.define('dweb-torch', TorchPlugin)

// 插入自定义标签
document.addEventListener('DOMContentLoaded', documentOnDOMContentLoaded);
function documentOnDOMContentLoaded() {
  const el = new TorchPlugin();
  document.body.append(el);
  document.removeEventListener('DOMContentLoaded', documentOnDOMContentLoaded);
}

export {
  TorchPlugin
}
