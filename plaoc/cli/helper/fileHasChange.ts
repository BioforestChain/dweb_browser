import fs from "node:fs";
import { isDeepStrictEqual } from "node:util";
const statCache = new Map<string, fs.Stats | false>();
export const fileHasChange = (
  filepath: string,
  curr = fs.existsSync(filepath) && fs.statSync(filepath)
) => {
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
      changed = !isDeepStrictEqual(curr, prev);
    }
  }
  if (changed) {
    statCache.set(filepath, curr);
  }
  return changed;
};
