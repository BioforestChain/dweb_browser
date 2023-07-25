import JSZip from "npm:jszip";
import { WalkAny } from "./WalkDir.ts";

export const bundleDirToZip = (bundleDir: string, zip?: JSZip) => {
  return zipEntriesToZip(walkDirToZipEntries(bundleDir), zip);
};

export type $ZipEntry =
  | {
      dir: false;
      path: string;
      data: Uint8Array | string;
      time?: Date;
    }
  | {
      dir: true;
      path: string;
      time?: Date;
    };
export const walkDirToZipEntries = (bundleDir: string) => {
  const entries: $ZipEntry[] = [];
  for (const entry of WalkAny(bundleDir)) {
    if (entry.isFile) {
      entries.push({
        dir: false,
        path: entry.relativepath,
        data: entry.read(),
        time: entry.stats.mtime,
      });
    } else {
      entries.push({
        dir: true,
        path: entry.relativepath,
        time: entry.stats.mtime,
      });
    }
  }
  return entries;
};

export const zipEntriesToZip = (entries: Iterable<$ZipEntry>, zip = new JSZip()) => {
  for (const entry of entries) {
    if (entry.dir) {
      zip.file(entry.path, null, {
        dir: true,
        date: entry.time ?? new Date(1e5),
      });
    } else {
      zip.file(entry.path, entry.data, {
        date: entry.time ?? new Date(1e5),
      });
    }
  }
  return zip;
};
// class MyDate extends Date {
//   constructor(...args: any[]) {
//     if (
//       enableDebug &&
//       new Error().stack?.includes("Object.statSync") === false
//     ) {
//       debugger;
//     }
//     super(...args);
//   }
// }
// globalThis.Date = MyDate;
// globalThis.enableDebug = false;
