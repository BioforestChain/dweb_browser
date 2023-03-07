import { BarcodeScanner } from "./barcodeScanner.plugin.ts"


export {
  BarcodeScanner
}

customElements.define('dweb-scanner', BarcodeScanner);

// 插入自定义标签
document.addEventListener('DOMContentLoaded', documentOnDOMContentLoaded);
function documentOnDOMContentLoaded() {
  const el = new BarcodeScanner();
  document.body.append(el);
  document.removeEventListener('DOMContentLoaded', documentOnDOMContentLoaded);
}

