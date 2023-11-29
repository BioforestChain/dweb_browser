import { CloseWatcher } from "../../../../../../../../plaoc/src/client/components/close-watcher/close-watcher.shim.ts";
export { CloseWatcher };
export type $CloseWatcher = InstanceType<typeof CloseWatcher>;

export function dispatchContextMenuEvent(e: PointerEvent) {
  e.target?.dispatchEvent(new PointerEvent("contextmenu", e));
}
