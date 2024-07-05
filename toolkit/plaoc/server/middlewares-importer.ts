import type { Router } from "./middlewares/router.ts";

// 导入中间件
export class MiddlewareImporter {
  static async init(path?: string) {
    if (!path) {
      return [];
    }
    try {
      const middlewares = await import(join("./middlewares", path));
      return (middlewares.default as Router).handlers;
    } catch (_e) {
      // console.log("MiddlewareConfig=>", e);
    }
  }
}

export function join(...paths: string[]): string {
  let joinedPath = paths[0];

  for (let i = 1; i < paths.length; i++) {
    let path = paths[i];
    // 处理 ./
    if (path.startsWith(".")) {
      path = path.substring(1);
    }
    const precededBySlash = joinedPath.endsWith("/");
    const followedBySlash = path.startsWith("/");

    // 处理前后都有 /
    if (precededBySlash && followedBySlash) {
      joinedPath += path.substring(1);
      // 处理前后没有 /
    } else if (!precededBySlash && !followedBySlash) {
      joinedPath += "/" + path;
    } else {
      // 一方有 /
      joinedPath += path;
    }
  }

  return joinedPath;
}
