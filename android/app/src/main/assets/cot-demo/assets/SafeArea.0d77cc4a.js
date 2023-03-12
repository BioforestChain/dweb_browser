import { d as defineComponent, c as createElementBlock, a as createBaseVNode, o as openBlock } from "./index.f58eec1c.js";
var _imports_0 = "/assets/safearea.91acab9d.svg";
const _hoisted_1 = { class: "card glass" };
const _hoisted_2 = /* @__PURE__ */ createBaseVNode("article", { class: "card-body" }, [
  /* @__PURE__ */ createBaseVNode("h2", { class: "card-title" }, "Get Safe Area Insets"),
  /* @__PURE__ */ createBaseVNode("div", { class: "card-actions justify-end btn-group" }, [
    /* @__PURE__ */ createBaseVNode("button", {
      class: "btn btn-accent rounded-full",
      id: "safearea-getSafeAreaInsets"
    }, "Get")
  ])
], -1);
const _sfc_main = /* @__PURE__ */ defineComponent({
  __name: "SafeArea",
  setup(__props) {
    const title = "Safe Area";
    return (_ctx, _cache) => {
      return openBlock(), createElementBlock("div", _hoisted_1, [
        createBaseVNode("figure", { class: "icon" }, [
          createBaseVNode("img", {
            src: _imports_0,
            alt: title
          })
        ]),
        _hoisted_2
      ]);
    };
  }
});
export { _sfc_main as default };
