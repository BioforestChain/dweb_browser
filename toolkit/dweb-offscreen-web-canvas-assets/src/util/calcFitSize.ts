import type { $Size } from "./types.ts";

export const calcFitSize = (imgSize: $Size, fitSize: $Size) => {
  const aspectRatio = imgSize.width / imgSize.height;
  /// 寻找最小的 fit
  const fit_width_s_height = fitSize.width / aspectRatio;
  let fitWidth = fitSize.width;
  let fitHeight = fit_width_s_height;
  if (fitHeight > fitSize.height) {
    fitWidth = fitSize.height * aspectRatio;
    fitHeight = fitSize.height;
  }
  return { width: fitWidth, height: fitHeight } satisfies $Size;
};
