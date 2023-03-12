import { F as FieldLabel } from "./FieldLabel.4b697f2e.js";
import { d as defineComponent, c as createElementBlock, a as createBaseVNode, b as createVNode, w as withCtx, o as openBlock } from "./index.f58eec1c.js";
var _imports_0 = "/assets/vibrate.c9e805b1.svg";
const _hoisted_1 = { class: "card glass" };
const _hoisted_2 = { class: "card-body" };
const _hoisted_3 = /* @__PURE__ */ createBaseVNode("h2", { class: "card-title" }, "Haptics Vibrate", -1);
const _hoisted_4 = /* @__PURE__ */ createBaseVNode("input", {
  type: "text",
  id: "haptics-vibrate-pattern",
  placeholder: "1,20,1,30"
}, null, -1);
const _hoisted_5 = /* @__PURE__ */ createBaseVNode("button", {
  class: "btn btn-accent inline-block rounded-full",
  id: "haptics-vibrate"
}, "Vibrate", -1);
const _sfc_main = /* @__PURE__ */ defineComponent({
  __name: "Haptics",
  setup(__props) {
    const title = "Haptics";
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
          createVNode(FieldLabel, { label: "Vibrate Pattern:" }, {
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
