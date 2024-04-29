import type { Canvg, Property } from "canvg";
import { calcFitSize } from "./util/calcFitSize.ts";
import type { $Size } from "./util/types.ts";
export const svgToBlob = (() => {
  const _installer = async () => {
    Object.assign(self, {
      global: self,
    });
    const { DOMParser } = await import("xmldom");
    const { Canvg, presets } = await import("canvg");
    const preset = presets.offscreen({ DOMParser: DOMParser });
    const canvas = new OffscreenCanvas(128, 128); // 无所谓，canvg会自动根据svg真实的大小进行调整绘制
    const ctx = canvas.getContext("2d")!;
    return { Canvg, preset, canvas, ctx, DOMParser };
  };
  let installed: undefined | ReturnType<typeof _installer>;
  const installer = () => (installed ??= _installer());

  const getSvgBoxSize = (v: Canvg) => {
    const svgEle = v.document.documentElement!;
    const widthAttr = svgEle.getAttribute("width", true);
    const heightAttr = svgEle.getAttribute("height", true);
    const viewBoxAttr = svgEle.getAttribute("viewBox") as Property<string>;
    // const styleAttr = svgEle.getAttribute("style");
    const originWidth = widthAttr.getNumber(0);
    const originHeight = heightAttr.getNumber(0);
    const defaultWidth = 800;
    const defaultHeight = 600;
    const originViewBox = viewBoxAttr.getValue("0,0,512,512").split(/[\s,]/);
    return {
      width: originWidth || +originViewBox[2] || defaultWidth,
      height: originHeight || +originViewBox[3] || defaultHeight,
    };
  };

  return async (svgCode: string, fitSize: $Size) => {
    const { Canvg, preset, canvas, ctx } = await installer();
    const v = await Canvg.from(ctx, svgCode, preset);

    const originSize = getSvgBoxSize(v);
    const newSize = calcFitSize(originSize, fitSize);
    v.resize(newSize.width, newSize.height, true);
    await v.render();
    return await canvas.convertToBlob();
  };
})();
