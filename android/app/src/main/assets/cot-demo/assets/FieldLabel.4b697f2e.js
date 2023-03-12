import { _ as _export_sfc, d as defineComponent, r as ref, o as openBlock, c as createElementBlock, a as createBaseVNode, t as toDisplayString, e as renderSlot } from "./index.f58eec1c.js";
var FieldLabel_vue_vue_type_style_index_0_scoped_true_lang = "";
const _hoisted_1 = { class: "form-control w-full max-w-xs" };
const _hoisted_2 = { class: "label" };
const _hoisted_3 = { class: "label-text" };
const _sfc_main = /* @__PURE__ */ defineComponent({
  __name: "FieldLabel",
  props: { label: String },
  setup(__props) {
    const props = __props;
    ref("");
    return (_ctx, _cache) => {
      return openBlock(), createElementBlock("div", _hoisted_1, [
        createBaseVNode("label", _hoisted_2, [
          createBaseVNode("span", _hoisted_3, toDisplayString(props.label), 1)
        ]),
        renderSlot(_ctx.$slots, "default", {}, void 0, true)
      ]);
    };
  }
});
var FieldLabel = /* @__PURE__ */ _export_sfc(_sfc_main, [["__scopeId", "data-v-745a6956"]]);
export { FieldLabel as F };
