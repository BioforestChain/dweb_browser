import { Store } from "../../helper/electronStore.ts";
import { $MMID } from "../js-process/std-dweb-core.ts";

export const desktopStore = new Store<{
  "taskbar/apps": Set<$MMID>;
}>("desktop.browser.dweb");
