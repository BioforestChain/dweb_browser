import { strictImageResource } from "helper/imageResourcesHelper.ts";
import { RandomNumberOptions, randomStringToNumber } from "helper/randomHelper.ts";
import { Compareable, enumToCompareable } from "helper/sortHelper.ts";
import { readAcceptSvg } from "src/provider/api.ts";
import { buildApiRequestArgs } from "src/provider/fetch.ts";
import { $WidgetAppData } from "src/types/app.type.ts";
import { ShallowRef, watchEffect } from "vue";
import blankApp_svg from "./blankApp.svg";
import { $AppIconInfo } from "./types.ts";

export const watchEffectAppMetadataToAppIcon = (
  config: {
    metaData: $WidgetAppData;
    defaultIconUrl?: string;
  },
  outputRef: ShallowRef<$AppIconInfo>
) => {
  const off = watchEffect(async () => {
    const acceptImage = await readAcceptSvg();
    const appMetaData = config.metaData;
    const { defaultIconUrl = blankApp_svg } = config;
    const selectedIcon = (appMetaData.icons ?? [])
      .map(
        (icon) =>
          new Compareable(strictImageResource(icon), (icon) => {
            const size = icon.sizes.at(-1)!;
            const area = size.width * size.height;
            return {
              purpose: enumToCompareable(icon.purpose, ["maskable", "any", "monochrome"]),
              type: acceptImage.length - acceptImage.findIndex((acceptTester) => acceptTester(icon.type)),
              area,
            };
          })
      )
      .sort((a, b) => a.compare(b))
      .at(-1);
    const iconurl = selectedIcon?.value.src ?? defaultIconUrl;

    /// 进行深拷贝，确保对象引用改变
    const refValue = { ...outputRef.value };
    refValue.src = iconurl.startsWith("file:")
      ? buildApiRequestArgs("/readFile", {
          search: {
            url: selectedIcon?.value.src ?? defaultIconUrl,
          },
        })[0].href
      : iconurl;
    refValue.markable = selectedIcon?.value.purpose === "maskable";

    const randomById = (config?: RandomNumberOptions) => randomStringToNumber(appMetaData.mmid, config);
    const monochrome = selectedIcon?.value.purpose === "monochrome" || iconurl === defaultIconUrl;
    refValue.monochrome = monochrome;
    refValue.monocolor = monochrome
      ? appMetaData.theme_color ??
        `hwb(${randomById({ seed: "hue", max: 360 })}deg 0% ${randomById({
          seed: "black",
          min: 0,
          max: 10,
        })}%)`
      : undefined;

    /// 触发更新，深拷贝的 shadowRef 的变更可以触发到 子组件
    outputRef.value = refValue;
  });

  return { off };
};
export type $WatchEffectAppMetadataToAppIconReturn = ReturnType<typeof watchEffectAppMetadataToAppIcon>;
