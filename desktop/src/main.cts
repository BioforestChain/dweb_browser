import { BootNMM } from "./sys/boot.cjs";
import { DnsNMM } from "./sys/dns.cjs";
import { HttpServerNMM } from "./sys/http-server/http-server.cjs";
import { JsProcessNMM } from "./sys/js-process/js-process.cjs";
import { MultiWebviewNMM } from "./sys/multi-webview.mobile.cjs";

export const dns = new DnsNMM();
dns.install(new BootNMM());
dns.install(new MultiWebviewNMM());
dns.install(new JsProcessNMM());
dns.install(new HttpServerNMM());

import { desktopJmm } from "./user/desktop/desktop.main.cjs";
dns.install(desktopJmm);

Object.assign(globalThis, { dns: dns });

console.log("location.href", location.href);
