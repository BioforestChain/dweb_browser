import { Command, EnumType, Input, Toggle, logColors, path } from "../../deps.ts";
import npmConfig from "../../scripts/npm.json" assert { type: "json" };
import { bundle } from "../cmd/bundle.ts";
const program = new Command();

enum AppType {
  Static = "static", // 纯静态App
  Dynamic = "dynamic" // 动态App
}

const appType = new EnumType(AppType);

// 命令行模式 打包application为.jmm到指定位置
program
  .description(npmConfig.description)
  .version(npmConfig.version)
  // 无界面应用必须包含后端
  // .requiredOption("-b, --back-path <string>", "backend application path.")
  .option("-d, --dest-path <string>", "Front-end application path.", {
    required:true,
  })
  .action(function ({ destPath }) {
    if (!destPath) {
      this.showHelp();
      return;
    }
  })
  .option(
    "-b, --front-build-path <string>",
    "frontBuildPath: Packaged front-end source code address.",
    {
      required:true
    }
  )
  .type("appType", appType)
  .option("-t, --app-type [appType:appType]", `App types are [${logColors.bgBlue("dynamic")}] and [${logColors.bgBlue("static")}].`)
  .option("-i, --interact [interact:boolean]", "Whether to enable interactive.",{
    default: false
  })
  .action(async (options, ..._args) => {
    if (options.interact) {
      return interactFactory();
    }

    // 前端项目地址
    const destPath = options.destPath;
    const frontBuildPath = path.resolve(destPath, options.frontBuildPath);

    // const appType = options.appType
  
    await bundle({
      destPath: destPath,
      frontBuildPath: frontBuildPath,
    });
  })
  .example(
    "In the project directory",
    "jmm -d ./ -b dist",
  )
  .example(
    "static project",
    "jmm -d ../dweb_browser/plaoc/demo -b dist -t static"
  )
  .parse(Deno.args);

// 使用交互模式
const interactFactory = async () => {
  const isCwd = await Toggle.prompt("项目是否在当前地址?");
  let destPath = Deno.cwd();
  // 如果不在当前地址，需要让用户输入地址
  if (!isCwd) {
    destPath = await Input.prompt("请输入项目地址: ");
  }

  let frontBuildPath = await Input.prompt("请输入前端打包目录：");
  frontBuildPath = path.resolve(destPath, frontBuildPath);
  await bundle({
    destPath,
    frontBuildPath,
  });
};
