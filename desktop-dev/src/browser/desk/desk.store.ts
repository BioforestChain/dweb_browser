import { $MMID } from "../../core/types.ts";
import { Store } from "../../helper/electronStore.ts";

export const deskStore = new Store<{
  "taskbar/apps": Set<$MMID>;
}>("desk.browser.dweb");
