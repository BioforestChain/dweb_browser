import { d as defineComponent, r as ref, Q as reactive, o as onMounted, c as createElementBlock, a as createVNode, b as createBaseVNode, h as withCtx, u as unref, F as Fragment, g as openBlock, w as withDirectives, at as vModelText, f as resolveComponent } from "./index.f89085c2.js";
import { F as FieldLabel } from "./FieldLabel.e4aa2d01.js";
import { t as toConsole, _ as _sfc_main$1, d as defineLogAction } from "./LogPanel.fb338927.js";
var _imports_0 = "/assets/share.945dcf3d.svg";
const _hoisted_1 = { class: "card glass" };
const _hoisted_2 = { class: "card-body" };
const _hoisted_3 = /* @__PURE__ */ createBaseVNode("h2", { class: "card-title" }, "Share", -1);
const _hoisted_4 = /* @__PURE__ */ createBaseVNode("div", { class: "text-xs mockup-code min-w-max" }, [
  /* @__PURE__ */ createBaseVNode("code", null, "\u8FD9\u662F\u4F20\u8F93\u7684json\u914D\u7F6E")
], -1);
const _hoisted_5 = { class: "justify-end card-actions btn-group" };
const _hoisted_6 = /* @__PURE__ */ createBaseVNode("div", { class: "divider" }, "LOG", -1);
const _sfc_main = /* @__PURE__ */ defineComponent({
  __name: "Share",
  setup(__props) {
    const title = "Share";
    const $logPanel = ref();
    const $sharePlugin = ref();
    let console;
    let share;
    const shareData = reactive({
      dialogTitle: "\u6211\u662FdialogTitle",
      title: "\u5206\u4EAB\u6807\u9898\u{1F349}",
      text: "\u5206\u4EAB\u6587\u5B57\u5206\u4EAB\u6587\u5B57",
      url: "https://gpt.waterbang.top",
      files: null
    });
    onMounted(() => {
      console = toConsole($logPanel);
      share = $sharePlugin.value;
    });
    const shareHandle = defineLogAction(async () => {
      const result = await share.share(shareData);
      console.info("shareHandle=>", result);
    }, { name: "shareHandle", args: [], logPanel: $logPanel });
    const fileChange = ($event) => {
      var _a;
      const target = $event.target;
      if (target && ((_a = target.files) == null ? void 0 : _a[0])) {
        console.log("event", $event);
        console.log("target.files=>", target.files[0]);
        shareData.files = target.files;
      }
    };
    return (_ctx, _cache) => {
      const _component_dweb_share = resolveComponent("dweb-share");
      return openBlock(), createElementBlock(Fragment, null, [
        createVNode(_component_dweb_share, {
          ref_key: "$sharePlugin",
          ref: $sharePlugin
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
            createVNode(FieldLabel, { label: "title:" }, {
              default: withCtx(() => [
                withDirectives(createBaseVNode("input", {
                  type: "text",
                  "onUpdate:modelValue": _cache[0] || (_cache[0] = ($event) => shareData.title = $event)
                }, null, 512), [
                  [vModelText, shareData.title]
                ])
              ]),
              _: 1
            }),
            createVNode(FieldLabel, { label: "text:" }, {
              default: withCtx(() => [
                withDirectives(createBaseVNode("input", {
                  type: "text",
                  "onUpdate:modelValue": _cache[1] || (_cache[1] = ($event) => shareData.text = $event)
                }, null, 512), [
                  [vModelText, shareData.text]
                ])
              ]),
              _: 1
            }),
            createVNode(FieldLabel, { label: "url:" }, {
              default: withCtx(() => [
                withDirectives(createBaseVNode("input", {
                  type: "url",
                  "onUpdate:modelValue": _cache[2] || (_cache[2] = ($event) => shareData.url = $event)
                }, null, 512), [
                  [vModelText, shareData.url]
                ])
              ]),
              _: 1
            }),
            createVNode(FieldLabel, { label: "files:" }, {
              default: withCtx(() => [
                createBaseVNode("input", {
                  type: "file",
                  onChange: _cache[3] || (_cache[3] = ($event) => fileChange($event))
                }, null, 32)
              ]),
              _: 1
            }),
            _hoisted_4,
            createBaseVNode("div", _hoisted_5, [
              createBaseVNode("button", {
                class: "inline-block rounded-full btn btn-accent",
                onClick: _cache[4] || (_cache[4] = (...args) => unref(shareHandle) && unref(shareHandle)(...args))
              }, "Share")
            ])
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
