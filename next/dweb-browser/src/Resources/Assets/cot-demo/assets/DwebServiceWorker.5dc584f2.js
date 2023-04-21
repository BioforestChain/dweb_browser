import { _ as _imports_0 } from "./splashscreen.ecee76e9.js";
import { d as defineComponent, r as ref, o as onMounted, az as dwebServiceWorker, c as createElementBlock, b as createBaseVNode, u as unref, av as toDisplayString, a as createVNode, F as Fragment, g as openBlock } from "./index.f89085c2.js";
import { t as toConsole, _ as _sfc_main$1, d as defineLogAction } from "./LogPanel.fb338927.js";
const _hoisted_1 = { class: "card glass" };
const _hoisted_2 = { class: "card-body" };
const _hoisted_3 = /* @__PURE__ */ createBaseVNode("h2", { class: "card-title" }, "\u4E0B\u8F7D\u6D4B\u8BD5", -1);
const _hoisted_4 = { class: "justify-end card-actions" };
const _hoisted_5 = ["value"];
const _hoisted_6 = { class: "stat" };
const _hoisted_7 = /* @__PURE__ */ createBaseVNode("div", { class: "stat-figure text-secondary" }, [
  /* @__PURE__ */ createBaseVNode("div", { class: "avatar online" }, [
    /* @__PURE__ */ createBaseVNode("div", { class: "w-16 rounded-full" }, [
      /* @__PURE__ */ createBaseVNode("img", { src: "https://www.bfmeta.org/imgs/logo_1000.webp" })
    ])
  ])
], -1);
const _hoisted_8 = { class: "stat-value" };
const _hoisted_9 = /* @__PURE__ */ createBaseVNode("div", { class: "stat-title" }, "download Task runing", -1);
const _hoisted_10 = { class: "stat-desc text-secondary" };
const _hoisted_11 = { class: "card-body" };
const _hoisted_12 = /* @__PURE__ */ createBaseVNode("h2", { class: "card-title" }, "APP \u5173\u95ED\u3001\u91CD\u542F", -1);
const _hoisted_13 = { class: "justify-end card-actions btn-group" };
const _hoisted_14 = { class: "card-body" };
const _hoisted_15 = /* @__PURE__ */ createBaseVNode("h2", { class: "card-title" }, "\u4E0B\u8F7D\u63A7\u5236\u5668\uFF1A \u6682\u505C/\u91CD\u4E0B/\u53D6\u6D88", -1);
const _hoisted_16 = { class: "justify-end card-actions btn-group" };
const _hoisted_17 = /* @__PURE__ */ createBaseVNode("div", { class: "divider" }, "LOG", -1);
const _sfc_main = /* @__PURE__ */ defineComponent({
  __name: "DwebServiceWorker",
  setup(__props) {
    const $logPanel = ref();
    let console;
    const progress = ref(0);
    onMounted(async () => {
      console = toConsole($logPanel);
      dwebServiceWorker.addEventListener("updatefound", (event) => {
        console.log("Dweb Service Worker update found!", event);
      });
      dwebServiceWorker.addEventListener("fetch", async (event) => {
        console.log("Dweb Service Worker fetch!", event.clientId);
        const response = await fetch(event.request);
        console.log("Dweb Service Worker fetch response=>", response);
        return event.respondWith(response);
      });
      dwebServiceWorker.addEventListener("onFetch", (event) => {
        console.log("Dweb Service Worker onFetch!", event);
      });
      const updateContoller = dwebServiceWorker.update;
      updateContoller.addEventListener("start", (event) => {
        console.log("Dweb Service Worker updateContoller start =>", event);
      });
      updateContoller.addEventListener("end", (event) => {
        console.log("Dweb Service Worker updateContoller end =>", event);
      });
      updateContoller.addEventListener("progress", (progressRate) => {
        progress.value = parseFloat(progressRate);
        console.log("Dweb Service Worker updateContoller progress =>", progressRate, parseFloat(progressRate));
      });
      updateContoller.addEventListener("cancel", (event) => {
        console.log("Dweb Service Worker updateContoller cancel =>", event);
      });
    });
    const close = defineLogAction(async () => {
      return await dwebServiceWorker.close();
    }, { name: "close", args: [], logPanel: $logPanel });
    const restart = defineLogAction(async () => {
      return await dwebServiceWorker.restart();
    }, { name: "restart", args: [], logPanel: $logPanel });
    const pause = defineLogAction(async () => {
      return await dwebServiceWorker.updateContoller.pause();
    }, { name: "pause", args: [], logPanel: $logPanel });
    const resume = defineLogAction(async () => {
      return await dwebServiceWorker.updateContoller.resume();
    }, { name: "resume", args: [], logPanel: $logPanel });
    const cancel = defineLogAction(async () => {
      return await dwebServiceWorker.updateContoller.cancel();
    }, { name: "cancel", args: [], logPanel: $logPanel });
    const download = defineLogAction(async () => {
      return await dwebServiceWorker.updateContoller.download("https://shop.plaoc.com/bfs-metadata.json");
    }, { name: "cancel", args: [], logPanel: $logPanel });
    const title = "Dweb Service Worker";
    return (_ctx, _cache) => {
      return openBlock(), createElementBlock(Fragment, null, [
        createBaseVNode("div", _hoisted_1, [
          createBaseVNode("figure", { class: "icon" }, [
            createBaseVNode("img", {
              src: _imports_0,
              alt: title
            })
          ]),
          createBaseVNode("article", _hoisted_2, [
            _hoisted_3,
            createBaseVNode("div", _hoisted_4, [
              createBaseVNode("button", {
                class: "inline-block rounded-full btn btn-accent",
                onClick: _cache[0] || (_cache[0] = (...args) => unref(download) && unref(download)(...args))
              }, "\u4E0B\u8F7D\u65B0\u7248\u672C")
            ]),
            createBaseVNode("div", null, [
              createBaseVNode("progress", {
                class: "w-56 progress progress-accent",
                value: progress.value,
                max: "100"
              }, null, 8, _hoisted_5),
              createBaseVNode("div", _hoisted_6, [
                _hoisted_7,
                createBaseVNode("div", _hoisted_8, toDisplayString(progress.value) + "%", 1),
                _hoisted_9,
                createBaseVNode("div", _hoisted_10, toDisplayString(100 - progress.value) + " tasks remaining", 1)
              ])
            ])
          ]),
          createBaseVNode("article", _hoisted_11, [
            _hoisted_12,
            createBaseVNode("div", _hoisted_13, [
              createBaseVNode("button", {
                class: "inline-block rounded-full btn btn-accent",
                onClick: _cache[1] || (_cache[1] = (...args) => unref(close) && unref(close)(...args))
              }, "close"),
              createBaseVNode("button", {
                class: "inline-block rounded-full btn btn-accent",
                onClick: _cache[2] || (_cache[2] = (...args) => unref(restart) && unref(restart)(...args))
              }, "restart")
            ])
          ]),
          createBaseVNode("article", _hoisted_14, [
            _hoisted_15,
            createBaseVNode("div", _hoisted_16, [
              createBaseVNode("button", {
                class: "inline-block rounded-full btn btn-accent",
                onClick: _cache[3] || (_cache[3] = (...args) => unref(pause) && unref(pause)(...args))
              }, "pause"),
              createBaseVNode("button", {
                class: "inline-block rounded-full btn btn-accent",
                onClick: _cache[4] || (_cache[4] = (...args) => unref(resume) && unref(resume)(...args))
              }, "resume"),
              createBaseVNode("button", {
                class: "inline-block rounded-full btn btn-accent",
                onClick: _cache[5] || (_cache[5] = (...args) => unref(cancel) && unref(cancel)(...args))
              }, "cancel")
            ])
          ])
        ]),
        _hoisted_17,
        createVNode(_sfc_main$1, {
          ref_key: "$logPanel",
          ref: $logPanel
        }, null, 512)
      ], 64);
    };
  }
});
export { _sfc_main as default };
