import crypto from "node:crypto";
import fs from "node:fs";
import os from "node:os";
import node_path from "node:path";
import { fileURLToPath, pathToFileURL } from "node:url";

export const importMapResolver = async (importMapURL: string) => {
  if (importMapURL?.endsWith(".jsonc")) {
    const importMapJsonStr = fs.readFileSync(fileURLToPath(importMapURL), "utf-8");
    const hash = crypto.createHash("sha256").update(importMapURL).digest("hex");
    const tmpJsonFile = node_path.join(os.tmpdir(), `import_map.tmp-${hash.slice(0, 6)}.json`);
    const importMapJson = Function(`return (${importMapJsonStr})`)();
    for (const [key, value] of Object.entries(importMapJson.imports as Record<string, string>)) {
      if (value.startsWith("./")) {
        // 添加pathToFileURL将路径转为file协议开头，为了修复windows系统路径 specifierKey prefix 异常
        importMapJson.imports[key] = pathToFileURL(
          node_path.resolve(fileURLToPath(importMapURL), "../", value) + (key.endsWith("/") ? "/" : "")
        );
      }
    }
    fs.writeFileSync(tmpJsonFile, JSON.stringify({ imports: importMapJson.imports }, null, 2));
    importMapURL = pathToFileURL(tmpJsonFile).href;
    console.log("importMapURL", importMapURL);
  }
  return importMapURL;
};
