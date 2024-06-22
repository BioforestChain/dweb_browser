import * as colors from "@std/fmt/colors";
import fs from "node:fs";
import path from "node:path";
import stream from "node:stream";
import webstream from "node:stream/web";
import { Octokit } from "npm:@octokit/rest";
import mime from "npm:mime";
import { UploadSpinner } from "../../scripts/helper/UploadSpinner.ts";
import { WalkFiles } from "../../scripts/helper/WalkDir.ts";
import { $ } from "../../scripts/helper/exec.ts";
import { createBaseResolveTo } from "../../scripts/helper/resolveTo.ts";
import { cliArgs, localProperties } from "./build-helper.ts";
const enableWebStream = false;

const resolveTo = createBaseResolveTo(import.meta.url);
export async function recordUploadRelease(taskId: string, uploadArgs: $UploadReleaseParams) {
  const ts = String.raw;
  const args = uploadArgs.map((it) => JSON.stringify(it));

  const uploadTestTsFile = resolveTo("./.upload-tasks.ts");
  if (fs.existsSync(uploadTestTsFile) === false) {
    fs.writeFileSync(
      uploadTestTsFile,
      ts`
      import { defineTask, tryExecTask } from "./build-helper.ts";
      if (import.meta.main) {
        void tryExecTask();
      }
      `
    );
  }
  if (fs.readFileSync(uploadTestTsFile, { encoding: "utf-8" }).includes(taskId) === false) {
    fs.appendFileSync(
      uploadTestTsFile,
      ts`
    defineTask('${taskId}',async()=>{
      await (await import('./${import.meta.url.split("/").pop()}')).doUploadRelease(${args})
    });
  `
    );
    await $([`deno`, `fmt`, uploadTestTsFile]);
  }
  console.log(colors.bgMagenta(`> deno task upload ${taskId}`));
}
export type $UploadReleaseParams = Parameters<typeof doUploadRelease>;

export async function doUploadRelease(tag: string, filepath_or_dirpath: string, glob?: string) {
  const auth = localProperties.get("github.auth");
  if (undefined === auth) {
    console.error(
      `❌ 请在 local.properties 中配置 github.auth=github_pat_***
      详见 [personal access tokens](https://github.com/settings/tokens?type=beta)`
    );
    return;
  }
  const octokit = new Octokit({
    auth: auth,
  });

  const { data } = await octokit.rest.users.getAuthenticated();
  console.log("Hellow", colors.bgBlue(data.name || data.login));

  const baseParams = {
    owner: "BioforestChain",
    repo: "dweb_browser",
  };

  console.info("💡 寻找 github-release", tag);

  let existsAssets: string[] = [];
  let release_id = await octokit.repos
    .getReleaseByTag({
      ...baseParams,
      tag: tag,
    })
    .then(
      (res) => {
        existsAssets = res.data.assets.map((it) => it.name);
        return res.data.id;
      },
      (e) => {
        if (e.status !== 404) {
          throw e;
        }
      }
    );

  if (release_id === undefined) {
    console.info("💡 未找到，创建 github-release", tag);
    const new_release_id = await octokit.repos
      .createRelease({
        ...baseParams,
        tag_name: tag,
      })
      .then((res) => res.data.id);
    release_id = new_release_id;
  }

  console.log("github release id =", release_id);
  const uploadFiles: string[] = [];
  if (fs.statSync(filepath_or_dirpath).isDirectory()) {
    /// 文件从小到大排序
    [...WalkFiles(filepath_or_dirpath, { match: glob })]
      .sort((a, b) => a.stats.size - b.stats.size)
      .forEach((entry) => {
        uploadFiles.push(entry.entrypath);
      });
  } else {
    uploadFiles.push(filepath_or_dirpath);
  }

  console.info("💡 开始执行文件上传");
  for (const [index, filepath] of uploadFiles.entries()) {
    if (existsAssets.includes(path.basename(filepath))) {
      continue;
    }
    const stat = fs.statSync(filepath);
    const totalSize = stat.size;

    const prefixText: string[] = [];
    const suffixText: string[] = [];
    uploadFiles.forEach((_filepath, _index) => {
      const filename = path.basename(_filepath);
      if (_index < index) {
        prefixText.push(colors.green(`✅ ${filename}`));
        return;
      }
      if (_index === index) {
        prefixText.push(colors.blue(`⏳ ${filename}`));
        return;
      }
      const clock_symbols = ["🕐", "🕑", "🕒", "🕓", "🕔", "🕕", "🕖", "🕗", "🕘", "🕙", "🕚", "🕛"];
      suffixText.push(colors.gray(`${clock_symbols[(_index - index) % clock_symbols.length]} ${filename}`));
    });
    const spinner = new UploadSpinner(totalSize, {
      prefixText: prefixText.join("\n") + `\n`,
      suffixText: "\n" + suffixText.join("\n"),
    });

    const result = await octokit.repos
      .uploadReleaseAsset({
        headers: {
          "Content-Type": mime.getType(path.extname(filepath)) || "application/octet-stream",
          // "Content-Length": totalSize.toString(),
          // "Transfer-Encoding": "chunked",
        },
        ...baseParams,
        release_id: release_id,
        name: path.basename(filepath),
        /// 目前流上传在deno中有bug
        data: (enableWebStream
          ? stream.Readable.toWeb(fs.createReadStream(filepath)).pipeThrough(
              new webstream.TransformStream<Uint8Array, Uint8Array>({
                transform(chunk, controller) {
                  spinner.addUploadedSize(chunk.byteLength);
                  controller.enqueue(chunk);
                },
              })
            )
          : fs.readFileSync(filepath)) as any,
      })
      .finally(() => {
        spinner.stop();
      });
    console.log("上传成功", result.data.browser_download_url);
  }
}

if (import.meta.main) {
  try {
    const throwErr = (reason: string) => {
      throw new Error(reason);
    };
    const scope = cliArgs.scope ?? throwErr("缺少 --scope= 配置，用于 github release tag={scope}-{version}");
    const version = cliArgs.version ?? throwErr("缺少 --version= 配置，用于 github release tag={scope}-{version}");
    const file = cliArgs.file ?? throwErr("缺少 --file= 配置，用于 github release assets");
    await doUploadRelease(
      scope, //"desktop",
      version, //"3.6.0601",
      file //"/Users/kzf/Development/GitHub/dweb_browser/next/kmp/app/desktopApp/build/compose/binaries/main-release/DwebBrowser-3.6.0601-arm64.dmg"
    );
  } catch (error) {
    console.error("QAQ", error);
  }
}
