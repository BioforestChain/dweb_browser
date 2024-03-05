import type { InjectionKey, Ref } from "vue";

export const gridMeshKey = Symbol("gridMesh") as InjectionKey<{
  columns: Ref<number>;
  rows: Ref<number>;
}>;

export const gridTemplateSizeKey = Symbol("gridTemplateSize") as InjectionKey<{
  rowTemplateSize: number;
  columnTemplateSize: number;
}>;
