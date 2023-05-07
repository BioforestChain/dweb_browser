// @ts-check
const debounce = require("lodash/debounce");
const fs = require("node:fs");
const path = require("node:path");
const android_assets_dir = path.join(
  __dirname,
  "../../android/app/src/main/assets"
);
const next_assets_dir = path.join(
  __dirname,
  "../../next/dweb-browser/src/Resources/Assets"
);
const origin_dir = path.join(__dirname, "../");

const tasks = ["bundle"].map((dirname) => {
  return {
    from: path.join(origin_dir, dirname),
    to: path.join(android_assets_dir, dirname),
  };
});
const nextTasks = ["bundle"].map((dirname) => {
  return {
    from: path.join(origin_dir, dirname),
    to: path.join(next_assets_dir, dirname),
  };
});

const doSync = () => {
  for (const task of tasks) {
    if (fs.existsSync(task.to)) {
      fs.rmSync(task.to, { recursive: true });
    }
    if (fs.existsSync(task.from)) {
      fs.cpSync(task.from, task.to, { recursive: true });
    }
  }
  for (const task of nextTasks) {
    if (fs.existsSync(task.to)) {
      fs.rmSync(task.to, { recursive: true });
    }
    if (fs.existsSync(task.from)) {
      fs.cpSync(task.from, task.to, { recursive: true });
    }
  }
  console.log("synced");
};

doSync();
if (process.argv.includes("--watch")) {
  const debounceSync = debounce(doSync, 500);
  for (const task of tasks) {
    if (fs.existsSync(task.from) === false) {
      break;
    }
    fs.watch(task.from, { recursive: true }, (event) => {
      console.log(event);
      debounceSync();
    });
  }
  for (const task of nextTasks) {
    if (fs.existsSync(task.from) === false) {
      break;
    }
    fs.watch(task.from, { recursive: true }, (event) => {
      console.log(event);
      debounceSync();
    });
  }
  return;
}
