/**
 * 把 RGB 颜色转为 16进制颜色
 * @param r
 * @param g
 * @param b
 * @param a
 * @returns
 */
export function converRGBAToHexa(r: string, g: string, b: string, a: string) {
  let hexaR = parseInt(r).toString(16).toUpperCase();
  let hexaG = parseInt(g).toString(16).toUpperCase();
  let hexaB = parseInt(b).toString(16).toUpperCase();
  let hexaA = parseInt(a).toString(16).toUpperCase();
  hexaR = hexaR.length === 1 ? `0${hexaR}` : hexaR;
  hexaG = hexaG.length === 1 ? `0${hexaG}` : hexaG;
  hexaB = hexaB.length === 1 ? `0${hexaB}` : hexaB;
  hexaA = hexaA.length === 1 ? `0${hexaA}` : hexaA;
  return `#${hexaR}${hexaG}${hexaB}${hexaA}`;
}

/**
 * 把十六进制的颜色转为  AgbaColor
 * @param str
 */
export function hexaToRGBA(str: string): $AgbaColor {
  return {
    red: parseInt(str.slice(1, 3), 16),
    green: parseInt(str.slice(3, 5), 16),
    blue: parseInt(str.slice(5, 7), 16),
    alpha: parseInt(str.slice(7), 16),
  };
}

export type $AgbaColor = {
  red: number;
  green: number;
  blue: number;
  alpha: number;
};

export function colorToHex(color: $AgbaColor) {
  const rgbaColor = [color.red, color.green, color.blue, color.alpha];
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
export function colorToRgba(color: $AgbaColor) {
  const rgbaColor = [color.red, color.green, color.blue, color.alpha];
  return `rgba(${rgbaColor
    .map((v, index) => {
      if (index === 3) {
        return v / 255;
      }
      return v;
    })
    .join(",")})`;
}
