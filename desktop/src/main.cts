import { BootNMM } from "./sys/boot.cjs";
import { DnsNMM } from "./sys/dns/dns.cjs";
import { HttpServerNMM } from "./sys/http-server/http-server.cjs";
import { JsProcessNMM } from "./sys/js-process/js-process.cjs";
import { MultiWebviewNMM } from "./sys/multi-webview/multi-webview.mobile.cjs";

export const dns = new DnsNMM();
dns.install(new BootNMM());
dns.install(new MultiWebviewNMM());
dns.install(new JsProcessNMM());
dns.install(new HttpServerNMM());

import { desktopJmm } from "./user/desktop/desktop.main.cjs";
dns.install(desktopJmm);

// 安装 browser 
import { browserJMM } from "./user/browser/browser.main.cjs"
dns.install(browserJMM)


Object.assign(globalThis, { dns: dns });
