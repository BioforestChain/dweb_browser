import fs from "node:fs";
import os from "node:os";
import node_path from "node:path";
import { rootResolve } from "../../scripts/helper/resolver.ts";

let dwebBrowserDataDir: string;
switch (os.platform()) {
  case "win32":
    dwebBrowserDataDir = node_path.resolve(os.userInfo().homedir, "AppData/Roaming/Local/dweb-browser/");
    break;
  case "darwin":
    dwebBrowserDataDir = node_path.resolve(os.userInfo().homedir, "Library/Application Support/dweb-browser");
    break;
  default:
    throw new Error("no support current os yet.");
}

const jmmAppDirname = node_path.resolve(dwebBrowserDataDir, "data/jmm.browser.dweb/apps");

for (const appId of fs.readdirSync(jmmAppDirname)) {
  const target = node_path.resolve(jmmAppDirname, appId, "usr/server");
  if (fs.existsSync(target)) {
    if (fs.statSync(target).isSymbolicLink()) {
      fs.unlinkSync(target);
    } else {
      fs.rmdirSync(target, { recursive: true });
    }
    fs.symlinkSync(rootResolve("./npm/@plaoc__server/dist/dev"), target, "junction");
    console.log("hacked", target);
  }
}
//  `/Users/kzf/Library/Application Support/dweb-browser/data/jmm.browser.dweb/apps/`
