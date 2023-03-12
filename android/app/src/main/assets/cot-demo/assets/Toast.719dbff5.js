import { d as defineComponent, r as ref, c as createElementBlock, a as createBaseVNode, b as createVNode, w as withCtx, T as TransitionGroup, o as openBlock, F as Fragment, g as renderList, n as normalizeClass, h as onMounted, i as withDirectives, v as vModelText, j as vModelSelect, k as resolveComponent } from "./index.f58eec1c.js";
import { F as FieldLabel } from "./FieldLabel.4b697f2e.js";
var _imports_0 = "/assets/toast.5cc20700.svg";
var LogPanel_vue_vue_type_style_index_0_lang = "";
const _hoisted_1$1 = { class: "mockup-code text-xs min-w-max w-full" };
const _hoisted_2$1 = { class: "max-h-[60vh] overflow-y-auto overflow-x-clip" };
const _hoisted_3$1 = ["data-prefix"];
const _hoisted_4$1 = ["innerHTML"];
const _hoisted_5$1 = { class: "actions pt-5 pr-5 flex justify-end" };
const toConsole = (ele) => {
  return ele.value;
};
const _sfc_main$1 = defineComponent({
  __name: "LogPanel",
  setup(__props, { expose }) {
    const messageLists = ref(/* @__PURE__ */ new Map());
    let msg_id_acc = 1;
    const pushMessage = (msg, id = msg_id_acc++) => {
      messageLists.value.set(id, msg);
      return id;
    };
    const format = (...logs) => logs.map((v) => String(v)).join(" ");
    const log = (...logs) => {
      const message = format(logs);
      pushMessage({ message, type: "debug", prefix: "~", class: "" });
    };
    const error = (...logs) => {
      const message = format(logs);
      pushMessage({ message, type: "error", prefix: "\u274C", class: "text-error" });
    };
    const warn = (...logs) => {
      const message = format(logs);
      pushMessage({ message, type: "warn", prefix: ">", class: "text-warning" });
    };
    const success = (...logs) => {
      const message = format(logs);
      pushMessage({ message, type: "success", prefix: "\u2705", class: "text-success" });
    };
    const info = (...logs) => {
      const message = format(logs);
      pushMessage({ message, type: "info", prefix: ">", class: "text-info" });
    };
    const timeMap = /* @__PURE__ */ new Map();
    const time = (label, ...logs) => {
      const msgId = pushMessage({
        message: format(label, ...logs),
        type: "info",
        prefix: "\u23F2",
        class: "bg-accent text-accent-content"
      });
      const startTime = Date.now();
      timeMap.set(label, { startTime, msgId });
    };
    const timeEnd = (label, ...logs) => {
      const timeItem = timeMap.get(label);
      if (timeItem) {
        const endTime = Date.now();
        timeMap.delete(label);
        messageLists.value.delete(timeItem.msgId);
        pushMessage(
          {
            message: format(label, ...logs) + ` <span class="px-1 text-[0.6rem] rounded-lg bg-accent text-accent-content">+${(endTime - timeItem.startTime) / 1e3}ms</span>`,
            type: "info",
            prefix: "\u25CF",
            class: "text-accent"
          },
          timeItem.msgId
        );
      }
    };
    const clear = () => {
      messageLists.value.clear();
    };
    expose({ log, debug: log, warn, success, error, info, time, timeEnd, clear });
    return (_ctx, _cache) => {
      return openBlock(), createElementBlock("div", _hoisted_1$1, [
        createBaseVNode("div", _hoisted_2$1, [
          createVNode(TransitionGroup, { name: "fade" }, {
            default: withCtx(() => [
              (openBlock(true), createElementBlock(Fragment, null, renderList(messageLists.value, ([id, item]) => {
                return openBlock(), createElementBlock("pre", {
                  key: id,
                  "data-prefix": id + " " + item.prefix,
                  class: normalizeClass(item.class)
                }, [
                  createBaseVNode("code", {
                    innerHTML: item.message
                  }, null, 8, _hoisted_4$1)
                ], 10, _hoisted_3$1);
              }), 128))
            ]),
            _: 1
          })
        ]),
        createBaseVNode("div", _hoisted_5$1, [
          createBaseVNode("button", {
            class: "btn btn-sm btn-outline btn-primary",
            onClick: _cache[0] || (_cache[0] = ($event) => clear())
          }, "Clear Log")
        ])
      ]);
    };
  }
});
const _hoisted_1 = { class: "card glass" };
const _hoisted_2 = { class: "card-body" };
const _hoisted_3 = /* @__PURE__ */ createBaseVNode("h2", { class: "card-title" }, "Show Toast", -1);
const _hoisted_4 = /* @__PURE__ */ createBaseVNode("option", { value: "long" }, "long", -1);
const _hoisted_5 = /* @__PURE__ */ createBaseVNode("option", { value: "short" }, "short", -1);
const _hoisted_6 = [
  _hoisted_4,
  _hoisted_5
];
const _hoisted_7 = { class: "card-actions justify-end" };
const _hoisted_8 = /* @__PURE__ */ createBaseVNode("div", { class: "divider" }, "LOG", -1);
const _sfc_main = /* @__PURE__ */ defineComponent({
  __name: "Toast",
  setup(__props) {
    const title = "Toast";
    const $logPanel = ref();
    const $toastPlugin = ref();
    let console;
    let toast;
    onMounted(() => {
      console = toConsole($logPanel);
      toast = $toastPlugin.value;
    });
    const toast_message = ref("\u6211\u662Ftoast\u{1F353}");
    const toast_duration = ref("short");
    const showToast = async () => {
      console.info("show toast:", toast_message.value, toast_duration.value);
      await toast.show({ text: toast_message.value, duration: toast_duration.value });
    };
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
                class: "btn btn-accent inline-block rounded-full",
                id: "toast-show",
                onClick: _cache[2] || (_cache[2] = ($event) => showToast())
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
