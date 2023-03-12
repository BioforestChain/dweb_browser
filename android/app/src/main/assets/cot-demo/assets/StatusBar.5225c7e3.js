import { d as defineComponent, c as createElementBlock, a as createBaseVNode, f as createStaticVNode, o as openBlock } from "./index.f58eec1c.js";
var _imports_0 = "/assets/statusbar.8abf1ec3.svg";
const _hoisted_1 = { class: "card glass" };
const _hoisted_2 = /* @__PURE__ */ createStaticVNode('<article class="card-body"><h2 class="card-title">Status Bar Background Color</h2><input class="color" type="color" id="statusbar-background-color"><div class="card-actions justify-end btn-group"><button class="btn btn-accent inline-block rounded-full" id="statusbar-setBackgroundColor">Set</button><button class="btn btn-accent inline-block rounded-full" id="statusbar-getBackgroundColor">Get</button></div></article><article class="card-body"><h2 class="card-title">Status Bar Style</h2><select class="select w-full max-w-xs" name="statusbar-style" id="statusbar-style"><option value="Default">Default</option><option value="Dark">Dark</option><option value="Light">Light</option></select><div class="card-actions justify-end btn-group"><button class="btn btn-accent inline-block rounded-full" id="statusbar-setStyle">Set</button><button class="btn btn-accent inline-block rounded-full" id="statusbar-getStyle">Get</button></div></article><article class="card-body"><h2 class="card-title">Status Bar Overlays WebView</h2><input class="toggle" type="checkbox" id="statusbar-overlay"><div class="card-actions justify-end btn-group"><button class="btn btn-accent inline-block rounded-full" id="statusbar-setOverlaysWebView">Set</button><button class="btn btn-accent inline-block rounded-full" id="statusbar-getOverlaysWebView">Get</button></div></article><div class="card-body"><div class="mockup-code text-xs min-w-max"><pre id="statusbar-observer-log"><code>xxxx</code></pre></div></div>', 4);
const _sfc_main = /* @__PURE__ */ defineComponent({
  __name: "StatusBar",
  setup(__props) {
    const title = "StatusBar";
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
