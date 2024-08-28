import { node_path, node_process } from "../deps/node.ts";
/**
 * 基于指定的roots和filenames，合并生成路径，并且目录会不断向上走。
 * 比如说:
 * ```js
 * filenames = ['x.mjs','b.cjs']
 * dirs = ['/aaa/bbb','/ccc/ddd/eee']
 *
 * [...GenerateTryFilepaths(filenames, dirs)] ===
 * [
 *
 * '/aaa/bbb/x.mjs',
 * '/aaa/bbb/b.cjs',
 * '/ccc/ddd/eee/x.mjs',
 * '/ccc/ddd/eee/b.cjs',
 *
 * '/aaa/x.mjs',
 * '/aaa/b.cjs',
 * '/ccc/ddd/x.mjs',
 * '/ccc/ddd/b.cjs',
 *
 * '/x.mjs',
 * '/b.cjs',
 * '/ccc/x.mjs',
 * '/ccc/b.cjs',
 *
 * // 自动去除重复
 * // '/x.mjs',
 * // '/b.cjs',
 * ]
 * ```
 * @param filenames
 * @param dirs
 */
export function* GenerateTryFilepaths(filenames: string[], dirs: string[]) {
  const tryFilepaths = new Set<string>();
  while (dirs.length > 0) {
    const superDirs: string[] = [];
    for (const dir of dirs) {
      /// 将可能的文件路径添加到 tryFilepaths 中
      for (const name of filenames) {
        const filepath = node_path.resolve(dir, name);
        if (tryFilepaths.has(filepath) === false) {
          tryFilepaths.add(filepath);
          yield filepath;
        }
      }
      /// 目录向上走
      const superDir = node_path.dirname(dir);
      if (superDir !== dir) {
        superDirs.push(superDir);
      }
    }
    dirs = superDirs;
  }
  return tryFilepaths;
}

/**
 * 获取manifest文件目录
 * @param cwd 当前路径
 * @returns
 */
export function getManifestFilePath(cwd?: string) {
  if (cwd) {
    return node_path.join(node_process.cwd(), cwd, "manifest.json");
  }

  return node_path.join(node_process.cwd(), "manifest.json");
}

export function isUrl(target: string) {
  return /^http[s]{0,1}:\/\//.test(target);
}
