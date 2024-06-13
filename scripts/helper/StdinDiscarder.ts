export class StdinDiscarder {
  #readingController?: AbortController;
  start() {
    void this.#start();
  }
  async #start() {
    if (this.#readingController) {
      return;
    }
    this.#readingController = new AbortController();
    const signal = this.#readingController.signal;
    const abortPromise = new Promise<never>((_, reject) => {
      signal.addEventListener("abort", reject);
    });
    const wrapWithAbortSignal = <R>(promise: Promise<R>) => Promise.any([promise, abortPromise]);

    Deno.stdin.setRaw(true, { cbreak: true });
    const ASCII_ETX_CODE = 3; // Ctrl+C emits this code
    const stdin = Deno.stdin.readable.getReader();
    try {
      while (signal.aborted === false) {
        const read = await wrapWithAbortSignal(stdin.read());
        if (read.done) {
          return;
        }
        const chunk = read.value;
        if (chunk.byteLength === 1 && chunk[0] === ASCII_ETX_CODE) {
          Deno.exit(ASCII_ETX_CODE);
        }
      }
    } finally {
      stdin.cancel();
      stdin.releaseLock();
      this.#readingController = undefined;
      Deno.stdin.setRaw(false, { cbreak: false });
      Deno.stdin.close();
    }
  }
  stop() {
    this.#readingController?.abort();
  }
}
