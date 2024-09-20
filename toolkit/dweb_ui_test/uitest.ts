import { createBaseResolveTo } from "../../scripts/helper/resolveTo.ts";

const resolveTo = createBaseResolveTo(import.meta.url);

if (import.meta.main) {
  if (Deno.args.includes("--android")) {
    let device = "";

    const env: Record<string, string> = {};
    const username = Deno.env.get("USER") || Deno.env.get("USERNAME");

    env["MAESTRO_ANDROID_APP_ID"] = "info.bagen.dwebbrowser";
    if (!Deno.args.includes("release") && username) {
      env["MAESTRO_ANDROID_APP_ID"] += `.dweb.${username}`;
    }

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
      const deviceIds = lines.filter((line) => line.endsWith("device")).map((line) => line.split("\t")[0]);

      if (deviceIds.length > 0) {
        device = deviceIds[0];
      }
    }

    if (device.length > 0) {
      const command = new Deno.Command("maestro", {
        args: ["--device", device, "test", "--include-tags=deep_link_install", "android/"],
        cwd: resolveTo(),
        env: env,
      });
      command.spawn();
    } else {
      const command = new Deno.Command("maestro", {
        args: ["test", "--include-tags=deep_link_install", "android/"],
        cwd: resolveTo(),
        env,
      });
      command.spawn();
    }
  } else {
    // xcodebuild -scheme "DwebBrowser" -configuration debug -sdk iphonesimulator -destination 'platform=iOS Simulator,name=iPhone 15 Pro Max,OS=latest' build run
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

    const shutdownDevices = iphoneDevices
      .filter((line) => line.toLowerCase().includes("shutdown"))
      .map((line) => {
        const match = line.match(/\(([\w-]+)\)/);
        return match ? { udid: match[1], status: "shutdown" } : null;
      })
      .filter(Boolean);

    const env: Record<string, string> = {
      MAESTRO_IOS_APP_ID: "com.instinct.bfexplorer.debug",
    };

    if (bootedDevices.length > 0 && bootedDevices[0]?.udid) {
      const command = new Deno.Command("maestro", {
        args: ["--udid", bootedDevices[0].udid, "test", "--include-tags=deep_link_install", "ios/"],
        cwd: resolveTo(),
        env: env,
      });
      command.spawn();
    } else {
      const command = new Deno.Command("maestro", {
        args: ["test", "--include-tags=deep_link_install", "ios/"],
        cwd: resolveTo(),
        env: env,
      });
      command.spawn();
    }
  }
}
