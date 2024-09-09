import { createBaseResolveTo } from "../../scripts/helper/resolveTo.ts";

const resolveTo = createBaseResolveTo(import.meta.url);

if (import.meta.main) {
  let device = "";

  const env: Record<string, string> = {}
  const username = Deno.env.get("USER") || Deno.env.get("USERNAME");

  env["MAESTRO_ANDROID_APP_ID"] = "info.bagen.dwebbrowser"
  if(username) {
    env["MAESTRO_ANDROID_APP_ID"] += `.dweb.${username}`
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
      args: ["--device", device, "test", "android/"],
      cwd: resolveTo(),
      env: env
    });
    command.spawn();
  } else {
    const command = new Deno.Command("maestro", {
      args: ["test", "android/"],
      cwd: resolveTo(),
      env
    });
    command.spawn();
  }
}
