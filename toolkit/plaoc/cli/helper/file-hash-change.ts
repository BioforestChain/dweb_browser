import type { Buffer } from "node:buffer";
import fs from "node:fs";
const statCache = new Map<string, Buffer | false>();
export const fileHasChange = (filepath: string, curr = fs.existsSync(filepath) && fs.readFileSync(filepath)) => {
  const prev = statCache.get(filepath);
  let changed = false;
  if (prev === undefined) {
    changed = true;
  } else {
    if (curr === false) {
      if (prev !== false) {
        changed = true;
      }
    } else if (prev === false) {
      changed = true;
    } else {
      changed === curr.equals(prev);
    }
  }
  if (changed) {
    statCache.set(filepath, curr);
  }
  return changed;
};

export const initFileState = (filepath: string, content: Buffer | false) => {
  statCache.set(filepath, content);
};
export const clearChangeState = () => statCache.clear();
