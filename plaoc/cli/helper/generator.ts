import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
import JSZip from "npm:jszip";
import { $AppMetaData, $MMID } from "../../deps.ts";
import { SERVE_MODE, defaultMetadata } from "./const.ts";
import { GenerateTryFilepaths } from "./util.ts";
import { $ZipEntry, walkDirToZipEntries, zipEntriesToZip } from "./zip.ts";

export type $MetadataJsonGeneratorOptions = {
  metadata?: unknown[];
  mode?: unknown;
  dev?: boolean;
  version?: string;
  id?: string;
  dir?: string;
  _?: unknown[];
};

export class MetadataJsonGenerator {
  readonly metadataFilepaths: string[];
  readonly baseMetadata: Partial<$AppMetaData>;
  constructor(readonly flags: $MetadataJsonGeneratorOptions) {
    this.metadataFilepaths =
      flags.metadata?.map((filepath) =>
        path.resolve(Deno.cwd(), filepath + "")
      ) ??
      (() => {
        const tryFilenames = ["manifest.json", "package.json"];
        // 如果指定了项目目录，到项目目录里面搜索配置文件
        let dirs = [path.resolve(Deno.cwd(), flags.dir ?? "")];
        if (flags.mode === SERVE_MODE.USR_WWW) {
          const www_dir = flags._?.[0]?.toString();
          if (www_dir) {
            dirs = [www_dir, ...dirs];
          }
        }

        return [...GenerateTryFilepaths(tryFilenames, dirs)];
      })();
    this.baseMetadata = {};
    const { baseMetadata } = this;

    for (const key of ["id", "version"] as const) {
      if (flags[key] !== undefined) {
        baseMetadata[key] = flags[key] as never;
      }
    }
  }
  tryReadMetadata() {
    for (const filepath of this.metadataFilepaths) {
      try {
        return JSON.parse(fs.readFileSync(filepath, "utf-8"));
        // deno-lint-ignore no-empty
      } catch {}
    }

    return {};
  }

  private metadata: undefined | $AppMetaData;
  readMetadata(force = false) {
    if (force) {
      this.metadata = undefined;
    }
    return (this.metadata ??= {
      ...defaultMetadata,
      ...this.tryReadMetadata(),
      ...this.baseMetadata,
    } as $AppMetaData);
  }
}

export type $BundleZipGeneratorOptions = {
  mode?: unknown;
  dev?: boolean;
  _?: unknown[];
};

export class BundleZipGenerator {
  private zipGetter: () => Promise<JSZip> = () => {
    throw new Error("no implement");
  };
  readonly www_dir: undefined | string;
  constructor(readonly flags: $BundleZipGeneratorOptions, readonly id: $MMID) {
    const bundleTarget = flags._?.[0]?.toString();
    /// 实时预览模式，使用代理html
    if (
      flags.mode === SERVE_MODE.LIVE ||
      (flags.mode === undefined &&
        bundleTarget !== undefined &&
        /^http[s]{0,1}:\/\//.test(bundleTarget))
    ) {
      const liveUrl = bundleTarget;
      if (liveUrl === undefined) {
        throw new Error(`no found live-url when serve-mode is '${flags.mode}'`);
      }

      const index_html_file_entry = {
        dir: false,
        path: `usr/www/index.html`,
        data: `<script>
        const proxyUrl = new URL(location.href);
        proxyUrl.searchParams.set("X-Plaoc-Proxy",${JSON.stringify(liveUrl)})
        location.replace(proxyUrl.href);
        </script>`,
        time: new Date(0),
      } satisfies $ZipEntry;
      this.zipGetter = async () =>
        await zipEntriesToZip([
          ...this.getBaseZipEntries(flags.dev),
          index_html_file_entry,
        ]);
    }
    /// 生产模式
    else if (flags.mode === SERVE_MODE.PROD) {
      const bundle_file = bundleTarget;
      if (bundle_file === undefined) {
        throw new Error(
          `no found bundle-file when serve-mode is '${flags.mode}'`
        );
      }
      this.zipGetter = () => JSZip.loadAsync(bundle_file);
    }

    /// 默认使用 本地文件夹模式
    // if (flags.mode === SERVE_MODE.USR_WWW)
    else {
      const www_dir = bundleTarget;
      if (www_dir === undefined) {
        throw new Error(`no found dir when serve-mode is '${flags.mode}'`);
      }
      this.www_dir = www_dir;
      this.zipGetter = async () =>
        await zipEntriesToZip([
          ...this.getBaseZipEntries(flags.dev),
          ...walkDirToZipEntries(www_dir).map((entry) => {
            return {
              ...entry,
              path: `usr/www/` + entry.path,
            };
          }),
        ]);
    }
  }
  getBaseZipEntries(dev = false) {
    const entries: $ZipEntry[] = [
      {
        dir: true,
        path: `usr`,
      },
      {
        dir: true,
        path: `usr/server`,
      },
      {
        dir: true,
        path: `usr/www`,
      },
    ];
    const addFile_DistToUsr = (filepath: string, alias: string = filepath) => {
      entries.push({
        dir: false,
        path: `usr/${alias}`,
        data: fs.readFileSync(
          fileURLToPath(import.meta.resolve(`../../dist/${filepath}`))
        ),
      });
    };
    if (dev) {
      addFile_DistToUsr("server/plaoc.server.dev.js", "server/plaoc.server.js");
      addFile_DistToUsr("server/plaoc.emulator.js");
    } else {
      addFile_DistToUsr("server/plaoc.server.js");
    }
    return entries;
  }
  private zip: undefined | JSZip;
  /** 获得打包的zip文件 */
  async bundleZip(force = false) {
    if (force) {
      this.zip = undefined;
    }
    return (this.zip ??= await this.zipGetter());
  }
}

export type $NameFlagHelperOptions = {
  id?: string;
  version?: string;
  _?: unknown[];
};
export class NameFlagHelper {
  get bundleName() {
    const metadata = this.metadataFlagHelper.readMetadata();
    return `${metadata.id}-${metadata.version}.zip`;
  }
  readonly bundleMime = "application/zip";
  readonly metadataName = "metadata.json";
  readonly metadataMime = "application/json";

  constructor(
    readonly flags: $NameFlagHelperOptions,
    readonly metadataFlagHelper: MetadataJsonGenerator
  ) {}
}
