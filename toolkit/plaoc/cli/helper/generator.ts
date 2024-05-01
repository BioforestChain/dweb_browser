import JSZip from "jszip";
import fs from "node:fs";
import { createRequire } from "node:module";
import node_path from "node:path";
import process from "node:process";
import type { $JmmAppInstallManifest, $MMID } from "../deps/dweb-browser/core.ts";
import { colors } from "../deps/cliffy.ts";
import { $MetadataJsonGeneratorOptions, SERVE_MODE, defaultMetadata } from "./const.ts";
import { GenerateTryFilepaths } from "./util.ts";
import { WalkFiles } from "./walk-dir.ts";
import { $ZipEntry, walkDirToZipEntries, zipEntriesToZip } from "./zip.ts";
const internalRequest = createRequire(import.meta.url);

export class MetadataJsonGenerator {
  readonly metadataFilepaths: string[];
  readonly baseMetadata: Partial<$JmmAppInstallManifest>;
  constructor(readonly flags: $MetadataJsonGeneratorOptions) {
    this.metadataFilepaths = (() => {
      const tryFilenames = ["metadata.json", "manifest.json", "package.json"];
      // 如果指定了项目目录，到项目目录里面搜索配置文件
      let dirs = [node_path.resolve(process.cwd(), flags.configDir ?? flags.webPublic ?? "")];
      if (flags.mode === SERVE_MODE.USR_WWW) {
        const www_dir = flags.configDir;
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
        const metadata = JSON.parse(fs.readFileSync(filepath, "utf-8"));
        if (typeof metadata.author === "string") {
          metadata.author = [metadata.author];
        }

        console.log(colors.gray("using metadata file:"), colors.cyan(filepath));
        return metadata;
        // deno-lint-ignore no-empty
      } catch {}
    }

    return {};
  }

  private metadata: undefined | $JmmAppInstallManifest;
  readMetadata(force = false) {
    if (force) {
      this.metadata = undefined;
    }
    return (this.metadata ??= {
      ...defaultMetadata,
      ...this.tryReadMetadata(),
      ...this.baseMetadata,
    } as $JmmAppInstallManifest);
  }
}
/**
 * 注入转发服务
 */
export class PlaocJsonGenerator {
  readonly plaocFilepaths: string;

  constructor(readonly flags: $MetadataJsonGeneratorOptions) {
    // plaoc.json
    this.plaocFilepaths = (() => {
      const tryFilenames = "plaoc.json";
      // 如果指定了项目目录，到项目目录里面搜索配置文件
      const file = node_path.resolve(process.cwd(), flags.configDir ?? "", tryFilenames);
      return file;
    })();
  }

  tryReadPlaoc() {
    const entries: $ZipEntry[] = [];
    try {
      const entry = fs.statSync(this.plaocFilepaths);
      if (entry.isFile()) {
        const data = fs.readFileSync(this.plaocFilepaths);
        entries.push({
          dir: false,
          path: `usr/www/plaoc.json`,
          data: data,
          time: entry.mtime,
        });
      }
      // deno-lint-ignore no-empty
    } catch {}
    return entries;
  }
}

/**
 * 可编程后端注入
 */
export class BackendServerGenerator {
  readonly serverFilepaths: string | null;
  constructor(readonly flags: $MetadataJsonGeneratorOptions) {
    this.serverFilepaths = (() => {
      if (flags.webServer == undefined) return null;
      // 如果指定了项目目录，到项目目录里面搜索配置文件
      return node_path.resolve(process.cwd(), flags.webServer);
    })();
  }

  async tryWriteServer() {
    const entries: $ZipEntry[] = [];
    async function copyFolder(src: string, pathalias: string) {
      if (fs.statSync(src).isFile()) {
        entries.push({
          dir: false,
          path: `usr/${pathalias}`.replace(/\\/g, "/"),
          data: fs.readFileSync(src),
        });
      } else {
        entries.push({
          dir: true,
          path: `usr/${pathalias}`.replace(/\\/g, "/"),
          time: new Date(),
        });
        for (const entry of WalkFiles(src)) {
          const child_addpath = entry.entrypath;
          await copyFolder(child_addpath, child_addpath.replace(src, pathalias));
        }
        return;
      }
    }
    // 可编程后端注入
    if (this.serverFilepaths) {
      await copyFolder(this.serverFilepaths, "server/middleware");
    }
    return entries;
  }
}

export class BundleZipGenerator {
  private zipGetter: () => Promise<JSZip> = () => {
    throw new Error("no implement");
  };
  readonly www_dir: undefined | string;
  constructor(
    readonly flags: $MetadataJsonGeneratorOptions,
    readonly plaoc: PlaocJsonGenerator,
    readonly server: BackendServerGenerator,
    readonly id: $MMID
  ) {
    const bundleTarget = flags.webPublic;
    /// 实时预览模式，使用代理html
    if (
      flags.mode === SERVE_MODE.LIVE ||
      (flags.mode === undefined && bundleTarget !== undefined && /^http[s]{0,1}:\/\//.test(bundleTarget))
    ) {
      const liveUrl = bundleTarget;
      if (liveUrl === undefined) {
        throw new Error(`no found live-url when serve-mode is '${flags.mode}'`);
      }
      const html = String.raw;
      const index_html_file_entry = {
        dir: false,
        path: `usr/www/index.html`,
        data: html`<script>
          const proxyUrl = new URL(location.href);
          proxyUrl.searchParams.set("X-Plaoc-Proxy", ${JSON.stringify(liveUrl)});
          location.replace(proxyUrl.href);
        </script>`,
        time: new Date(0),
      } satisfies $ZipEntry;
      this.zipGetter = async () => {
        return zipEntriesToZip(
          this.normalizeZipEntries([
            ...(await this.getBaseZipEntries(flags.dev)),
            index_html_file_entry,
            ...plaoc.tryReadPlaoc(),
            ...(await server.tryWriteServer()),
          ])
        );
      };
    }
    /// 生产模式
    else if (flags.mode === SERVE_MODE.PROD) {
      const bundle_file = bundleTarget;
      if (bundle_file === undefined) {
        throw new Error(`no found bundle-file when serve-mode is '${flags.mode}'`);
      }
      this.zipGetter = () => JSZip.loadAsync(bundle_file);
    }
    /// 默认使用 本地文件夹模式
    else {
      const www_dir = bundleTarget;
      if (www_dir === undefined) {
        throw new Error(`no found dir when serve-mode is '${flags.mode}'`);
      }
      this.www_dir = www_dir;
      this.zipGetter = async () => {
        return zipEntriesToZip(
          this.normalizeZipEntries([
            ...(await server.tryWriteServer()),
            ...(await this.getBaseZipEntries(flags.dev)),
            ...plaoc.tryReadPlaoc(),
            ...walkDirToZipEntries(www_dir).map((entry) => {
              return {
                ...entry,
                path: (`usr/www/` + entry.path).replace(/\\/g, "/"),
              };
            }),
          ])
        );
      };
    }
  }
  async getBaseZipEntries(dev = false) {
    const entries: $ZipEntry[] = [];

    const addFiles_DistToUsr = async (addpath_full: string, pathalias: string, pathbase = "usr/") => {
      let data = null;
      // 远程的文件
      if (addpath_full.startsWith("http://") || addpath_full.startsWith("https://")) {
        data = await (await fetch(addpath_full)).text();
      }
      /// 本地文件
      else {
        console.log('addpath_full',addpath_full)
        if (fs.statSync(addpath_full).isFile()) {
          data = fs.readFileSync(addpath_full);
        } else {
          for (const entry of WalkFiles(addpath_full)) {
            const child_addpath = node_path.join(addpath_full, entry.relativepath);
            await addFiles_DistToUsr(child_addpath, child_addpath.replace(addpath_full, pathalias), pathbase);
          }
          return;
        }
      }
      entries.push({
        dir: false,
        path: `${pathbase}${pathalias}`.replace(/\\/g, "/"),
        data: data,
      });
    };
    const distDir = node_path.dirname(
      internalRequest.resolve(dev ? "@plaoc/server/plaoc.server.dev.js" : "@plaoc/server/plaoc.server.js")
    );
    for (const entry of WalkFiles(distDir)) {
      await addFiles_DistToUsr(entry.entrypath, `server/${entry.relativepath}`);
    }
    return entries;
  }
  /**
   * 补充 dir=true
   * @param entries
   * @returns
   */
  normalizeZipEntries(entries: $ZipEntry[]) {
    const entryMap = new Map<string, $ZipEntry>();
    function* ReadParentPaths(entrypath: string) {
      while (true) {
        const dirname = node_path.dirname(entrypath);
        if (dirname === ".") {
          break;
        }
        yield dirname;
        entrypath = dirname;
      }
    }

    const allDirnamePaths = new Set<string>();
    for (const entry of entries) {
      entryMap.set(entry.path, entry);
      for (const dirname of ReadParentPaths(entry.path)) {
        allDirnamePaths.add(dirname);
      }
    }

    for (const dirname of allDirnamePaths) {
      if (entryMap.has(dirname) === false) {
        entryMap.set(dirname, { path: dirname, dir: true });
      }
    }

    return [...entryMap.values()].sort((a, b) => {
      return (b.dir ? 1 : 0) - (a.dir ? 1 : 0);
    });
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

  constructor(readonly flags: $NameFlagHelperOptions, readonly metadataFlagHelper: MetadataJsonGenerator) {}
}
