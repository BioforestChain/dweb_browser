import translate from "npm:translate";

Deno.test("测试翻译效果", async () => {
  const text = await translate("接天莲叶无穷碧，映日荷花别样红。", { from: "zh", to: "en" });
  console.log(text);
});

Deno.test("测试cli", async () => {
  await testCommand(["run", "-A", "../index.ts", "./manifest.test.json", "--from", "zh", "--to", "en"]);
});

Deno.test("测试读取配置文件", async () => {
  await testCommand(["run", "-A", "../index.ts", "--config", "./dwebtranslaterc.json"]);
});

Deno.test("测试读取默认配置文件", async () => {
  await testCommand(["run", "-A", "../index.ts"]);
});

const testCommand = async (args: string[]) => {
  // 获取当前脚本所在目录
  const cwd = new URL(".", import.meta.url).pathname;
  // 创建并运行 CLI 命令
  const task = new Deno.Command("deno", {
    cwd, // 设置工作目录为当前脚本所在目录
    args: args,
    stdin: "inherit", // 继承标准输入
    stdout: "piped", // 使用 piped 以捕获标准输出
    stderr: "piped", // 使用 piped 以捕获错误输出
  });

  // 执行命令并等待结果
  const { code, stdout, stderr } = await task.output();

  const output = new TextDecoder().decode(stdout);
  const errorOutput = new TextDecoder().decode(stderr);

  output && console.log("输出:", output);
  errorOutput && console.error("错误输出:", errorOutput);

  if (code !== 0) {
    throw new Error(`CLI 命令执行失败，退出码: ${code}`);
  }
};
