/// <reference lib="webworker"/>

export function prepareInWorker(src: string) {
  if (typeof window !== "object") {
    return {
      toMainChannel: self as WorkerGlobalScope,
      isWorker: true,
    };
  } else {
    let currentScript = document.currentScript;
    if (!currentScript) {
      document.querySelectorAll<HTMLScriptElement>("script[data-worker-id]").forEach((scriptEle) => {
        if (scriptEle.src == src) {
          currentScript = scriptEle;
        }
      });
    }
    if (!currentScript) {
      throw new Error(`no found script: ${src}`);
    }
    const randomId = currentScript.dataset.workerId!;
    delete currentScript.dataset.workerId;
    const mokeWorker = Reflect.get(window, randomId) as import("./prepare-in-main.ts").MokeWorker;
    Reflect.deleteProperty(window, randomId);
    return {
      toMainChannel: mokeWorker.toMainPort,
      isWorker: false,
    };
  }
}
