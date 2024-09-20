import { Command } from "../deps/cliffy.ts";

/// 创建独立的Android/iOS APP引导程序。

export const doWizardCommand = new Command()
  .description("Create a standalone Android/iOS app bootloader.")
  // .option("-o --out <out:string>", "Output directory.", {
  //   default: "app",
  // })
  .action(() => {
    doWizard();
  });

const doWizard = () => {
  console.log("create webserver");
};
