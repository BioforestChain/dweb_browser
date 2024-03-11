import CloseWatcher from "../../../../../../../../toolkit/dwebview-polyfill/src/close-watcher/close-watcher.shim.ts";
export { CloseWatcher };
export type $CloseWatcher = InstanceType<typeof CloseWatcher>;

const mock_symbole = Symbol("moke-context-menu");
/// 原生的 contextmenu 和 dispatchContextMenuEvent 可能会同时执行，所以第一次需要有一个延迟检查机制，避免过早读取到 support 值
let _check_delay_ = 100;
let _is_native_support_context_menu_ = false;
document.body.addEventListener("contextmenu", function test(e) {
  _is_native_support_context_menu_ = !(mock_symbole in e);
  document.body.removeEventListener("contextmenu", test);
});
export async function dispatchContextMenuEvent(e: PointerEvent) {
  if (_check_delay_ > 0) {
    await new Promise((cb) => setTimeout(cb, _check_delay_));
    _check_delay_ = 0;
  }
  if (_is_native_support_context_menu_) return;
  e.target?.dispatchEvent(Object.assign(new PointerEvent("contextmenu", e), { [mock_symbole]: true }));
}
