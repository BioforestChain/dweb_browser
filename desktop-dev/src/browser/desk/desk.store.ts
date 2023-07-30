import { $MMID } from "../../core/types.ts";
import { Store } from "../../helper/electronStore.ts";
export class DeskStore extends Store<{
  "taskbar/apps": Set<$MMID>;
  "desktop/orders": Map<$MMID, { order: number }>;
}> {
  constructor() {
    super("desk.browser.dweb");
  }
}

export const deskStore = new DeskStore();
