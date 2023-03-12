import { d as defineComponent, c as createElementBlock, a as createBaseVNode, f as createStaticVNode, o as openBlock } from "./index.f58eec1c.js";
var _imports_0 = "/assets/navigationbar.fceda033.svg";
const _hoisted_1 = { class: "card glass" };
const _hoisted_2 = /* @__PURE__ */ createStaticVNode('<article class="card-body"><h2 class="card-title">Navigation Bar Color</h2><input class="color" type="color" id="navigationbar-color"><div class="card-actions justify-end btn-group"><button class="btn btn-accent rounded-full" id="navigationbar-setNavigationBarColor">Set</button><button class="btn btn-accent rounded-full" id="navigationbar-getNavigationBarColor">Get</button></div></article><article class="card-body"><h2 class="card-title">Navigation Bar Overlays WebView</h2><input class="toggle toggle-accent" type="checkbox" id="navigationbar-overlay"><div class="card-actions justify-end btn-group"><button class="btn btn-accent inline-block rounded-full" id="navigationbar-setOverlaysWebView">Set</button><button class="btn btn-accent inline-block rounded-full" id="navigationbar-getOverlaysWebView">Get</button></div></article><div class="card-body"><div class="mockup-code text-xs min-w-max"><pre id="navigationbar-observer-log"><code>xxxx</code></pre></div></div>', 3);
const _sfc_main = /* @__PURE__ */ defineComponent({
  __name: "NavigationBar",
  setup(__props) {
    const title = "Navigation Bar";
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
