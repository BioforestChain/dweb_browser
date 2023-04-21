import { d as defineComponent, r as ref, o as onMounted, c as createElementBlock, a as createVNode, b as createBaseVNode, h as withCtx, u as unref, w as withDirectives, v as vModelSelect, F as Fragment, g as openBlock, f as resolveComponent, ax as barcodeScannerPlugin } from "./index.f89085c2.js";
import { _ as _imports_0 } from "./vibrate.d24dcb2b.js";
import { F as FieldLabel } from "./FieldLabel.e4aa2d01.js";
import { t as toConsole, _ as _sfc_main$1, d as defineLogAction } from "./LogPanel.fb338927.js";
const _hoisted_1 = { class: "card glass" };
const _hoisted_2 = { class: "card-body" };
const _hoisted_3 = /* @__PURE__ */ createBaseVNode("h2", { class: "card-title" }, "Scanner", -1);
const _hoisted_4 = { class: "card-body" };
const _hoisted_5 = /* @__PURE__ */ createBaseVNode("h2", { class: "card-title" }, "get Photo", -1);
const _hoisted_6 = /* @__PURE__ */ createBaseVNode("option", { value: "PROMPT" }, "PROMPT", -1);
const _hoisted_7 = /* @__PURE__ */ createBaseVNode("option", { value: "CAMERA" }, "CAMERA", -1);
const _hoisted_8 = /* @__PURE__ */ createBaseVNode("option", { value: "PHOTOS" }, "PHOTOS", -1);
const _hoisted_9 = [
  _hoisted_6,
  _hoisted_7,
  _hoisted_8
];
const _hoisted_10 = { class: "justify-end card-actions" };
const _hoisted_11 = /* @__PURE__ */ createBaseVNode("div", { class: "divider" }, "LOG", -1);
const _sfc_main = /* @__PURE__ */ defineComponent({
  __name: "BarcodeScanning",
  setup(__props) {
    const title = "Scanner";
    const $logPanel = ref();
    const $barcodeScannerPlugin = ref();
    let console;
    let scanner = barcodeScannerPlugin;
    let barcodeScanner;
    onMounted(() => {
      console = toConsole($logPanel);
      barcodeScanner = $barcodeScannerPlugin.value;
    });
    const result = ref();
    const onFileChanged = defineLogAction(async ($event) => {
      var _a;
      const target = $event.target;
      if (target && ((_a = target.files) == null ? void 0 : _a[0])) {
        const img = target.files[0];
        console.info("photo ==> ", img.name, img.type, img.size);
        result.value = await scanner.process(img).then((res) => res.text());
      }
    }, { name: "process", args: [result], logPanel: $logPanel });
    const onStop = defineLogAction(async () => {
      await scanner.stop();
    }, { name: "onStop", args: [], logPanel: $logPanel });
    const taskPhoto = defineLogAction(async () => {
      result.value = await barcodeScanner.startScanning();
    }, { name: "taskPhoto", args: [result], logPanel: $logPanel });
    const cameraSource = ref("PHOTOS");
    const getPhoto = defineLogAction(async () => {
      result.value = await barcodeScanner.getPhoto({ source: cameraSource.value });
    }, { name: "getPhoto", args: [result], logPanel: $logPanel });
    return (_ctx, _cache) => {
      const _component_dweb_barcode_scanning = resolveComponent("dweb-barcode-scanning");
      return openBlock(), createElementBlock(Fragment, null, [
        createVNode(_component_dweb_barcode_scanning, {
          ref_key: "$barcodeScannerPlugin",
          ref: $barcodeScannerPlugin
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
            createVNode(FieldLabel, { label: "Vibrate Pattern:" }, {
              default: withCtx(() => [
                createBaseVNode("input", {
                  type: "file",
                  onChange: _cache[0] || (_cache[0] = ($event) => unref(onFileChanged)($event)),
                  accept: "image/*",
                  capture: ""
                }, null, 32)
              ]),
              _: 1
            }),
            createBaseVNode("button", {
              class: "inline-block rounded-full btn btn-accent",
              onClick: _cache[1] || (_cache[1] = (...args) => unref(taskPhoto) && unref(taskPhoto)(...args))
            }, "scanner"),
            createBaseVNode("button", {
              class: "inline-block rounded-full btn btn-accent",
              onClick: _cache[2] || (_cache[2] = (...args) => unref(onStop) && unref(onStop)(...args))
            }, "stop")
          ]),
          createBaseVNode("article", _hoisted_4, [
            _hoisted_5,
            withDirectives(createBaseVNode("select", {
              class: "w-full max-w-xs select",
              "onUpdate:modelValue": _cache[3] || (_cache[3] = ($event) => cameraSource.value = $event)
            }, _hoisted_9, 512), [
              [vModelSelect, cameraSource.value]
            ]),
            createBaseVNode("div", _hoisted_10, [
              createBaseVNode("button", {
                class: "inline-block rounded-full btn btn-accent",
                onClick: _cache[4] || (_cache[4] = (...args) => unref(getPhoto) && unref(getPhoto)(...args))
              }, "getPhoto")
            ])
          ])
        ]),
        _hoisted_11,
        createVNode(_sfc_main$1, {
          ref_key: "$logPanel",
          ref: $logPanel
        }, null, 512)
      ], 64);
    };
  }
});
export { _sfc_main as default };
