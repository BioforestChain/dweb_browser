<script lang="ts" setup>
import { $WidgetMetaData } from "@/types/app.type";
import { computed } from "vue";
import base_css from "./widget-base.scss?inline";
import "./widget.scss";

const props = defineProps({
  widgetMetaData: {
    type: Object as () => $WidgetMetaData,
    required: true,
  },
  index: {
    type: Number,
    required: true,
  },
});
const createStyleHelper = (forceStyle = false) => {
  if (document.adoptedStyleSheets && !forceStyle) {
    const style = new CSSStyleSheet();
    return {
      installStyle(root: ShadowRoot) {
        if (root.adoptedStyleSheets.includes(style) === false) {
          root.adoptedStyleSheets = [...root.adoptedStyleSheets, style];
        }
        return this;
      },
      uninstallStyle(root: ShadowRoot) {
        const sss = root.adoptedStyleSheets.slice();
        const index = sss.indexOf(style);
        if (index !== -1) {
          sss.splice(index, 1);
          root.adoptedStyleSheets = sss;
        }
        return this;
      },
      setCssText(cssText: string) {
        style.replaceSync(cssText);
        return this;
      },
    };
  } else {
    const style = document.createElement("style");
    return {
      installStyle(root: ShadowRoot) {
        root.appendChild(style);
        return this;
      },
      uninstallStyle(_root: DocumentOrShadowRoot) {
        style.parentElement?.removeChild(style);
        return this;
      },
      setCssText(cssText: string) {
        style.innerHTML = cssText;
        return this;
      },
    };
  }
};

const buildWidgetElement = (widgetMetaData: $WidgetMetaData) => {
  class WidgetElement extends HTMLElement {
    private static meta = widgetMetaData;
    private static baseStyle = createStyleHelper(true).setCssText(base_css);

    private static template = document.createElement("template");
    private static style = createStyleHelper(true);
    static updateMeta(meta: $WidgetMetaData) {
      this.meta = meta;
      this.template.innerHTML = meta.templateHtml;
      this.style.setCssText(meta.scopedStyle);
    }
    constructor() {
      super();
      const shadow = this.attachShadow({ mode: "open" });
      shadow.appendChild(WidgetElement.template.content.cloneNode(true));

      WidgetElement.baseStyle.installStyle(shadow);
      WidgetElement.style.installStyle(shadow);
    }
  }
  return WidgetElement;
};
const html = computed(() => {
  const tagName = `${props.widgetMetaData.appId}--${props.widgetMetaData.widgetName}`;
  /// TODO 如果应用升级，那么就需要刷新重载桌面，才能确保这些组件使用上新的 metadata
  let WidgetElement = customElements.get(tagName) as ReturnType<typeof buildWidgetElement> | undefined;
  if (WidgetElement === undefined) {
    WidgetElement = buildWidgetElement(props.widgetMetaData);
    customElements.define(tagName, WidgetElement);
  }
  WidgetElement.updateMeta(props.widgetMetaData);

  return `<${tagName}></${tagName}>`;
});
</script>
<template>
  <div class="widget" draggable="true" v-html="html"></div>
</template>
