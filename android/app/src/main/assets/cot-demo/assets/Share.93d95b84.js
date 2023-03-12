import { F as FieldLabel } from "./FieldLabel.4b697f2e.js";
import { d as defineComponent, c as createElementBlock, a as createBaseVNode, b as createVNode, w as withCtx, o as openBlock } from "./index.f58eec1c.js";
var _imports_0 = "/assets/share.945dcf3d.svg";
const _hoisted_1 = { class: "card glass" };
const _hoisted_2 = { class: "card-body" };
const _hoisted_3 = /* @__PURE__ */ createBaseVNode("h2", { class: "card-title" }, "Share", -1);
const _hoisted_4 = /* @__PURE__ */ createBaseVNode("input", { type: "text" }, null, -1);
const _hoisted_5 = /* @__PURE__ */ createBaseVNode("input", { type: "text" }, null, -1);
const _hoisted_6 = /* @__PURE__ */ createBaseVNode("input", { type: "url" }, null, -1);
const _hoisted_7 = /* @__PURE__ */ createBaseVNode("input", { type: "file" }, null, -1);
const _hoisted_8 = /* @__PURE__ */ createBaseVNode("div", { class: "mockup-code text-xs min-w-max" }, [
  /* @__PURE__ */ createBaseVNode("code", null, "\u8FD9\u662F\u4F20\u8F93\u7684json\u914D\u7F6E")
], -1);
const _hoisted_9 = /* @__PURE__ */ createBaseVNode("div", { class: "card-actions justify-end btn-group" }, [
  /* @__PURE__ */ createBaseVNode("button", {
    class: "btn btn-accent inline-block rounded-full",
    id: "share-share"
  }, "Share")
], -1);
const _sfc_main = /* @__PURE__ */ defineComponent({
  __name: "Share",
  setup(__props) {
    const title = "Share";
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
          createVNode(FieldLabel, { label: "title:" }, {
            default: withCtx(() => [
              _hoisted_4
            ]),
            _: 1
          }),
          createVNode(FieldLabel, { label: "text:" }, {
            default: withCtx(() => [
              _hoisted_5
            ]),
            _: 1
          }),
          createVNode(FieldLabel, { label: "url:" }, {
            default: withCtx(() => [
              _hoisted_6
            ]),
            _: 1
          }),
          createVNode(FieldLabel, { label: "files:" }, {
            default: withCtx(() => [
              _hoisted_7
            ]),
            _: 1
          }),
          _hoisted_8,
          _hoisted_9
        ])
      ]);
    };
  }
});
export { _sfc_main as default };
