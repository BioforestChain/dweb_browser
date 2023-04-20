import { d as defineComponent, r as ref, o as onMounted, c as createElementBlock, a as createVNode, b as createBaseVNode, h as withCtx, u as unref, F as Fragment, g as openBlock, w as withDirectives, v as vModelSelect, at as vModelText, f as resolveComponent } from "./index.f89085c2.js";
import { _ as _imports_0 } from "./vibrate.d24dcb2b.js";
import { F as FieldLabel } from "./FieldLabel.e4aa2d01.js";
import { t as toConsole, d as defineLogAction } from "./LogPanel.fb338927.js";
const _hoisted_1 = { class: "card glass" };
const _hoisted_2 = { class: "card-body" };
const _hoisted_3 = /* @__PURE__ */ createBaseVNode("h2", { class: "card-title" }, "\u89E6\u78B0\u8F7B\u8D28\u91CF\u7269\u4F53", -1);
const _hoisted_4 = /* @__PURE__ */ createBaseVNode("option", { value: "HEAVY" }, "HEAVY", -1);
const _hoisted_5 = /* @__PURE__ */ createBaseVNode("option", { value: "MEDIUM" }, "MEDIUM", -1);
const _hoisted_6 = /* @__PURE__ */ createBaseVNode("option", { value: "LIGHT" }, "LIGHT", -1);
const _hoisted_7 = [
  _hoisted_4,
  _hoisted_5,
  _hoisted_6
];
const _hoisted_8 = { class: "justify-end card-actions" };
const _hoisted_9 = { class: "card-body" };
const _hoisted_10 = /* @__PURE__ */ createBaseVNode("h2", { class: "card-title" }, "\u632F\u52A8\u901A\u77E5", -1);
const _hoisted_11 = /* @__PURE__ */ createBaseVNode("option", { value: "SUCCESS" }, "SUCCESS", -1);
const _hoisted_12 = /* @__PURE__ */ createBaseVNode("option", { value: "WARNING" }, "WARNING", -1);
const _hoisted_13 = /* @__PURE__ */ createBaseVNode("option", { value: "ERROR" }, "ERROR", -1);
const _hoisted_14 = [
  _hoisted_11,
  _hoisted_12,
  _hoisted_13
];
const _hoisted_15 = { class: "justify-end card-actions" };
const _hoisted_16 = { class: "card-body" };
const _hoisted_17 = /* @__PURE__ */ createBaseVNode("h2", { class: "card-title" }, "\u5355\u51FB\u624B\u52BF\u7684\u53CD\u9988\u632F\u52A8", -1);
const _hoisted_18 = { class: "card-body" };
const _hoisted_19 = /* @__PURE__ */ createBaseVNode("h2", { class: "card-title" }, "\u7981\u7528\u624B\u52BF\u7684\u53CD\u9988\u632F\u52A8", -1);
const _hoisted_20 = { class: "card-body" };
const _hoisted_21 = /* @__PURE__ */ createBaseVNode("h2", { class: "card-title" }, "\u53CC\u51FB\u624B\u52BF\u7684\u53CD\u9988\u632F\u52A8", -1);
const _hoisted_22 = { class: "card-body" };
const _hoisted_23 = /* @__PURE__ */ createBaseVNode("h2", { class: "card-title" }, "\u91CD\u51FB\u624B\u52BF\u7684\u53CD\u9988\u632F\u52A8, \u6BD4\u5982\u83DC\u5355\u952E/\u60E8\u6848/3Dtouch", -1);
const _hoisted_24 = { class: "card-body" };
const _hoisted_25 = /* @__PURE__ */ createBaseVNode("h2", { class: "card-title" }, "\u6EF4\u7B54", -1);
const _hoisted_26 = { class: "card-body" };
const _hoisted_27 = /* @__PURE__ */ createBaseVNode("h2", { class: "card-title" }, "Haptics Vibrate", -1);
const _sfc_main = /* @__PURE__ */ defineComponent({
  __name: "Haptics",
  setup(__props) {
    const title = "Haptics";
    const $logPanel = ref();
    const $hapticsPlugin = ref();
    let haptics;
    const vibrate = ref(300);
    onMounted(() => {
      toConsole($logPanel);
      haptics = $hapticsPlugin.value;
    });
    const impactStyle = ref("HEAVY");
    const impactLight = defineLogAction(
      async () => {
        haptics.impactLight({ style: impactStyle.value });
      },
      { name: "impactLight", args: [], logPanel: $logPanel }
    );
    const notificationStyle = ref("SUCCESS");
    const notification = defineLogAction(
      async () => {
        haptics.notification({ type: notificationStyle.value });
      },
      { name: "notification", args: [], logPanel: $logPanel }
    );
    const hapticsVibrate = defineLogAction(
      async () => {
        haptics.vibrate({ duration: vibrate.value });
      },
      { name: "hapticsVibrate", args: [vibrate], logPanel: $logPanel }
    );
    return (_ctx, _cache) => {
      const _component_dweb_haptics = resolveComponent("dweb-haptics");
      return openBlock(), createElementBlock(Fragment, null, [
        createVNode(_component_dweb_haptics, {
          ref_key: "$hapticsPlugin",
          ref: $hapticsPlugin
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
            createVNode(FieldLabel, { label: "Haptics Duration:" }, {
              default: withCtx(() => [
                withDirectives(createBaseVNode("select", {
                  name: "haptics-duration",
                  id: "haptics-duration",
                  "onUpdate:modelValue": _cache[0] || (_cache[0] = ($event) => impactStyle.value = $event)
                }, _hoisted_7, 512), [
                  [vModelSelect, impactStyle.value]
                ])
              ]),
              _: 1
            }),
            createBaseVNode("div", _hoisted_8, [
              createBaseVNode("button", {
                class: "inline-block rounded-full btn btn-accent",
                onClick: _cache[1] || (_cache[1] = (...args) => unref(impactLight) && unref(impactLight)(...args))
              }, "impactLight")
            ])
          ]),
          createBaseVNode("article", _hoisted_9, [
            _hoisted_10,
            createVNode(FieldLabel, { label: "Haptics Duration:" }, {
              default: withCtx(() => [
                withDirectives(createBaseVNode("select", {
                  name: "haptics-duration",
                  id: "haptics-duration",
                  "onUpdate:modelValue": _cache[2] || (_cache[2] = ($event) => notificationStyle.value = $event)
                }, _hoisted_14, 512), [
                  [vModelSelect, notificationStyle.value]
                ])
              ]),
              _: 1
            }),
            createBaseVNode("div", _hoisted_15, [
              createBaseVNode("button", {
                class: "inline-block rounded-full btn btn-accent",
                onClick: _cache[3] || (_cache[3] = (...args) => unref(notification) && unref(notification)(...args))
              }, "notification")
            ])
          ]),
          createBaseVNode("article", _hoisted_16, [
            _hoisted_17,
            createBaseVNode("button", {
              class: "inline-block rounded-full btn btn-accent",
              onClick: _cache[4] || (_cache[4] = (...args) => unref(haptics).vibrateClick && unref(haptics).vibrateClick(...args))
            }, "vibrateClick")
          ]),
          createBaseVNode("article", _hoisted_18, [
            _hoisted_19,
            createBaseVNode("button", {
              class: "inline-block rounded-full btn btn-accent",
              onClick: _cache[5] || (_cache[5] = (...args) => unref(haptics).vibrateDisabled && unref(haptics).vibrateDisabled(...args))
            }, "Disabled")
          ]),
          createBaseVNode("article", _hoisted_20, [
            _hoisted_21,
            createBaseVNode("button", {
              class: "inline-block rounded-full btn btn-accent",
              onClick: _cache[6] || (_cache[6] = (...args) => unref(haptics).vibrateDoubleClick && unref(haptics).vibrateDoubleClick(...args))
            }, "DoubleClick")
          ]),
          createBaseVNode("article", _hoisted_22, [
            _hoisted_23,
            createBaseVNode("button", {
              class: "inline-block rounded-full btn btn-accent",
              onClick: _cache[7] || (_cache[7] = (...args) => unref(haptics).vibrateHeavyClick && unref(haptics).vibrateHeavyClick(...args))
            }, "HeavyClick")
          ]),
          createBaseVNode("article", _hoisted_24, [
            _hoisted_25,
            createBaseVNode("button", {
              class: "inline-block rounded-full btn btn-accent",
              onClick: _cache[8] || (_cache[8] = (...args) => unref(haptics).vibrateTick && unref(haptics).vibrateTick(...args))
            }, "vibrateTick")
          ]),
          createBaseVNode("article", _hoisted_26, [
            _hoisted_27,
            createVNode(FieldLabel, { label: "Vibrate Pattern:" }, {
              default: withCtx(() => [
                withDirectives(createBaseVNode("input", {
                  type: "text",
                  id: "haptics-vibrate-pattern",
                  placeholder: "1,20,1,30",
                  "onUpdate:modelValue": _cache[9] || (_cache[9] = ($event) => vibrate.value = $event)
                }, null, 512), [
                  [vModelText, vibrate.value]
                ])
              ]),
              _: 1
            }),
            createBaseVNode("button", {
              class: "inline-block rounded-full btn btn-accent",
              onClick: _cache[10] || (_cache[10] = (...args) => unref(hapticsVibrate) && unref(hapticsVibrate)(...args))
            }, "Vibrate")
          ])
        ])
      ], 64);
    };
  }
});
export { _sfc_main as default };
