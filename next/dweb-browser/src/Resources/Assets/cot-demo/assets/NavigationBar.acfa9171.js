import { d as defineComponent, r as ref, o as onMounted, c as createElementBlock, a as createVNode, b as createBaseVNode, u as unref, w as withDirectives, v as vModelSelect, e as vModelCheckbox, F as Fragment, f as resolveComponent, g as openBlock } from "./index.f89085c2.js";
import { t as toConsole, _ as _sfc_main$1, d as defineLogAction } from "./LogPanel.fb338927.js";
import { V as VColorPicker } from "./VColorPicker.a712b1c6.js";
var _imports_0 = "/assets/navigationbar.fceda033.svg";
const _hoisted_1 = { class: "card glass" };
const _hoisted_2 = { class: "card-body" };
const _hoisted_3 = /* @__PURE__ */ createBaseVNode("h2", { class: "card-title" }, "Navigation Bar Background Color", -1);
const _hoisted_4 = { class: "justify-end card-actions btn-group" };
const _hoisted_5 = ["disabled"];
const _hoisted_6 = { class: "card-body" };
const _hoisted_7 = /* @__PURE__ */ createBaseVNode("h2", { class: "card-title" }, "Navigation Bar Style", -1);
const _hoisted_8 = /* @__PURE__ */ createBaseVNode("option", { value: "DEFAULT" }, "Default", -1);
const _hoisted_9 = /* @__PURE__ */ createBaseVNode("option", { value: "DARK" }, "Dark", -1);
const _hoisted_10 = /* @__PURE__ */ createBaseVNode("option", { value: "LIGHT" }, "Light", -1);
const _hoisted_11 = [
  _hoisted_8,
  _hoisted_9,
  _hoisted_10
];
const _hoisted_12 = { class: "justify-end card-actions btn-group" };
const _hoisted_13 = ["disabled"];
const _hoisted_14 = { class: "card-body" };
const _hoisted_15 = /* @__PURE__ */ createBaseVNode("h2", { class: "card-title" }, "Navigation Bar Overlays WebView", -1);
const _hoisted_16 = { class: "justify-end card-actions btn-group" };
const _hoisted_17 = ["disabled"];
const _hoisted_18 = { class: "card-body" };
const _hoisted_19 = /* @__PURE__ */ createBaseVNode("h2", { class: "card-title" }, "Navigation Bar Visible", -1);
const _hoisted_20 = { class: "justify-end card-actions btn-group" };
const _hoisted_21 = ["disabled"];
const _hoisted_22 = /* @__PURE__ */ createBaseVNode("div", { class: "divider" }, "LOG", -1);
const _sfc_main = /* @__PURE__ */ defineComponent({
  __name: "NavigationBar",
  setup(__props) {
    const title = "NavigationBar";
    const $logPanel = ref();
    const $navigationBar = ref();
    let console;
    let navigationBar;
    onMounted(async () => {
      console = toConsole($logPanel);
      navigationBar = $navigationBar.value;
      onNavigationBarChange(await navigationBar.getState(), "init");
    });
    const onNavigationBarChange = (info, type) => {
      color.value = info.color;
      style.value = info.style;
      overlay.value = info.overlay;
      visible.value = info.visible;
      console.log(type, info);
    };
    const color = ref(null);
    const setColor = defineLogAction(
      async () => {
        await navigationBar.setColor(color.value);
      },
      { name: "setColor", args: [color], logPanel: $logPanel }
    );
    const getColor = defineLogAction(
      async () => {
        color.value = await navigationBar.getColor();
      },
      { name: "getColor", args: [color], logPanel: $logPanel }
    );
    const style = ref(null);
    const setStyle = defineLogAction(
      async () => {
        await navigationBar.setStyle(style.value);
      },
      { name: "setStyle", args: [style], logPanel: $logPanel }
    );
    const getStyle = defineLogAction(
      async () => {
        style.value = await navigationBar.getStyle();
      },
      { name: "getStyle", args: [style], logPanel: $logPanel }
    );
    const overlay = ref(null);
    const setOverlay = defineLogAction(() => navigationBar.setOverlay(overlay.value), {
      name: "setOverlay",
      args: [overlay],
      logPanel: $logPanel
    });
    const getOverlay = defineLogAction(
      async () => {
        overlay.value = await navigationBar.getOverlay();
      },
      {
        name: "getOverlay",
        args: [overlay],
        logPanel: $logPanel
      }
    );
    const visible = ref(null);
    const setVisible = defineLogAction(() => navigationBar.setVisible(visible.value), {
      name: "setVisible",
      args: [visible],
      logPanel: $logPanel
    });
    const getVisible = defineLogAction(
      async () => {
        visible.value = await navigationBar.getVisible();
      },
      {
        name: "getOverlay",
        args: [visible],
        logPanel: $logPanel
      }
    );
    return (_ctx, _cache) => {
      const _component_dweb_navigation_bar = resolveComponent("dweb-navigation-bar");
      return openBlock(), createElementBlock(Fragment, null, [
        createVNode(_component_dweb_navigation_bar, {
          ref_key: "$navigationBar",
          ref: $navigationBar,
          onStatechange: _cache[0] || (_cache[0] = ($event) => onNavigationBarChange($event.detail, "change"))
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
            createVNode(VColorPicker, {
              modelValue: color.value,
              "onUpdate:modelValue": _cache[1] || (_cache[1] = ($event) => color.value = $event),
              modes: ["rgba"]
            }, null, 8, ["modelValue"]),
            createBaseVNode("div", _hoisted_4, [
              createBaseVNode("button", {
                class: "inline-block rounded-full btn btn-accent",
                disabled: null == color.value,
                onClick: _cache[2] || (_cache[2] = (...args) => unref(setColor) && unref(setColor)(...args))
              }, " Set ", 8, _hoisted_5),
              createBaseVNode("button", {
                class: "inline-block rounded-full btn btn-accent",
                onClick: _cache[3] || (_cache[3] = (...args) => unref(getColor) && unref(getColor)(...args))
              }, "Get")
            ])
          ]),
          createBaseVNode("article", _hoisted_6, [
            _hoisted_7,
            withDirectives(createBaseVNode("select", {
              class: "w-full max-w-xs select",
              name: "navigationbar-style",
              id: "navigationbar-style",
              "onUpdate:modelValue": _cache[4] || (_cache[4] = ($event) => style.value = $event)
            }, _hoisted_11, 512), [
              [vModelSelect, style.value]
            ]),
            createBaseVNode("div", _hoisted_12, [
              createBaseVNode("button", {
                class: "inline-block rounded-full btn btn-accent",
                disabled: null == style.value,
                onClick: _cache[5] || (_cache[5] = (...args) => unref(setStyle) && unref(setStyle)(...args))
              }, " Set ", 8, _hoisted_13),
              createBaseVNode("button", {
                class: "inline-block rounded-full btn btn-accent",
                onClick: _cache[6] || (_cache[6] = (...args) => unref(getStyle) && unref(getStyle)(...args))
              }, "Get")
            ])
          ]),
          createBaseVNode("article", _hoisted_14, [
            _hoisted_15,
            withDirectives(createBaseVNode("input", {
              class: "toggle",
              type: "checkbox",
              id: "navigationbar-overlay",
              "onUpdate:modelValue": _cache[7] || (_cache[7] = ($event) => overlay.value = $event)
            }, null, 512), [
              [vModelCheckbox, overlay.value]
            ]),
            createBaseVNode("div", _hoisted_16, [
              createBaseVNode("button", {
                class: "inline-block rounded-full btn btn-accent",
                disabled: null == overlay.value,
                onClick: _cache[8] || (_cache[8] = (...args) => unref(setOverlay) && unref(setOverlay)(...args))
              }, " Set ", 8, _hoisted_17),
              createBaseVNode("button", {
                class: "inline-block rounded-full btn btn-accent",
                onClick: _cache[9] || (_cache[9] = (...args) => unref(getOverlay) && unref(getOverlay)(...args))
              }, "Get")
            ])
          ]),
          createBaseVNode("article", _hoisted_18, [
            _hoisted_19,
            withDirectives(createBaseVNode("input", {
              class: "toggle",
              type: "checkbox",
              id: "navigationbar-overlay",
              "onUpdate:modelValue": _cache[10] || (_cache[10] = ($event) => visible.value = $event)
            }, null, 512), [
              [vModelCheckbox, visible.value]
            ]),
            createBaseVNode("div", _hoisted_20, [
              createBaseVNode("button", {
                class: "inline-block rounded-full btn btn-accent",
                disabled: null == visible.value,
                onClick: _cache[11] || (_cache[11] = (...args) => unref(setVisible) && unref(setVisible)(...args))
              }, " Set ", 8, _hoisted_21),
              createBaseVNode("button", {
                class: "inline-block rounded-full btn btn-accent",
                onClick: _cache[12] || (_cache[12] = (...args) => unref(getVisible) && unref(getVisible)(...args))
              }, "Get")
            ])
          ])
        ]),
        _hoisted_22,
        createVNode(_sfc_main$1, {
          ref_key: "$logPanel",
          ref: $logPanel
        }, null, 512)
      ], 64);
    };
  }
});
export { _sfc_main as default };
