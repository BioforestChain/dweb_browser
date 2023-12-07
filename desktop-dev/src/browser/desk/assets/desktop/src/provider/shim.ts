import { CloseWatcher } from "../../../../../../../../plaoc/src/client/components/close-watcher/close-watcher.shim.ts";
export { CloseWatcher };
export type $CloseWatcher = InstanceType<typeof CloseWatcher>;

let _is_native_support_context_menu_ = false;
document.body.addEventListener("contextmenu", function test() {
  _is_native_support_context_menu_ = true;
  document.body.removeEventListener("contextmenu", test);
});
export function dispatchContextMenuEvent(e: PointerEvent) {
  if (_is_native_support_context_menu_) return;
  e.target?.dispatchEvent(new PointerEvent("contextmenu", e));
}
