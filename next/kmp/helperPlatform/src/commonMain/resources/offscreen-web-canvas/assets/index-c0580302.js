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
  function getFetchOpts(link) {
    const fetchOpts = {};
    if (link.integrity)
      fetchOpts.integrity = link.integrity;
    if (link.referrerPolicy)
      fetchOpts.referrerPolicy = link.referrerPolicy;
    if (link.crossOrigin === "use-credentials")
      fetchOpts.credentials = "include";
    else if (link.crossOrigin === "anonymous")
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
const nativeWorkerCtor = Worker;
class MockMessagePort extends EventTarget {
  constructor() {
    super(...arguments);
    this.closed = false;
    this.beforeStart = [];
  }
  start() {
    const beforeStartArgs = this.beforeStart;
    if (beforeStartArgs) {
      this.beforeStart = void 0;
      for (const [data, transfer] of beforeStartArgs) {
        this.dispatchMessageEvent(data, transfer);
      }
      this.addEventListener = super.addEventListener.bind(this);
    }
  }
  postMessage(data, transfer) {
    if (this.closed) {
      return;
    }
    if (this.remote.beforeStart) {
      this.remote.beforeStart.push([data, transfer]);
      return;
    }
    this.remote.dispatchMessageEvent(data, transfer);
  }
  dispatchMessageEvent(data, transfer) {
    const ports = transfer?.filter((v) => v instanceof MessagePort) ?? [];
    this.dispatchEvent(
      new MessageEvent("message", {
        data,
        ports
      })
    );
  }
  addEventListener(type, callback, options) {
    super.addEventListener(type, callback, options);
    this.start();
  }
  close() {
    if (this.closed) {
      return;
    }
    this.closed = true;
    this.remote.close();
  }
}
class MokeWorker {
  constructor(workerUrl) {
    this.mockChannel = (() => {
      const port1 = new MockMessagePort();
      const port2 = new MockMessagePort();
      port1.remote = port2;
      port2.remote = port1;
      return [
        port1,
        port2
      ];
    })();
    this.toWorkerPort = this.mockChannel[0];
    this.toMainPort = this.mockChannel[1];
    this.postMessage = this.toWorkerPort.postMessage.bind(this.toWorkerPort);
    this.addEventListener = this.toWorkerPort.addEventListener.bind(this.toWorkerPort);
    const randomId = (Date.now() + Math.random()).toString(36);
    const script = this.script = document.createElement("script");
    debugger;
    script.dataset.workerId = randomId;
    script.type = "module";
    script.async = true;
    document.head.appendChild(script);
    script.src = workerUrl;
    Object.assign(window, { Worker: nativeWorkerCtor, [randomId]: this });
  }
}
function prepareInMain(canvas2) {
  if (typeof canvas2.transferControlToOffscreen !== "function") {
    const offscreencanvas = canvas2;
    offscreencanvas.convertToBlob = (options) => {
      return new Promise((resolve, reject) => {
        canvas2.toBlob(
          (blob2) => {
            if (blob2) {
              resolve(blob2);
            } else {
              reject();
            }
          },
          options?.type,
          options?.quality
        );
      });
    };
    let isoffscreen = false;
    canvas2.transferControlToOffscreen = () => {
      if (isoffscreen) {
        return offscreencanvas;
      }
      isoffscreen = true;
      const offscreenContainer = document.createElement("div");
      offscreenContainer.appendChild(canvas2);
      document.body.appendChild(offscreenContainer);
      offscreenContainer.style.pointerEvents = "none";
      offscreenContainer.style.position = "absolute";
      offscreenContainer.style.left = "0";
      offscreenContainer.style.top = "0";
      offscreenContainer.style.width = "0";
      offscreenContainer.style.height = "0";
      offscreenContainer.style.opacity = "0";
      delete canvas2.transferControlToOffscreen;
      return offscreencanvas;
    };
    Object.assign(window, {
      Worker: MokeWorker
    });
  }
}
const encodedJs = "ZnVuY3Rpb24gcHJlcGFyZUluV29ya2VyKHNyYykgewogIGlmICh0eXBlb2Ygd2luZG93ICE9PSAib2JqZWN0IikgewogICAgcmV0dXJuIHsKICAgICAgdG9NYWluQ2hhbm5lbDogc2VsZiwKICAgICAgaXNXb3JrZXI6IHRydWUKICAgIH07CiAgfSBlbHNlIHsKICAgIGNvbnN0IGN1cnJlbnRTY3JpcHQgPSBkb2N1bWVudC5jdXJyZW50U2NyaXB0ID8/IFtdLnNsaWNlLmNhbGwoZG9jdW1lbnQucXVlcnlTZWxlY3RvckFsbCgiW2RhdGEtd29ya2VyLWlkXSIpKS5maW5kKChzY3JpcHRFbGUpID0+IHNjcmlwdEVsZS5zcmMgPT0gc3JjKTsKICAgIGNvbnN0IHJhbmRvbUlkID0gY3VycmVudFNjcmlwdC5kYXRhc2V0LndvcmtlcklkOwogICAgZGVsZXRlIGN1cnJlbnRTY3JpcHQuZGF0YXNldC53b3JrZXJJZDsKICAgIGNvbnN0IG1va2VXb3JrZXIgPSB3aW5kb3dbcmFuZG9tSWRdOwogICAgZGVsZXRlIHdpbmRvd1tyYW5kb21JZF07CiAgICByZXR1cm4gewogICAgICB0b01haW5DaGFubmVsOiBtb2tlV29ya2VyLnRvTWFpblBvcnQsCiAgICAgIGlzV29ya2VyOiBmYWxzZQogICAgfTsKICB9Cn0KY29uc3QgeyB0b01haW5DaGFubmVsLCBpc1dvcmtlciB9ID0gcHJlcGFyZUluV29ya2VyKGltcG9ydC5tZXRhLnVybCk7Cihhc3luYyAoKSA9PiB7CiAgbGV0IHdzVXJsID0gbnVsbDsKICBjb25zdCBjYW52YXMgPSBhd2FpdCBuZXcgUHJvbWlzZSgKICAgIChyZXNvbHZlKSA9PiB0b01haW5DaGFubmVsLmFkZEV2ZW50TGlzdGVuZXIoCiAgICAgICJtZXNzYWdlIiwKICAgICAgKGV2ZW50KSA9PiB7CiAgICAgICAgcmVzb2x2ZShldmVudC5kYXRhLmNhbnZhcyk7CiAgICAgICAgd3NVcmwgPSBldmVudC5kYXRhLndzVXJsIHx8IG51bGw7CiAgICAgIH0sCiAgICAgIHsgb25jZTogdHJ1ZSB9CiAgICApCiAgKTsKICBjb25zdCBjdHggPSBjYW52YXMuZ2V0Q29udGV4dCgiMmQiKTsKICBjb25zdCBmZXRjaEltYWdlQml0bWFwID0gYXN5bmMgKGltYWdlVXJsLCBmZXRjaE9wdGlvbnMsIGltYWdlT3B0aW9ucykgPT4gewogICAgY29uc3QgaW1nYmxvYiA9IGF3YWl0IGZldGNoKGltYWdlVXJsLCBmZXRjaE9wdGlvbnMpLnRoZW4oCiAgICAgIChyZXMpID0+IHJlcy5ibG9iKCkKICAgICk7CiAgICBjb25zdCBpbWcgPSBhd2FpdCBjcmVhdGVJbWFnZUJpdG1hcChpbWdibG9iLCBpbWFnZU9wdGlvbnMpOwogICAgcmV0dXJuIGltZzsKICB9OwogIGNvbnN0IGNhbnZhc1RvRGF0YVVSTCA9IGFzeW5jIChjYW52YXMyLCBvcHRpb25zKSA9PiB7CiAgICBpZiAoInRvRGF0YVVSTCIgaW4gY2FudmFzMikgewogICAgICByZXR1cm4gYXdhaXQgY2FudmFzMi50b0RhdGFVUkwob3B0aW9ucz8udHlwZSwgb3B0aW9ucz8ucXVhbGl0eSk7CiAgICB9IGVsc2UgewogICAgICByZXR1cm4gYXdhaXQgYmxvYlRvRGF0YVVSTChhd2FpdCBjYW52YXMyLmNvbnZlcnRUb0Jsb2Iob3B0aW9ucykpOwogICAgfQogIH07CiAgY29uc3QgYmxvYlRvRGF0YVVSTCA9IChibG9iKSA9PiBuZXcgUHJvbWlzZSgocmVzb2x2ZSwgcmVqZWN0KSA9PiB7CiAgICBjb25zdCByZWFkZXIgPSBuZXcgRmlsZVJlYWRlcigpOwogICAgcmVhZGVyLm9ubG9hZCA9ICgpID0+IHJlc29sdmUocmVhZGVyLnJlc3VsdCk7CiAgICByZWFkZXIub25lcnJvciA9IHJlamVjdDsKICAgIHJlYWRlci5yZWFkQXNEYXRhVVJMKGJsb2IpOwogIH0pOwogIGNvbnN0IEFzeW5jRnVuY3Rpb24gPSBldmFsKAogICAgYChhc3luYygpPT57fSkuY29uc3RydWN0b3JgCiAgKTsKICBjb25zdCBldmFsQ29kZSA9IGFzeW5jIChyZXR1cm5Db25maWcsIGNvZGUsIGNiKSA9PiB7CiAgICB0cnkgewogICAgICBjb25zdCByZXMgPSBhd2FpdCBuZXcgQXN5bmNGdW5jdGlvbigKICAgICAgICAiY2FudmFzLGN0eCxmZXRjaEltYWdlQml0bWFwLGJsb2JUb0RhdGFVUkwsY2FudmFzVG9EYXRhVVJMIiwKICAgICAgICBjb2RlCiAgICAgICkoY2FudmFzLCBjdHgsIGZldGNoSW1hZ2VCaXRtYXAsIGJsb2JUb0RhdGFVUkwsIGNhbnZhc1RvRGF0YVVSTCk7CiAgICAgIGNiKAogICAgICAgIHZvaWQgMCwKICAgICAgICByZXR1cm5Db25maWcudm9pZCA/IHZvaWQgMCA6IHJldHVybkNvbmZpZy5qc29uaWZ5ID8gSlNPTi5zdHJpbmdpZnkocmVzKSA6IFN0cmluZyhyZXMpCiAgICAgICk7CiAgICB9IGNhdGNoIChlcnIpIHsKICAgICAgY29uc29sZS5lcnJvcihlcnIpOwogICAgICBjYihTdHJpbmcoZXJyKSwgdm9pZCAwKTsKICAgIH0KICB9OwogIE9iamVjdC5hc3NpZ24oc2VsZiwgewogICAgZXZhbENvZGUsCiAgICBjYW52YXMsCiAgICBjdHgsCiAgICBmZXRjaEltYWdlQml0bWFwLAogICAgYmxvYlRvRGF0YVVSTCwKICAgIGNhbnZhc1RvRGF0YVVSTAogIH0pOwogIGlmICh3c1VybCkgewogICAgY29uc3Qgd3MgPSBuZXcgV2ViU29ja2V0KHdzVXJsKTsKICAgIHdzLm9ubWVzc2FnZSA9IGFzeW5jIChldikgPT4gewogICAgICBjb25zb2xlLmxvZygiZXYuZGF0YToiLCBldik7CiAgICAgIGNvbnN0IHJlcSA9IEpTT04ucGFyc2UoZXYuZGF0YSk7CiAgICAgIGF3YWl0IGV2YWxDb2RlKAogICAgICAgIHsgdm9pZDogcmVxLnJlc3VsdFZvaWQsIGpzb25pZnk6IHJlcS5yZXN1bHRKc29uSWZ5IH0sCiAgICAgICAgcmVxLnJ1bkNvZGUsCiAgICAgICAgKGVycm9yLCBzdWNjZXNzKSA9PiB7CiAgICAgICAgICB3cy5zZW5kKAogICAgICAgICAgICBKU09OLnN0cmluZ2lmeSh7CiAgICAgICAgICAgICAgcmlkOiByZXEucmlkLAogICAgICAgICAgICAgIGVycm9yLAogICAgICAgICAgICAgIHN1Y2Nlc3MKICAgICAgICAgICAgfSkKICAgICAgICAgICk7CiAgICAgICAgfQogICAgICApOwogICAgfTsKICB9IGVsc2UgewogICAgY29uc29sZS5sb2coInN0YXJ0IHRlc3RzIik7CiAgICBjb25zdCB0ZXN0UmVzaXplID0gYXN5bmMgKCkgPT4gewogICAgICBjYW52YXMud2lkdGggPSAzMDA7CiAgICAgIGNhbnZhcy5oZWlnaHQgPSAzMDA7CiAgICAgIHJldHVybiB7CiAgICAgICAgd2lkdGg6IGNhbnZhcy53aWR0aCwKICAgICAgICBoZWlnaHQ6IGNhbnZhcy5oZWlnaHQKICAgICAgfTsKICAgIH07CiAgICBjb25zdCB0ZXN0RHJhd0ltYWdlID0gYXN5bmMgKCkgPT4gewogICAgICBjb25zdCBpbWcgPSBhd2FpdCBmZXRjaEltYWdlQml0bWFwKAogICAgICAgICJodHRwczovL3VwbG9hZC53aWtpbWVkaWEub3JnL3dpa2lwZWRpYS9jb21tb25zLzUvNTUvSm9obl9XaWxsaWFtX1dhdGVyaG91c2VfQV9NZXJtYWlkLmpwZyIKICAgICAgKTsKICAgICAgY3R4LmRyYXdJbWFnZShpbWcsIDAsIDApOwogICAgICByZXR1cm4gYXdhaXQgY2FudmFzVG9EYXRhVVJMKGNhbnZhcyk7CiAgICB9OwogICAgZm9yIChjb25zdCBbdGVzdCwgY29uZmlnXSBvZiAvKiBAX19QVVJFX18gKi8gbmV3IE1hcChbCiAgICAgIFsKICAgICAgICB0ZXN0UmVzaXplLAogICAgICAgIHsKICAgICAgICAgIHZvaWQ6IGZhbHNlLAogICAgICAgICAganNvbmlmeTogdHJ1ZQogICAgICAgIH0KICAgICAgXSwKICAgICAgWwogICAgICAgIHRlc3REcmF3SW1hZ2UsCiAgICAgICAgewogICAgICAgICAgdm9pZDogZmFsc2UsCiAgICAgICAgICBqc29uaWZ5OiBmYWxzZQogICAgICAgIH0KICAgICAgXQogICAgXSkpIHsKICAgICAgYXdhaXQgZXZhbENvZGUoCiAgICAgICAgY29uZmlnLAogICAgICAgIHRlc3QudG9TdHJpbmcoKS5tYXRjaCgvXHsoW1x3XFddKylcfS8pWzFdLAogICAgICAgIChlcnIsIHJlc3VsdCkgPT4gZXJyID8gY29uc29sZS5lcnJvcih0ZXN0Lm5hbWUsIGVycikgOiBjb25zb2xlLmxvZyh0ZXN0Lm5hbWUsIHJlc3VsdCkKICAgICAgKTsKICAgIH0KICB9Cn0pKCkuY2F0Y2goY29uc29sZS5lcnJvcik7Cg==";
const blob = typeof window !== "undefined" && window.Blob && new Blob(["URL.revokeObjectURL(import.meta.url);" + atob(encodedJs)], { type: "text/javascript;charset=utf-8" });
function WorkerWrapper(options) {
  let objURL;
  try {
    objURL = blob && (window.URL || window.webkitURL).createObjectURL(blob);
    if (!objURL)
      throw "";
    return new Worker(objURL, {
      type: "module",
      name: options?.name
    });
  } catch (e) {
    return new Worker(
      "data:application/javascript;base64," + encodedJs,
      {
        type: "module",
        name: options?.name
      }
    );
  }
}
const canvas = document.createElement("canvas");
prepareInMain(canvas);
const worker = new WorkerWrapper();
const offscreen = canvas.transferControlToOffscreen();
worker.postMessage(
  {
    canvas: offscreen,
    width: canvas.clientWidth,
    height: canvas.clientHeight,
    wsUrl: new URLSearchParams(location.search).get("channel")
  },
  [offscreen]
);
