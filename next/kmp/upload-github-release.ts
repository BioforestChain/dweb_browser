import fs from "node:fs";
import path from "node:path";
import webstreams from "node:stream/web";
import { Octokit } from "npm:@octokit/rest";
import mime from "npm:mime";
import { UploadSpinner, args, localProperties } from "./build-helper.ts";

export async function uploadRelease(scope: string, version: string, filepath: string) {
  const auth = localProperties.get("github.auth");
  if (undefined === auth) {
    console.error(
      "❌ 请在 local.properties 中配置 personal access tokens: https://github.com/settings/tokens?type=beta"
    );
    return;
  }
  const octokit = new Octokit({
    auth: auth,
  });

  const { data } = await octokit.rest.users.getAuthenticated();
  console.log("Hellow", data.name);

  const baseParams = {
    owner: "BioforestChain",
    repo: "dweb_browser",
  };
  const tag = `${scope}-${version}`;
  let release_id = await octokit.repos
    .getReleaseByTag({
      ...baseParams,
      tag: tag,
    })
    .then(
      (res) => res.data.id,
      (e) => {
        if (e.status !== 404) {
          throw e;
        }
      }
    );

  if (release_id === undefined) {
    const new_release_id = await octokit.repos
      .createRelease({
        ...baseParams,
        tag_name: tag,
      })
      .then((res) => res.data.id);
    release_id = new_release_id;
  }

  console.log("release_id", release_id);

  const totalSize = fs.statSync(filepath).size;
  const source = fs.createReadStream(filepath);

  const streamBody = new webstreams.ReadableStream<Uint8Array>({
    start(controller) {
      const spinner = new UploadSpinner(totalSize, { prefixText: `Uploading ${path.basename(filepath)}` });

      source.on("data", (_chunk) => {
        const chunk = _chunk as Uint8Array;
        spinner.addUploadSize(chunk.byteLength);
        controller.enqueue(chunk);
        source.pause();
      });
      source.on("end", () => {
        controller.close();
        spinner.stop();
      });
      source.on("error", (error) => {
        controller.error(error);
      });
    },
    pull() {
      source.resume();
    },
    cancel() {
      // Optionally handle the stream close or cleanup logic if needed
      source.destroy();
    },
  });

  const result = await octokit.repos.uploadReleaseAsset({
    headers: {
      "content-type": mime.getType(path.extname(filepath)) || "application/octet-stream",
      "content-size": totalSize,
    },
    ...baseParams,
    release_id: release_id,
    name: path.basename(filepath),
    data: streamBody as any,
  });
  console.log("上传成功", result.data.browser_download_url);
}

if (import.meta.main) {
  try {
    const throwErr = (reason: string) => {
      throw new Error(reason);
    };
    const scope = args.scope ?? throwErr("缺少 --scope= 配置，用于 github release tag={scope}-{version}");
    const version = args.version ?? throwErr("缺少 --version= 配置，用于 github release tag={scope}-{version}");
    const file = args.file ?? throwErr("缺少 --file= 配置，用于 github release assets");
    await uploadRelease(
      scope, //"desktop",
      version, //"3.6.0601",
      file //"/Users/kzf/Development/GitHub/dweb_browser/next/kmp/app/desktopApp/build/compose/binaries/main-release/DwebBrowser-3.6.0601-arm64.dmg"
    );
  } catch (error) {
    console.error("QAQ", error);
  }
}
