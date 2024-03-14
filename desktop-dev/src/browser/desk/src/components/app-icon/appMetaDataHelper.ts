import { readAcceptSvg } from "@/provider/api.ts";
import { buildApiRequestArgs } from "@/provider/fetch.ts";
import { $WidgetAppData } from "@/types/app.type.ts";
import { strictImageResource } from "helper/imageResourcesHelper.ts";
import { RandomNumberOptions, randomStringToNumber } from "helper/randomHelper.ts";
import { Compareable as ComparableWrapper, enumToCompareable as enumToComparable } from "helper/sortHelper.ts";
import { ShallowRef, watchEffect } from "vue";
import "../../shims.ts";
import blankApp_svg from "./blankApp.svg";
import { $AppIconInfo } from "./types.ts";

/**
 * 挑选合适的图标作为桌面上的图标
 * @param config
 * @param outputRef
 * @returns
 */
export const watchEffectAppMetadataToAppIcon = (
  config: {
    metaData: $WidgetAppData;
    defaultIconUrl?: string;
  },
  outputRef: ShallowRef<$AppIconInfo>,
) => {
  const off = watchEffect(async () => {
    const acceptImage = await readAcceptSvg();
    const appMetaData = config.metaData;
    const { defaultIconUrl = blankApp_svg } = config;
    const selectedIcon = (appMetaData.icons ?? [])
      .map(
        (icon) =>
          new ComparableWrapper(strictImageResource(icon), (icon) => {
            const size = icon.sizes.at(-1)!;
            const area = -size.width * size.height;
            return {
              purpose: enumToComparable(icon.purpose, ["maskable", "any", "monochrome"]),
              type: acceptImage.length - acceptImage.findIndex((acceptTester) => acceptTester(icon.type)),
              area,
            };
          }),
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
    refValue.maskable = selectedIcon?.value.purpose === "maskable";

    const randomById = (config?: RandomNumberOptions) => randomStringToNumber(appMetaData.mmid, config);
    const monochrome = selectedIcon?.value.purpose === "monochrome" || iconurl === defaultIconUrl;
    refValue.monochrome = monochrome;
    if (monochrome) {
      /// 如果有配色，那么使用配色
      if (appMetaData.theme_color) {
        refValue.monocolor = appMetaData.theme_color;
      }
      /// 否则使用自动生成的渐变色
      else {
        /** to bottom */
        const randomGradientDeg = randomById({ seed: "gradient", min: 90, max: 270 });
        const randomColorHue = randomById({ seed: "hue", max: 360 });
        refValue.monoimage = `linear-gradient(${randomGradientDeg}deg, hsl(${randomColorHue}deg 100% 65%), hsl(${randomColorHue}deg 100% 45%))`;
      }
    }

    /// 触发更新，深拷贝的 shadowRef 的变更可以触发到 子组件
    outputRef.value = refValue;
  });

  return { off };
};
export type $WatchEffectAppMetadataToAppIconReturn = ReturnType<typeof watchEffectAppMetadataToAppIcon>;
