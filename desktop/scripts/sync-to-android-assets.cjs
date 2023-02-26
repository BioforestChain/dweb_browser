// @ts-check
const fs = require("node:fs");
const path = require("node:path");

const android_assets_dir = path.join(
  __dirname,
  "../../android/app/src/main/assets"
);
const origin_dir = path.join(__dirname, "../");

for (const dirname of ["bundle"]) {
  const to = path.join(android_assets_dir, dirname);
  fs.rmSync(to, { recursive: true });
  const from = path.join(origin_dir, dirname);
  fs.cpSync(from, to, { recursive: true });
}
