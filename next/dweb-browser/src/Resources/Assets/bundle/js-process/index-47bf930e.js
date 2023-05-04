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
const isPromiseLike = (value) => {
  return value instanceof Object && typeof value.then === "function";
};
class PromiseOut {
  constructor() {
    this.is_resolved = false;
    this.is_rejected = false;
    this.is_finished = false;
    this.promise = new Promise((resolve, reject) => {
      this.resolve = (value) => {
        try {
          if (isPromiseLike(value)) {
            value.then(this.resolve, this.reject);
          } else {
            this.is_resolved = true;
            this.is_finished = true;
            resolve(this.value = value);
            this._runThen();
            this._innerFinallyArg = Object.freeze({
              status: "resolved",
              result: this.value
            });
            this._runFinally();
          }
        } catch (err) {
          this.reject(err);
        }
      };
      this.reject = (reason) => {
        this.is_rejected = true;
        this.is_finished = true;
        reject(this.reason = reason);
        this._runCatch();
        this._innerFinallyArg = Object.freeze({
          status: "rejected",
          reason: this.reason
        });
        this._runFinally();
      };
    });
  }
  onSuccess(innerThen) {
    if (this.is_resolved) {
      this.__callInnerThen(innerThen);
    } else {
      (this._innerThen || (this._innerThen = [])).push(innerThen);
    }
  }
  onError(innerCatch) {
    if (this.is_rejected) {
      this.__callInnerCatch(innerCatch);
    } else {
      (this._innerCatch || (this._innerCatch = [])).push(innerCatch);
    }
  }
  onFinished(innerFinally) {
    if (this.is_finished) {
      this.__callInnerFinally(innerFinally);
    } else {
      (this._innerFinally || (this._innerFinally = [])).push(innerFinally);
    }
  }
  _runFinally() {
    if (this._innerFinally) {
      for (const innerFinally of this._innerFinally) {
        this.__callInnerFinally(innerFinally);
      }
      this._innerFinally = void 0;
    }
  }
  __callInnerFinally(innerFinally) {
    queueMicrotask(async () => {
      try {
        await innerFinally(this._innerFinallyArg);
      } catch (err) {
        console.error(
          "Unhandled promise rejection when running onFinished",
          innerFinally,
          err
        );
      }
    });
  }
  _runThen() {
    if (this._innerThen) {
      for (const innerThen of this._innerThen) {
        this.__callInnerThen(innerThen);
      }
      this._innerThen = void 0;
    }
  }
  _runCatch() {
    if (this._innerCatch) {
      for (const innerCatch of this._innerCatch) {
        this.__callInnerCatch(innerCatch);
      }
      this._innerCatch = void 0;
    }
  }
  __callInnerThen(innerThen) {
    queueMicrotask(async () => {
      try {
        await innerThen(this.value);
      } catch (err) {
        console.error(
          "Unhandled promise rejection when running onSuccess",
          innerThen,
          err
        );
      }
    });
  }
  __callInnerCatch(innerCatch) {
    queueMicrotask(async () => {
      try {
        await innerCatch(this.value);
      } catch (err) {
        console.error(
          "Unhandled promise rejection when running onError",
          innerCatch,
          err
        );
      }
    });
  }
}
const ALL_PROCESS_MAP = /* @__PURE__ */ new Map();
let acc_process_id = 0;
const allocProcessId = () => acc_process_id++;
const createProcess = async (env_script_url, metadata_json, env_json, fetch_port, name = new URL(env_script_url).hostname) => {
  const process_id = allocProcessId();
  const worker_url = URL.createObjectURL(
    new Blob(
      [
        `import("${env_script_url}").then(async({installEnv,Metadata})=>{
          void installEnv(new Metadata(${metadata_json},${env_json}));
          postMessage("ready")
        },(err)=>postMessage("ERROR:"+err))`
      ],
      {
        // esm 代码必须有正确的 mime
        type: "text/javascript"
      }
    )
  );
  const worker = new Worker(worker_url, {
    type: "module",
    name
  });
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
const createIpc = async (process_id, mmid, ipc_port) => {
  const process = _forceGetProcess(process_id);
  process.worker.postMessage(["ipc-connect", mmid], [ipc_port]);
  const connect_ready_po = new PromiseOut();
  const onEnvReady = (event) => {
    if (Array.isArray(event.data) && event.data[0] === "ipc-connect-ready" && event.data[1] === mmid) {
      connect_ready_po.resolve();
    }
  };
  process.worker.addEventListener("message", onEnvReady);
  await connect_ready_po.promise;
  process.worker.removeEventListener("message", onEnvReady);
  return;
};
const destroyProcess = (process_id) => {
  const process = _forceGetProcess(process_id);
  process.worker.terminate();
};
const on_create_process_signal = createSignal();
const APIS = {
  createProcess,
  runProcessMain,
  createIpc,
  destroyProcess
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
