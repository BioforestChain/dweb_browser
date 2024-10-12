import crypto from "node:crypto";
import fs from "node:fs";
import os from "node:os";
import node_path from "node:path";
import { WalkFiles, type WalkOptions } from "./WalkDir.ts";

export const calcDirHash = (srcDir: string, options?: WalkOptions & { reason?: string }) => {
  const hashBuilder = crypto.createHash("sha256");
  srcDir = node_path.normalize(srcDir);
  console.log("srcDir", srcDir);
  for (const entry of WalkFiles(srcDir, options)) {
    hashBuilder.update(entry.entrypath);
    const stat = fs.statSync(entry.entrypath);
    hashBuilder.update(JSON.stringify([stat.size, stat.ctimeMs, stat.mtimeMs]));
  }

  const dirHash = hashBuilder.digest("hex");

  const reason = options?.reason;
  const dirNameHash = crypto
    .createHash("sha256")
    .update(srcDir)
    .update(reason ? `?reason=${reason}` : "")
    .digest("hex")
    .slice(0, 8);

  return {
    isChange(hashDir = os.tmpdir(), prefix = "dir-hash.", suffix = ".sha256") {
      const hashFile = node_path.join(hashDir, `${prefix}${dirNameHash}${suffix}`);
      const nochange = fs.existsSync(hashFile) && fs.readFileSync(hashFile, "utf-8") === dirHash;
      return !nochange;
    },
    writeHash(hashDir = os.tmpdir(), prefix = "dir-hash.", suffix = ".sha256") {
      const hashFile = node_path.join(hashDir, `${prefix}${dirNameHash}${suffix}`);
      fs.writeFileSync(hashFile, dirHash);
    },
  };
};
