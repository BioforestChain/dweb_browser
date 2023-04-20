import { d as defineComponent, r as ref, o as onMounted, c as createElementBlock, b as createBaseVNode, a as createVNode, F as Fragment, ay as CloseWatcher, g as openBlock } from "./index.f89085c2.js";
import { t as toConsole, _ as _sfc_main$1 } from "./LogPanel.fb338927.js";
var _imports_0 = "/assets/closewatcher.21ec84b6.svg";
const _hoisted_1 = { class: "card glass" };
const _hoisted_2 = { class: "card-body" };
const _hoisted_3 = /* @__PURE__ */ createBaseVNode("h2", { class: "card-title" }, "Close Watcher", -1);
const _hoisted_4 = /* @__PURE__ */ createBaseVNode("h3", { class: "text-lg font-bold" }, "Dialog", -1);
const _hoisted_5 = /* @__PURE__ */ createBaseVNode("p", { class: "py-4" }, "Hi", -1);
const _hoisted_6 = /* @__PURE__ */ createBaseVNode("div", { class: "divider" }, "LOG", -1);
const _sfc_main = /* @__PURE__ */ defineComponent({
  __name: "CloseWatcher",
  setup(__props) {
    const title = "Close Watcher";
    const $logPanel = ref();
    const $dialogEle = ref();
    let console;
    let dialogEle;
    onMounted(() => {
      console = toConsole($logPanel);
      dialogEle = $dialogEle.value;
    });
    const openDialog = () => {
      if (dialogEle.open) {
        return;
      }
      dialogEle.showModal();
      const closer = new CloseWatcher();
      closer.addEventListener("close", (event) => {
        console.log("CloseWatcher close", event.isTrusted, event.timeStamp);
        dialogEle.close();
      });
      dialogEle.onclose = (event) => {
        console.log("DialogEle close", event.isTrusted, event.timeStamp);
        closer.close();
      };
      dialogEle.oncancel = (event) => {
        console.log("DialogEle cancel", event.isTrusted, event.timeStamp);
        closer.close();
      };
    };
    const closeDialog = () => {
      dialogEle.close();
    };
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
            createBaseVNode("dialog", {
              ref_key: "$dialogEle",
              ref: $dialogEle
            }, [
              createBaseVNode("div", { class: "modal modal-open" }, [
                createBaseVNode("div", { class: "modal-box" }, [
                  _hoisted_4,
                  _hoisted_5,
                  createBaseVNode("div", { class: "modal-action" }, [
                    createBaseVNode("button", {
                      class: "btn",
                      onClick: closeDialog
                    }, "Yay!")
                  ])
                ])
              ])
            ], 512),
            createBaseVNode("button", {
              class: "inline-block rounded-full btn btn-accent",
              onClick: openDialog
            }, "Open Dialog")
          ])
        ]),
        _hoisted_6,
        createVNode(_sfc_main$1, {
          ref_key: "$logPanel",
          ref: $logPanel
        }, null, 512)
      ], 64);
    };
  }
});
export { _sfc_main as default };
