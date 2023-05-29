// TODO 根据开发模式来取消这一行代码
import "source-map-support/register";

import { protocol } from "electron";

try {
  protocol.registerSchemesAsPrivileged([
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
