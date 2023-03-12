import { F as FieldLabel } from "./FieldLabel.4b697f2e.js";
import { d as defineComponent, c as createElementBlock, a as createBaseVNode, b as createVNode, w as withCtx, o as openBlock } from "./index.f58eec1c.js";
var _imports_0 = "/assets/splashscreen.361e86f1.svg";
const _hoisted_1 = { class: "card glass" };
const _hoisted_2 = { class: "card-body" };
const _hoisted_3 = /* @__PURE__ */ createBaseVNode("h2", { class: "card-title" }, "Splash Screen Show/Hide", -1);
const _hoisted_4 = /* @__PURE__ */ createBaseVNode("label", { class: "input-group" }, [
  /* @__PURE__ */ createBaseVNode("input", {
    type: "number",
    placeholder: "1000"
  }),
  /* @__PURE__ */ createBaseVNode("span", null, "ms")
], -1);
const _hoisted_5 = /* @__PURE__ */ createBaseVNode("div", { class: "card-actions justify-end btn-group" }, [
  /* @__PURE__ */ createBaseVNode("button", {
    class: "btn btn-accent rounded-full",
    id: "splashscreen-show"
  }, "Show")
], -1);
const _sfc_main = /* @__PURE__ */ defineComponent({
  __name: "SplashScreen",
  setup(__props) {
    const title = "Splash Screen";
    return (_ctx, _cache) => {
      return openBlock(), createElementBlock("div", _hoisted_1, [
        createBaseVNode("figure", { class: "icon" }, [
          createBaseVNode("img", {
            src: _imports_0,
            alt: title
          })
        ]),
        createBaseVNode("article", _hoisted_2, [
          _hoisted_3,
          createVNode(FieldLabel, { label: "Auto Hidden After:" }, {
            default: withCtx(() => [
              _hoisted_4
            ]),
            _: 1
          }),
          _hoisted_5
        ])
      ]);
    };
  }
});
export { _sfc_main as default };
