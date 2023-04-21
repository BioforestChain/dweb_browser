import { d as defineComponent, r as ref, c as createElementBlock, b as createBaseVNode, a as createVNode, h as withCtx, T as TransitionGroup, g as openBlock, F as Fragment, i as renderList, n as normalizeClass, j as isRef } from "./index.f89085c2.js";
var LogPanel_vue_vue_type_style_index_0_lang = "";
var LogPanel_vue_vue_type_style_index_1_lang = "";
const _hoisted_1 = { class: "mockup-code text-xs min-w-full w-full" };
const _hoisted_2 = { class: "max-h-[60vh] overflow-y-auto overflow-x-clip flex flex-col-reverse" };
const _hoisted_3 = /* @__PURE__ */ createBaseVNode("div", { class: "anchor" }, null, -1);
const _hoisted_4 = ["data-prefix"];
const _hoisted_5 = ["innerHTML"];
const _hoisted_6 = { class: "actions pt-5 pr-5 flex justify-end" };
const toConsole = (ele) => {
  return ele.value;
};
const normalizeArgs = (args) => {
  const res = [];
  for (const arg of args) {
    if (isRef(arg)) {
      res.push(arg.value);
    } else {
      res.push(arg);
    }
  }
  return res;
};
const defineLogAction = (fun, config) => {
  const nargs = () => normalizeArgs(config.args);
  return async (...args) => {
    const logger = config.logPanel.value || console;
    logger.time(config.name, ...nargs());
    try {
      const result = await fun(...args);
      logger.timeEnd(
        config.name,
        ...nargs(),
        `<span class="px-1 text-[0.6rem] rounded-lg bg-accent text-accent-content shrink-0">-></span>`,
        result
      );
      return result;
    } catch (err) {
      logger.timeEnd(config.name, ...nargs());
      logger.error(config.name, err);
    }
  };
};
const _sfc_main = defineComponent({
  __name: "LogPanel",
  setup(__props, { expose }) {
    const messageLists = ref(/* @__PURE__ */ new Map());
    let msg_id_acc = 1;
    const pushMessage = (msg, id = msg_id_acc++) => {
      messageLists.value.set(id, msg);
      return id;
    };
    const format = (...logs) => logs.map((v) => {
      if (typeof v === "object" && v !== null) {
        if (Object.getPrototypeOf(v) === Object.prototype) {
          try {
            return `<span class="text-blue">${JSON.stringify(v)}</span>`;
          } catch {
          }
        } else if (v instanceof Error) {
          return `<span class="text-red">${v.stack || v.message}</span>`;
        }
      }
      return String(v);
    }).join(" ");
    const log = (...logs) => {
      const message = format(...logs);
      pushMessage({ message, type: "debug", prefix: "~", class: "" });
    };
    const error = (...logs) => {
      const message = format(...logs);
      pushMessage({ message, type: "error", prefix: ">", class: "text-error" });
    };
    const warn = (...logs) => {
      const message = format(...logs);
      pushMessage({ message, type: "warn", prefix: ">", class: "text-warning" });
    };
    const success = (...logs) => {
      const message = format(...logs);
      pushMessage({ message, type: "success", prefix: ">", class: "text-success" });
    };
    const info = (...logs) => {
      const message = format(...logs);
      pushMessage({ message, type: "info", prefix: ">", class: "text-info" });
    };
    const timeMap = /* @__PURE__ */ new Map();
    const time = (label, ...logs) => {
      if (timeMap.has(label)) {
        return;
      }
      const msgId = pushMessage({
        message: format(label, ...logs),
        type: "info",
        prefix: ".",
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
            message: format(label, ...logs) + ` <span class="px-1 text-[0.6rem] rounded-lg bg-accent text-accent-content shrink-0">+${(endTime - timeItem.startTime) / 1e3}ms</span>`,
            type: "info",
            prefix: ".",
            class: "text-accent"
          },
          timeItem.msgId
        );
      }
    };
    const clear = () => {
      timeMap.clear();
      messageLists.value.clear();
    };
    expose({ log, debug: log, warn, success, error, info, time, timeEnd, clear });
    return (_ctx, _cache) => {
      return openBlock(), createElementBlock("div", _hoisted_1, [
        createBaseVNode("div", _hoisted_2, [
          _hoisted_3,
          createVNode(TransitionGroup, { name: "fade" }, {
            default: withCtx(() => [
              (openBlock(true), createElementBlock(Fragment, null, renderList(messageLists.value, ([id, item]) => {
                return openBlock(), createElementBlock("pre", {
                  key: id,
                  "data-prefix": id + item.prefix,
                  class: normalizeClass(["whitespace-normal flex flex-row justify-start py-1", item.class])
                }, [
                  createBaseVNode("code", {
                    class: "flex-1 break-all flex flex-wrap justify-between flex-row",
                    innerHTML: item.message
                  }, null, 8, _hoisted_5)
                ], 10, _hoisted_4);
              }), 128))
            ]),
            _: 1
          })
        ]),
        createBaseVNode("div", _hoisted_6, [
          createBaseVNode("button", {
            class: "btn btn-sm btn-outline btn-primary",
            onClick: _cache[0] || (_cache[0] = ($event) => clear())
          }, "Clear Log")
        ])
      ]);
    };
  }
});
export { _sfc_main as _, defineLogAction as d, toConsole as t };
