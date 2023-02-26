(function polyfill() {
  const relList = document.createElement("link").relList;
  if (relList && relList.supports && relList.supports("modulepreload")) {
    return;
  }
  for (const link of document.querySelectorAll('link[rel="modulepreload"]')) {
    processPreload(link);
  }
  new MutationObserver((mutations) => {
    for (const mutation of mutations) {
      if (mutation.type !== "childList") {
        continue;
      }
      for (const node of mutation.addedNodes) {
        if (node.tagName === "LINK" && node.rel === "modulepreload")
          processPreload(node);
      }
    }
  }).observe(document, { childList: true, subtree: true });
  function getFetchOpts(script) {
    const fetchOpts = {};
    if (script.integrity)
      fetchOpts.integrity = script.integrity;
    if (script.referrerpolicy)
      fetchOpts.referrerPolicy = script.referrerpolicy;
    if (script.crossorigin === "use-credentials")
      fetchOpts.credentials = "include";
    else if (script.crossorigin === "anonymous")
      fetchOpts.credentials = "omit";
    else
      fetchOpts.credentials = "same-origin";
    return fetchOpts;
  }
  function processPreload(link) {
    if (link.ep)
      return;
    link.ep = true;
    const fetchOpts = getFetchOpts(link);
    fetch(link.href, fetchOpts);
  }
})();
const createSignal = () => {
  return new Signal();
};
class Signal {
  constructor() {
    this._cbs = /* @__PURE__ */ new Set();
    this.listen = (cb) => {
      this._cbs.add(cb);
      return () => this._cbs.delete(cb);
    };
    this.emit = (...args) => {
      for (const cb of this._cbs) {
        cb.apply(null, args);
      }
    };
  }
}
class PromiseOut {
  constructor() {
    this.promise = new Promise((resolve, reject) => {
      this.resolve = resolve;
      this.reject = reject;
    });
  }
}
const ALL_PROCESS_MAP = /* @__PURE__ */ new Map();
let acc_process_id = 0;
const allocProcessId = () => acc_process_id++;
const createProcess = async (env_script_url, fetch_port) => {
  console.log(env_script_url, fetch_port);
  const process_id = allocProcessId();
  const worker_url = URL.createObjectURL(
    new Blob(
      [
        `import("${env_script_url}").then(()=>postMessage("ready"),(err)=>postMessage("ERROR:"+err))`
      ],
      {
        // esm 代码必须有正确的 mime
        type: "application/javascript"
      }
    )
  );
  const worker = new Worker(worker_url, { type: "module" });
  await new Promise((resolve, reject) => {
    worker.addEventListener(
      "message",
      (event) => {
        if (event.data === "ready") {
          resolve();
        } else {
          reject(event.data);
        }
      },
      { once: true }
    );
  });
  worker.postMessage(["fetch-ipc-channel", fetch_port], [fetch_port]);
  const env_ready_po = new PromiseOut();
  const onEnvReady = (event) => {
    if (Array.isArray(event.data) && event.data[0] === "env-ready") {
      env_ready_po.resolve();
    }
  };
  worker.addEventListener("message", onEnvReady);
  await env_ready_po.promise;
  worker.removeEventListener("message", onEnvReady);
  ALL_PROCESS_MAP.set(process_id, { worker, fetch_port });
  on_create_process_signal.emit({
    process_id,
    env_script_url
  });
  return {
    process_id
  };
};
const _forceGetProcess = (process_id) => {
  const process = ALL_PROCESS_MAP.get(process_id);
  if (process === void 0) {
    throw new Error(`no found worker by id: ${process_id}`);
  }
  return process;
};
const runProcessMain = (process_id, config) => {
  const process = _forceGetProcess(process_id);
  process.worker.postMessage(["run-main", config]);
};
const createIpc = (process_id) => {
  const process = _forceGetProcess(process_id);
  const channel = new MessageChannel();
  process.worker.postMessage(["ipc-channel", channel.port2], [channel.port2]);
  return channel.port1;
};
const on_create_process_signal = createSignal();
const APIS = {
  createProcess,
  runProcessMain,
  createIpc
};
Object.assign(globalThis, APIS);
const html = String.raw;
on_create_process_signal.listen(({ process_id, env_script_url }) => {
  document.body.innerHTML += html`<div>
    <span>PID:${process_id}</span>
    <span>URL:${env_script_url}</span>
  </div>`;
});
const scriptRel = "modulepreload";
const assetsURL = function(dep, importerUrl) {
  return new URL(dep, importerUrl).href;
};
const seen = {};
const __vitePreload = function preload(baseModule, deps, importerUrl) {
  if (!deps || deps.length === 0) {
    return baseModule();
  }
  const links = document.getElementsByTagName("link");
  return Promise.all(deps.map((dep) => {
    dep = assetsURL(dep, importerUrl);
    if (dep in seen)
      return;
    seen[dep] = true;
    const isCss = dep.endsWith(".css");
    const cssSelector = isCss ? '[rel="stylesheet"]' : "";
    const isBaseRelative = !!importerUrl;
    if (isBaseRelative) {
      for (let i = links.length - 1; i >= 0; i--) {
        const link2 = links[i];
        if (link2.href === dep && (!isCss || link2.rel === "stylesheet")) {
          return;
        }
      }
    } else if (document.querySelector(`link[href="${dep}"]${cssSelector}`)) {
      return;
    }
    const link = document.createElement("link");
    link.rel = isCss ? "stylesheet" : scriptRel;
    if (!isCss) {
      link.as = "script";
      link.crossOrigin = "";
    }
    link.href = dep;
    document.head.appendChild(link);
    if (isCss) {
      return new Promise((res, rej) => {
        link.addEventListener("load", res);
        link.addEventListener("error", () => rej(new Error(`Unable to preload CSS for ${dep}`)));
      });
    }
  })).then(() => baseModule());
};
if ("ipcRenderer" in self) {
  (async () => {
    const { exportApis } = await __vitePreload(() => import("./openNativeWindow.preload-0848682a.js"), true ? [] : void 0, import.meta.url);
    exportApis(globalThis);
  })();
}
