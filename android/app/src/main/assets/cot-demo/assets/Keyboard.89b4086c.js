import { d as defineComponent, c as createElementBlock, a as createBaseVNode, f as createStaticVNode, o as openBlock } from "./index.f58eec1c.js";
var _imports_0 = "/assets/keyboard.c8ce4e4e.svg";
const _hoisted_1 = { class: "card glass" };
const _hoisted_2 = /* @__PURE__ */ createStaticVNode('<article class="card-body"><h2 class="card-title">Keyboard Show/Hide</h2><div class="card-actions justify-end btn-group"><button class="btn btn-accent inline-block rounded-full" id="keyboard-show">Show</button><button class="btn btn-accent inline-block rounded-full" id="keyboard-hide">Hide</button></div></article><div class="card-body"><div class="mockup-code text-xs min-w-max"><pre id="keyboard-observer-log"><code>xxxx</code></pre></div></div>', 2);
const _sfc_main = /* @__PURE__ */ defineComponent({
  __name: "Keyboard",
  setup(__props) {
    const title = "Keyboard";
    return (_ctx, _cache) => {
      return openBlock(), createElementBlock("div", _hoisted_1, [
        createBaseVNode("figure", { class: "icon" }, [
          createBaseVNode("img", {
            src: _imports_0,
            alt: title
          })
        ]),
        _hoisted_2
      ]);
    };
  }
});
export { _sfc_main as default };
