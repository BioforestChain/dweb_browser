import JSZip from "jszip";
import fs from "node:fs";
import { createRequire } from "node:module";
import node_path from "node:path";
import process from "node:process";
import { colors } from "../deps/cliffy.ts";
import type { $JmmAppInstallManifest, $MMID } from "../helper/const.ts";
import { defaultMetadata, type $MetadataJsonGeneratorOptions } from "./const.ts";
import { GenerateTryFilepaths, isUrl } from "./util.ts";
import { WalkFiles } from "./walk-dir.ts";
import { walkDirToZipEntries, zipEntriesToZip, type $ZipEntry } from "./zip.ts";
const internalRequest = createRequire(import.meta.url);

export class MetadataJsonGenerator {
  readonly metadataFilepaths: string[];
  readonly baseMetadata: Partial<$JmmAppInstallManifest>;
  constructor(readonly flags: $MetadataJsonGeneratorOptions) {
    this.metadataFilepaths = (() => {
      const tryFilenames = ["metadata.json", "manifest.json", "package.json"];
      // 如果指定了项目目录，到项目目录里面搜索配置文件
      let dirs = [node_path.resolve(process.cwd(), flags.configDir ?? flags.webPublic ?? "")];
      if (flags.configDir) {
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
      await copyFolder(this.serverFilepaths, "server/middlewares");
    }
    return entries;
  }
}

export class BundleZipGenerator {
  private zipGetter: () => Promise<JSZip> = () => {
    throw new Error("no implement");
  };
  www_dir: undefined | string;
  constructor(
    readonly flags: $MetadataJsonGeneratorOptions,
    readonly plaoc: PlaocJsonGenerator,
    readonly server: BackendServerGenerator,
    readonly id: $MMID
  ) {
    const bundleTarget = flags.webPublic;
    /// 实时预览模式，使用代理html
    if (bundleTarget !== undefined && isUrl(bundleTarget)) {
      this.liveMode(bundleTarget);
    }
    /// 默认使用 本地文件夹模式
    else {
      this.localMode(bundleTarget);
    }
  }
  /**实时预览模式: plaoc serve http://xxxx */
  private liveMode(bundleTarget: string) {
    const liveUrl = bundleTarget;
    if (liveUrl === undefined) {
      throw new Error(`no found live-url。example:plaoc serve http://xxxx`);
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
          ...(await this.getBaseZipEntries(true)),
          index_html_file_entry,
          ...this.plaoc.tryReadPlaoc(),
          ...(await this.server.tryWriteServer()),
        ])
      );
    };
  }

  /**本地模式，直接打包本地源码 */
  private localMode(www_dir: string) {
    if (www_dir === undefined) {
      throw new Error(`no found dir. example:plaoc serve ./dir`);
    }
    this.www_dir = www_dir;
    this.zipGetter = async () => {
      return zipEntriesToZip(
        this.normalizeZipEntries([
          ...(await this.server.tryWriteServer()),
          ...(await this.getBaseZipEntries(false)),
          ...this.plaoc.tryReadPlaoc(),
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

  async getBaseZipEntries(isLive = false) {
    const entries: $ZipEntry[] = [];

    const addFiles_DistToUsr = async (addpath_full: string, pathalias: string, pathbase = "usr/") => {
      let data = null;
      // 远程的文件
      if (addpath_full.startsWith("http://") || addpath_full.startsWith("https://")) {
        data = await (await fetch(addpath_full)).text();
      }
      /// 本地文件
      else {
        // console.log("addpath_full", addpath_full);
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
      internalRequest.resolve(isLive ? "@plaoc/server/plaoc.server.dev.js" : "@plaoc/server/plaoc.server.js")
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
  private normalizeZipEntries(entries: $ZipEntry[]) {
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

export class BundleResourceNameHelper {
  static readonly metadataName = "metadata.json";

  constructor(readonly metadataFlagHelper: MetadataJsonGenerator) {}

  bundleName(force: boolean = false) {
    const metadata = this.metadataFlagHelper.readMetadata(force);
    return `${metadata.id}-${metadata.version}.zip`;
  }
}

/**
 * 注入plaoc.json 和 可编程后端
 * @param flags
 * @param metadataFlagHelper
 * @returns
 */
export const injectPrepare = (flags: $MetadataJsonGeneratorOptions, metadataFlagHelper: MetadataJsonGenerator) => {
  // 注入plaoc.json
  const plaocHelper = new PlaocJsonGenerator(flags);
  // 尝试注入可编程后端
  const injectServer = new BackendServerGenerator(flags);
  const data = metadataFlagHelper.readMetadata();
  const bundleFlagHelper = new BundleZipGenerator(flags, plaocHelper, injectServer, data.id);
  const bundleResourceNameHelper = new BundleResourceNameHelper(metadataFlagHelper);

  return { bundleFlagHelper, bundleResourceNameHelper };
};
