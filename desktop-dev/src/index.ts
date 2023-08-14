import "./helper/electron.ts"; /// 全局导入
import "./polyfill.ts"; /// 垫片导入

try {
  Electron.protocol.registerSchemesAsPrivileged([
    {
      scheme: "http",
      privileges: {
        bypassCSP: true,
        standard: true,
        secure: true,
        stream: true,
      },
    },
    {
      scheme: "https",
      privileges: {
        bypassCSP: true,
        standard: true,
        secure: true,
        stream: true,
      },
    },
  ]);
} catch (err) {
  console.log("err: ", err);
}

import { dns } from "./main.ts";
dns.bootstrap();
