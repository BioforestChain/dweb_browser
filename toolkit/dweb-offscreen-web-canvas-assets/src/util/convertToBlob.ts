const isDesktop = (() => {
  try {
    return !navigator.userAgentData.mobile;
  } catch {
    return false;
  }
})();
export const convertToBlob = (canvas: OffscreenCanvas | HTMLCanvasElement, options?: ImageEncodeOptions) => {
  if ("convertToBlob" in canvas) {
    return canvas.convertToBlob();
  }
  if (isDesktop && "toDataURL" in canvas) {
    return fetch(canvas.toDataURL(options?.type, options?.quality)).then((res) => res.blob());
  }
  if ("toBlob" in canvas) {
    return new Promise<Blob>((resolve, reject) => {
      canvas.toBlob(
        (blob) => {
          if (blob) {
            resolve(blob);
          } else {
            reject("fail to blob");
          }
        },
        options?.type,
        options?.quality
      );
    });
  }
  throw new Error("fail to convertToBlob");
};
