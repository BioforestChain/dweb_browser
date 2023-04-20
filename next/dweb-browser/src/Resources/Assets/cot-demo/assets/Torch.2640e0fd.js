import { _ as _imports_0 } from "./toast.7a5f524e.js";
import { t as toConsole, _ as _sfc_main$1, d as defineLogAction } from "./LogPanel.fb338927.js";
import { d as defineComponent, r as ref, o as onMounted, c as createElementBlock, a as createVNode, b as createBaseVNode, u as unref, F as Fragment, g as openBlock, f as resolveComponent } from "./index.f89085c2.js";
const _hoisted_1 = { class: "card glass" };
const _hoisted_2 = { class: "card-body" };
const _hoisted_3 = /* @__PURE__ */ createBaseVNode("h2", { class: "card-title" }, "Torch", -1);
const _hoisted_4 = { class: "justify-end card-actions" };
const _hoisted_5 = /* @__PURE__ */ createBaseVNode("div", { class: "divider" }, "LOG", -1);
const _sfc_main = /* @__PURE__ */ defineComponent({
  __name: "Torch",
  setup(__props) {
    const title = "Toast";
    const $logPanel = ref();
    const $torchPlugin = ref();
    let console;
    let toast;
    onMounted(() => {
      console = toConsole($logPanel);
      toast = $torchPlugin.value;
    });
    const toggleTorch = defineLogAction(
      async () => {
        return toast.toggleTorch();
      },
      { name: "toggleTorch", args: [], logPanel: $logPanel }
    );
    const getState = defineLogAction(
      async () => {
        const result = await toast.getTorchState();
        console.info("torch state", result);
      },
      { name: "getState", args: [], logPanel: $logPanel }
    );
    return (_ctx, _cache) => {
      const _component_dweb_torch = resolveComponent("dweb-torch");
      return openBlock(), createElementBlock(Fragment, null, [
        createVNode(_component_dweb_torch, {
          ref_key: "$torchPlugin",
          ref: $torchPlugin
        }, null, 512),
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
                onClick: _cache[0] || (_cache[0] = (...args) => unref(toggleTorch) && unref(toggleTorch)(...args))
              }, "toggle"),
              createBaseVNode("button", {
                class: "inline-block rounded-full btn btn-accent",
                onClick: _cache[1] || (_cache[1] = (...args) => unref(getState) && unref(getState)(...args))
              }, "state")
            ])
          ])
        ]),
        _hoisted_5,
        createVNode(_sfc_main$1, {
          ref_key: "$logPanel",
          ref: $logPanel
        }, null, 512)
      ], 64);
    };
  }
});
export { _sfc_main as default };
