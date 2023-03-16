/**
 * 将rgba(r, b, g, a)或#rrbbggaa或#rgba转为#rrbbggaa 十六进制
 * @param color
 * @returns  #rrbbggaa
 */
export function convertColorToArga(color: string) {
  // 默认是纯黑色
  const rgbaColor = [/* red */ 0, /* green */ 0, /* blue */ 0, /* alpha */ 255];

  // "rgba("  "rgb("
  if (color.startsWith("rgb")) {
    // 提取括号中的内容，提取数字
    const colorArr = color.match(/\((.+)\)/)?.[1].match(/\d+/g);
    if (colorArr) {
      for (const [index, item] of colorArr.entries()) {
        const value = parseFloat(item);
        switch (index) {
          case 0:
            rgbaColor[index] = value;
            break;
          case 1:
            rgbaColor[index] = value;
            break;
          case 2:
            rgbaColor[index] = value;
            break;
          case 3:
            rgbaColor[index] = value * 255;
            break;
        }
      }
    }
  } else if (color.startsWith("#")) {
    color = color.slice(1);
    if (color.length === 8 || color.length === 6) {
      color.match(/../g)!.forEach((value, index) => {
        rgbaColor[index] = parseInt(value, 16);
      });
    } else if (color.length === 4 || color.length === 3) {
      color.match(/./g)!.forEach((value, index) => {
        rgbaColor[index] = parseInt(value + value, 16);
      });
    }
  }
  return {
    red: rgbaColor[0],
    green: rgbaColor[1],
    blue: rgbaColor[2],
    alpha: rgbaColor[3],
  } as $AgbaColor;
}
export type $AgbaColor = {
  red: number;
  green: number;
  blue: number;
  alpha: number;
};
export const enum COLOR_FORMAT {
  HEXA,
  RGBA,
}
export function normalizeArgaToColor(
  color: $AgbaColor,
  format: COLOR_FORMAT = COLOR_FORMAT.RGBA
) {
  const rgbaColor = [color.red, color.green, color.blue, color.alpha];
  if (format === COLOR_FORMAT.HEXA) {
    const hex =
      "#" +
      rgbaColor
        .map((v) => (v & 255).toString(16).padStart(2, "0"))
        .join("")
        .toUpperCase();
    if (hex.endsWith("ff")) {
      return hex.slice(0, -2);
    }
    return hex;
  }

  /// COLOR.RGBA
  return `rgba(${rgbaColor
    .map((v, index) => {
      if (index === 3) {
        return v / 255;
      }
      return v;
    })
    .join(",")})`;
}
