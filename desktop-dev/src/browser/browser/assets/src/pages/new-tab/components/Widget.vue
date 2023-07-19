<script lang="ts" setup>
import { $WidgetMetaData } from "@/types/app.type";
import { computed } from "vue";
import "./widget.ts";

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
const buildWidgetElement = (widgetMetaData: $WidgetMetaData) => {
  class WidgetElement extends HTMLElement {
    private static template = document.createElement("template");
    private static meta = widgetMetaData;
    private static style = document.adoptedStyleSheets ? new CSSStyleSheet() : document.createElement("style");
    static updateMeta(meta: $WidgetMetaData) {
      WidgetElement.meta = meta;
      WidgetElement.template.innerHTML = meta.templateHtml;
      if (WidgetElement.style instanceof CSSStyleSheet) {
        WidgetElement.style.replaceSync(meta.scopedStyle);
      } else {
        WidgetElement.style.innerHTML = meta.scopedStyle;
      }
    }
    constructor() {
      super();
      const meta = WidgetElement.meta;
      const shadow = this.attachShadow({ mode: "open" });
      shadow.appendChild(WidgetElement.template.content.cloneNode(true));

      if (shadow.adoptedStyleSheets) {
        const styleSheet = new CSSStyleSheet();
        styleSheet.replaceSync(meta.scopedStyle);
        shadow.adoptedStyleSheets = [styleSheet];
      } else {
        const styleEle = document.createElement("style");
        styleEle.innerHTML = meta.scopedStyle;
        shadow.appendChild(styleEle);
      }
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
<style scoped lang="scss"></style>
