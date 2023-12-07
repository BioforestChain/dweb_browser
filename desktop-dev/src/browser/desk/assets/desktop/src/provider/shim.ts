import { CloseWatcher } from "../../../../../../../../plaoc/src/client/components/close-watcher/close-watcher.shim.ts";
export { CloseWatcher };
export type $CloseWatcher = InstanceType<typeof CloseWatcher>;

const mock_symbole = Symbol("moke-context-menu");
let _is_native_support_context_menu_ = false;
document.body.addEventListener("contextmenu", function test(e) {
  _is_native_support_context_menu_ = !(mock_symbole in e);
  document.body.removeEventListener("contextmenu", test);
});
export function dispatchContextMenuEvent(e: PointerEvent) {
  if (_is_native_support_context_menu_) return;
  e.target?.dispatchEvent(Object.assign(new PointerEvent("contextmenu", e), { [mock_symbole]: true }));
}
