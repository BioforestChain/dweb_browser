import { parseArgs } from "@std/cli/parse-args";
export const args = parseArgs(Deno.args);

import fs from "node:fs";
import { createBaseResolveTo } from "../../scripts/helper/resolveTo.ts";
const resolveTo = createBaseResolveTo(import.meta.url);
export const loadProperties = (filepath: string) => {
  const properties = new Map<string, string>();
  fs.readFileSync(filepath, "utf-8")
    .split("\n")
    .map((line) => line.trim())
    .filter((line) => line.startsWith("#") === false && line !== "")
    .forEach((line) => {
      const splitIndex = line.indexOf("=");
      if (splitIndex !== -1) {
        const key = line.slice(0, splitIndex).trim();
        let value = line.slice(splitIndex + 1).trim();
        if (value.startsWith(`"`) && value.endsWith(`"`)) {
          value = value.slice(1, -1);
        }
        properties.set(key, value);
      }
    });
  return Object.assign(properties, {
    getBoolean: (key: string) => {
      return properties.get(key) === "true";
    },
  });
};
export const localProperties = loadProperties(resolveTo("./local.properties"));

import ora from "npm:ora";
import prettyBytes from "npm:pretty-bytes";
export class UploadSpinner {
  readonly spinner;
  #ti;
  constructor(readonly totalSize: number, ...args: Parameters<typeof ora>) {
    this.spinner = ora(...args).start();
    this.#ti = setInterval(() => {
      this.redraw();
    }, 500);
  }
  private uploadRecords: Array<{ size: number; time: number }> = [];
  private accSize = 0;

  addUploadSize(size: number) {
    this.accSize += size;
    this.uploadRecords.unshift({ size, time: Date.now() });
    this.redraw();
  }
  redraw() {
    const now = Date.now();
    this.spinner.text = ((this.accSize / this.totalSize) * 100).toFixed(2) + "%";
    let secondsAccSize = 0;
    let endTime = now;
    for (const item of this.uploadRecords) {
      if (now - item.time > 1000) {
        break;
      }
      secondsAccSize += item.size;
      endTime = item.time;
    }
    if (endTime === now) {
      this.spinner.suffixText = `${prettyBytes(0)}/s`;
    } else {
      this.spinner.suffixText = `${prettyBytes((secondsAccSize / (now - endTime)) * 1000)}/s`;
    }
  }
  stop() {
    this.spinner.stop();
    clearInterval(this.#ti);
  }
}
