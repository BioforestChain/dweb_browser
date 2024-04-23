import type { ImageResource } from "../core/types.ts";

const PURPOSES = ["monochrome", "maskable", "any"] as const;
export const strictImageResource = (img: ImageResource, baseUrl = document.baseURI) => {
  const imgUrl = new URL(img.src, baseUrl);
  let imageType = img.type;
  if (imageType === undefined) {
    const imageUrlExt = imgUrl.pathname.slice(imgUrl.pathname.lastIndexOf("."));
    if (imageUrlExt === ".jpg" || imageUrlExt === ".jpeg") {
      imageType = "image/jpeg";
    } else if (
      imageUrlExt === ".webp" ||
      imageUrlExt === ".png" ||
      imageUrlExt === ".avif" ||
      imageUrlExt === ".apng"
    ) {
      imageType = "image/" + imageUrlExt.slice(1);
    } else if (imageUrlExt === ".svg") {
      imageType = "image/svg+xml";
    } else {
      imageType = "image/*";
    }
  }

  const imageSizes: Array<{ width: number; height: number }> = [];
  if (img.sizes === undefined || img.sizes === null) {
    if (imageType === "image/svg+xml") {/// floor(sqrt(max_int))
      imageSizes.push({ width: 46340, height: 46340 });
    } else {
      imageSizes.push({ width: 1, height: 1 });
    }
  } else if (img.sizes === "any") {
    imageSizes.push({ width: 46340, height: 46340 });
  } else {
    for (const size of img.sizes.split(/\s+/g)) {
      const matchd = size.match(/(\d+)x(\d+)/);
      if (matchd) {
        imageSizes.push({ width: +matchd[1], height: +matchd[2] });
      }
    }
    Math.hypot;
    if (imageSizes.length === 0) {
      imageSizes.push({ width: 1, height: 1 });
    }
  }

  return {
    src: imgUrl.href,
    purpose: (PURPOSES.includes(img.purpose as never) ? img.purpose : "any") as (typeof PURPOSES)[number],
    type: imageType,
    sizes: imageSizes,
  };
};
