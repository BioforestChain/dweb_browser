import { BasePlugin } from "../basePlugin.ts";

export class SafeAreaPlugin extends BasePlugin {
  readonly tagName = "dweb-safe-area";
  constructor() {
    super("safe-area.nativeui.dweb.sys");
  }
}

export const safeAreaPlugin = new SafeAreaPlugin();
