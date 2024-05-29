import { walk } from "https://deno.land/std@0.174.0/fs/mod.ts";
import { dirname } from "https://deno.land/std@0.174.0/path/mod.ts";
import { parse } from "https://deno.land/std@0.174.0/encoding/yaml.ts";

const config = parse(await Deno.readTextFile("deno.jsonc")) as { imports: Record<string, string> };

async function fixImports(dir: string) {

  for await (const { path, isFile } of walk(dir)) {
    if (isFile && path.endsWith(".ts")) {
      const content = await Deno.readTextFile(path);
      let newContent = content
      
      for (const [importTo, importFrom] of Object.entries(config.imports)) {
        const importFromNew = escapeRegExp(importFrom)//importFrom.replace(/\/$/, "")
        const regexp = new RegExp(`import(?:\\s+type)?\\s+\\{[^}]*\\}\\s+from\\s+["']${importFromNew}`, "g");
        newContent = newContent.replace(
          regexp,
          (match) => {
            const currentDir = dirname(path);
            if (currentDir.includes("dweb-help")) {
              // 在 dweb-helper 文件夹内
              return match.replace(importFrom, ".");
            } else {
              // 在 dweb-helper 文件夹外
              return match.replace(importFrom, importTo);
            }
          }
        );
      }
      
      if (newContent !== content) {
        await Deno.writeTextFile(path, newContent);
        console.log(`Fixed imports in ${path}`);
      }
    }
  }
}

function escapeRegExp(text: string) {
  return text.replace(/[.*+?^${}()|[\]\\]/g, '\\$&'); 
}

await fixImports("./toolkit");