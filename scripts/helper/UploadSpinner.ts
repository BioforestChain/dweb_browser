import * as colors from "jsr:@std/fmt/colors";
import ora, { type Options } from "npm:ora";
import prettyBytes from "npm:pretty-bytes";
import { StdinDiscarder } from "./StdinDiscarder.ts";

export class EasySpinner {
  static stdinDiscarder = new StdinDiscarder();
  readonly spinner;
  #ti;
  constructor(options?: Options & { redrawInterval?: number }) {
    // deno 下的， discardStdin 不能很好支持，所以我们手动进行捕捉
    this.spinner = ora({ discardStdin: false, ...options }).start();
    // UploadSpinner.stdinDiscarder.start();

    if (options?.redrawInterval) {
      this.#ti = setInterval(() => {
        this.redraw();
      }, options.redrawInterval);
    }
  }
  readonly startTime = Date.now();
  text: string | (() => string) = "";
  private getText() {
    const { text } = this;
    if (typeof text === "function") {
      return text();
    }
    return text;
  }
  redraw() {
    const now = Date.now();
    const second = ((now - this.startTime) / 1000).toFixed(1);
    this.spinner.text = colors.cyan(`+${second}s`) + ` ${this.getText()}`;
  }
  stop() {
    this.#isStoped = true;
    this.spinner.stopAndPersist();
    // UploadSpinner.stdinDiscarder.stop();
    clearInterval(this.#ti);
  }
  #isStoped = false;
  get isStoped() {
    return this.#isStoped;
  }
}

export class UploadSpinner {
  static stdinDiscarder = new StdinDiscarder();

  readonly spinner;
  #ti;
  constructor(readonly totalSize: number, options?: Options) {
    // deno 下的， discardStdin 不能很好支持，所以我们手动进行捕捉
    this.spinner = ora({ discardStdin: false, ...options }).start();
    UploadSpinner.stdinDiscarder.start();

    this.#ti = setInterval(() => {
      this.redraw();
    }, 500);
  }
  private uploadRecords: Array<{ size: number; time: number }> = [];
  private accSize = 0;

  addUploadedSize(size: number) {
    this.accSize += size;
    this.uploadRecords.unshift({ size, time: Date.now() });
    this.redraw();
  }
  redraw() {
    const now = Date.now();
    if (this.accSize === 0) {
      this.spinner.text = "Uploading...";
      return;
    }
    this.spinner.text = ((this.accSize / this.totalSize) * 100).toFixed(2) + "%";

    if (this.accSize === this.totalSize) {
      return;
    }
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
      this.spinner.text += ` | ${prettyBytes(0)}/s`;
    } else {
      this.spinner.text += ` | ${prettyBytes(secondsAccSize)}/s`;
    }
  }
  stop() {
    this.spinner.stopAndPersist();
    UploadSpinner.stdinDiscarder.stop();
    if (this.accSize !== this.totalSize) {
      console.log(colors.yellow(this.spinner.text));
    }
    clearInterval(this.#ti);
  }
}
