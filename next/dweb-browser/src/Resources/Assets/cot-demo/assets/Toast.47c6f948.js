import { d as defineComponent, r as ref, o as onMounted, c as createElementBlock, a as createVNode, b as createBaseVNode, h as withCtx, u as unref, F as Fragment, g as openBlock, w as withDirectives, at as vModelText, v as vModelSelect, f as resolveComponent } from "./index.f89085c2.js";
import { _ as _imports_0 } from "./toast.7a5f524e.js";
import { F as FieldLabel } from "./FieldLabel.e4aa2d01.js";
import { t as toConsole, _ as _sfc_main$1, d as defineLogAction } from "./LogPanel.fb338927.js";
const _hoisted_1 = { class: "card glass" };
const _hoisted_2 = { class: "card-body" };
const _hoisted_3 = /* @__PURE__ */ createBaseVNode("h2", { class: "card-title" }, "Show Toast", -1);
const _hoisted_4 = /* @__PURE__ */ createBaseVNode("option", { value: "long" }, "long", -1);
const _hoisted_5 = /* @__PURE__ */ createBaseVNode("option", { value: "short" }, "short", -1);
const _hoisted_6 = [
  _hoisted_4,
  _hoisted_5
];
const _hoisted_7 = { class: "justify-end card-actions" };
const _hoisted_8 = /* @__PURE__ */ createBaseVNode("div", { class: "divider" }, "LOG", -1);
const _sfc_main = /* @__PURE__ */ defineComponent({
  __name: "Toast",
  setup(__props) {
    const title = "Toast";
    const $logPanel = ref();
    const $toastPlugin = ref();
    let toast;
    onMounted(() => {
      toConsole($logPanel);
      toast = $toastPlugin.value;
    });
    const toast_message = ref("\u6211\u662Ftoast\u{1F353}");
    const toast_duration = ref("short");
    const showToast = defineLogAction(
      async () => {
        return toast.show({ text: toast_message.value, duration: toast_duration.value });
      },
      { name: "showToast", args: [toast_message, toast_duration], logPanel: $logPanel }
    );
    return (_ctx, _cache) => {
      const _component_dweb_toast = resolveComponent("dweb-toast");
      return openBlock(), createElementBlock(Fragment, null, [
        createVNode(_component_dweb_toast, {
          ref_key: "$toastPlugin",
          ref: $toastPlugin
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
            createVNode(FieldLabel, { label: "Toast Message:" }, {
              default: withCtx(() => [
                withDirectives(createBaseVNode("input", {
                  type: "text",
                  id: "toast-message",
                  "onUpdate:modelValue": _cache[0] || (_cache[0] = ($event) => toast_message.value = $event)
                }, null, 512), [
                  [vModelText, toast_message.value]
                ])
              ]),
              _: 1
            }),
            createVNode(FieldLabel, { label: "Toast Duration:" }, {
              default: withCtx(() => [
                withDirectives(createBaseVNode("select", {
                  name: "toast-duration",
                  id: "toast-duration",
                  "onUpdate:modelValue": _cache[1] || (_cache[1] = ($event) => toast_duration.value = $event)
                }, _hoisted_6, 512), [
                  [vModelSelect, toast_duration.value]
                ])
              ]),
              _: 1
            }),
            createBaseVNode("div", _hoisted_7, [
              createBaseVNode("button", {
                class: "inline-block rounded-full btn btn-accent",
                id: "toast-show",
                onClick: _cache[2] || (_cache[2] = ($event) => unref(showToast)())
              }, "Show")
            ])
          ])
        ]),
        _hoisted_8,
        createVNode(_sfc_main$1, {
          ref_key: "$logPanel",
          ref: $logPanel
        }, null, 512)
      ], 64);
    };
  }
});
export { _sfc_main as default };
