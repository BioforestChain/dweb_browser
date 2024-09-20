export const getDevices = async (platform: string): Promise<string[]> => {
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
