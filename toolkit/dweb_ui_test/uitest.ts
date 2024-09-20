import { Checkbox, prompt, Select } from "jsr:@cliffy/prompt@1.0.0-rc.5";
import { createBaseResolveTo } from "../../scripts/helper/resolveTo.ts";

const resolveTo = createBaseResolveTo(import.meta.url);

const getDevices = async (platform: string): Promise<string[]> => {
  let deviceList: string[] = [];
  if (platform === "Android") {
    const devices = new Deno.Command("adb", {
      args: ["devices"],
      stdout: "piped",
      stderr: "piped",
    });

    const output = await devices.output();

    if (output.success) {
      const outputText = new TextDecoder().decode(output.stdout);
      const lines = outputText.split("\n").map((line) => line.trim());

      // 过滤出设备 ID 列表
      deviceList = lines.filter((line) => line.endsWith("device")).map((line) => line.split("\t")[0]);
    }
  } else if (platform === "iOS") {
    const cmd = new Deno.Command("xcrun", {
      args: ["simctl", "list", "devices"],
      stdout: "piped",
      stderr: "piped",
    });

    const { stdout } = await cmd.output();
    const output = new TextDecoder().decode(stdout);

    // 过滤出所有 iPhone 设备
    const iphoneDevices = output.split("\n").filter((line) => line.toLowerCase().includes("iphone"));

    // 区分 booted 和 shutdown 设备
    const bootedDevices = iphoneDevices
      .filter((line) => line.toLowerCase().includes("booted"))
      .map((line) => {
        const match = line.match(/\(([\w-]+)\)/);
        return match ? { udid: match[1], status: "booted" } : null;
      })
      .filter(Boolean);

    if (bootedDevices.length > 0) {
      deviceList = bootedDevices.map((value) => value?.udid).filter((value) => value !== undefined);
    }
  }

  return deviceList;
};

if (import.meta.main) {
  let selectedDevice = "";
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
    },
  ]);

  const maestroArgs = [];
  const env: Record<string, string> = {};

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
    cwd: resolveTo(),
    env: env,
  });
  command.spawn();
}
