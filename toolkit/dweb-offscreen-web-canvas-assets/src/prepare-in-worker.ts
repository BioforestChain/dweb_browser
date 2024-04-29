/// <reference lib="webworker"/>

export function prepareInWorker(src: string) {
  if (typeof window !== "object") {
    return {
      toMainChannel: self as WorkerGlobalScope,
      isWorker: true,
    };
  } else {
    const currentScript = (document.currentScript ??
      [].slice
        .call(document.querySelectorAll("[data-worker-id]"))
        .find((scriptEle) => scriptEle.src == src))! as HTMLScriptElement;
    const randomId = currentScript.dataset.workerId!;
    delete currentScript.dataset.workerId;
    const mokeWorker = window[randomId] as import("./prepare-in-main.ts").MokeWorker;
    delete window[randomId];
    return {
      toMainChannel: mokeWorker.toMainPort,
      isWorker: false,
    };
  }
}
