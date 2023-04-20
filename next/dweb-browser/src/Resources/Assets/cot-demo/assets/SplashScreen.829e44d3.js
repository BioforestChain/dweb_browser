import { d as defineComponent, r as ref, o as onMounted, c as createElementBlock, a as createVNode, b as createBaseVNode, h as withCtx, u as unref, F as Fragment, g as openBlock, w as withDirectives, at as vModelText, f as resolveComponent } from "./index.f89085c2.js";
import { _ as _imports_0 } from "./splashscreen.ecee76e9.js";
import { F as FieldLabel } from "./FieldLabel.e4aa2d01.js";
import { t as toConsole, d as defineLogAction } from "./LogPanel.fb338927.js";
const _hoisted_1 = { class: "card glass" };
const _hoisted_2 = { class: "card-body" };
const _hoisted_3 = /* @__PURE__ */ createBaseVNode("h2", { class: "card-title" }, "Splash Screen Show/Hide", -1);
const _hoisted_4 = { class: "input-group" };
const _hoisted_5 = /* @__PURE__ */ createBaseVNode("span", null, "ms", -1);
const _hoisted_6 = { class: "justify-end card-actions btn-group" };
const _sfc_main = /* @__PURE__ */ defineComponent({
  __name: "SplashScreen",
  setup(__props) {
    const $logPanel = ref();
    const $splashScreen = ref();
    let console;
    let splashScreen;
    onMounted(async () => {
      console = toConsole($logPanel);
      splashScreen = $splashScreen.value;
    });
    const autoHidden = ref(1e3);
    const show = defineLogAction(async () => {
      const result = await await splashScreen.show({ showDuration: autoHidden.value });
      console.info("splash screen:", result);
    }, { name: "show", args: [autoHidden], logPanel: $logPanel });
    const title = "Splash Screen";
    return (_ctx, _cache) => {
      const _component_dweb_splash_screen = resolveComponent("dweb-splash-screen");
      return openBlock(), createElementBlock(Fragment, null, [
        createVNode(_component_dweb_splash_screen, {
          ref_key: "$splashScreen",
          ref: $splashScreen
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
            createVNode(FieldLabel, { label: "Auto Hidden After:" }, {
              default: withCtx(() => [
                createBaseVNode("label", _hoisted_4, [
                  withDirectives(createBaseVNode("input", {
                    type: "number",
                    placeholder: "1000",
                    "onUpdate:modelValue": _cache[0] || (_cache[0] = ($event) => autoHidden.value = $event)
                  }, null, 512), [
                    [vModelText, autoHidden.value]
                  ]),
                  _hoisted_5
                ])
              ]),
              _: 1
            }),
            createBaseVNode("div", _hoisted_6, [
              createBaseVNode("button", {
                class: "rounded-full btn btn-accent",
                onClick: _cache[1] || (_cache[1] = (...args) => unref(show) && unref(show)(...args))
              }, "Show")
            ])
          ])
        ])
      ], 64);
    };
  }
});
export { _sfc_main as default };
