import { JsProcessNMM } from "./sys/js-process.cjs";
import { DnsNMM } from "./sys/dns.cjs";
import { BootNMM } from "./sys/boot.cjs";
import { MultiWebviewNMM } from "./sys/multi-webview.mobile.cjs";

export const dns = new DnsNMM();
dns.install(new BootNMM());
dns.install(new MultiWebviewNMM());
dns.install(new JsProcessNMM());

import { desktopJmm } from "./user/desktop.js.cjs";
dns.install(desktopJmm);

Object.assign(globalThis, { dns: dns });

console.log("location.href", location.href);
