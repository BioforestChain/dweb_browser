// @ts-check
const debounce = require("lodash/debounce");
const fs = require("node:fs");
const path = require("node:path");
const android_assets_dir = path.join(
  __dirname,
  "../../android/app/src/main/assets"
);
const origin_dir = path.join(__dirname, "../");

const tasks = ["bundle", "cot"].map((dirname) => {
  return {
    from: path.join(origin_dir, dirname),
    to: path.join(android_assets_dir, dirname),
  };
});

const doSync = () => {
  for (const task of tasks) {
    if (fs.existsSync(task.to)) {
      fs.rmSync(task.to, { recursive: true });
    }
    fs.cpSync(task.from, task.to, { recursive: true });
  }
  console.log("synced");
};

doSync();
if (process.argv.includes("--watch")) {
  const debounceSync = debounce(doSync, 500);
  for (const task of tasks) {
    fs.watch(task.from, { recursive: true }, (event) => {
      debounceSync();
    });
  }
}
