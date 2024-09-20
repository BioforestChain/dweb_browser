import { Checkbox, prompt, Select } from "jsr:@cliffy/prompt@1.0.0-rc.5";
import { createBaseResolveTo } from "../../scripts/helper/resolveTo.ts";
import { getDevices } from "./utils/device.ts";
import { getLocalIp } from "./utils/localIp.ts";

if (import.meta.main) {
  let selectedDevice = "",
    addr = "";
  const promptResult = await prompt([
    {
      name: "platform",
      message: "请选择要测试的平台",
      type: Select,
      options: ["Android", "iOS"],
      after: async ({ platform }, next) => {
        if (platform) {
          const deviceList = await getDevices(platform);

          if (deviceList.length > 0) {
            selectedDevice = await Select.prompt({
              message: "请选择要测试的设备",
              options: deviceList,
            });
          }
        }
        await next();
      },
    },
    {
      name: "tags",
      message: "请选择想要测试的标签",
      type: Checkbox,
      options: ["deep_link_install", "plaoc_plugins"],
      after: async ({ tags }, next) => {
        if (tags && tags.includes("plaoc_plugins")) {
          addr = await getLocalIp();
        }
        await next()
      },
    },
  ]);

  const maestroArgs = [];
  const env: Record<string, string> = {};

  // 设置本地ip地址，用于plugins服务
  env["MAESTRO_LOCAL_IP"] = addr;

  if (promptResult.platform === "Android") {
    const username = Deno.env.get("USER") || Deno.env.get("USERNAME");

    env["MAESTRO_ANDROID_APP_ID"] = "info.bagen.dwebbrowser";
    if (!Deno.args.includes("release") && username) {
      env["MAESTRO_ANDROID_APP_ID"] += `.dweb.${username}`;
    }

    if (selectedDevice.length > 0) {
      maestroArgs.push("--device");
      maestroArgs.push(selectedDevice);
    }

    maestroArgs.push("test");

    if (promptResult.tags) {
      maestroArgs.push(`--include-tags=${promptResult.tags.join(",")}`);
    }

    maestroArgs.push(`--exclude-tags=launch`);
    maestroArgs.push(`android/`);
  } else if (promptResult.platform === "iOS") {
    env["MAESTRO_IOS_APP_ID"] = "com.instinct.bfexplorer.debug";

    if (selectedDevice.length > 0) {
      maestroArgs.push("--udid");
      maestroArgs.push(selectedDevice);
    }

    maestroArgs.push("test");

    if (promptResult.tags) {
      maestroArgs.push(`--include-tags=${promptResult.tags.join(",")}`);
    }

    maestroArgs.push(`--exclude-tags=launch`);
    maestroArgs.push(`ios/`);
  }

  const command = new Deno.Command("maestro", {
    args: maestroArgs,
    cwd: createBaseResolveTo(import.meta.url)(),
    env: env,
  });
  command.spawn();
}
