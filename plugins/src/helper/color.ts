import { Color } from "../types/color.d.ts";

/**
 * 将rgba(r, b, g, a)或#rrbbggaa或#rgba转为#rrbbggaa 十六进制
 * @param color 
 * @returns  #rrbbggaa
 */
export function convertToRGBAHex(color: string): Color.RGBAHex {
  let colorHex = "#";

  if (color.startsWith("rgba(")) {
    const colorArr = color.replace("rgba(", "").replace(")", "").split(",");

    for (let [index, item] of colorArr.entries()) {
      if (index === 3) {
        item = `${parseFloat(item) * 255}`;
      }
      let itemHex = Math.round(parseFloat(item)).toString(16);

      if (itemHex.length === 1) {
        itemHex = "0" + itemHex;
      }

      colorHex += itemHex;
    }
  }
  if (color.startsWith("#")) {
    if (color.length === 9) {
      colorHex = color;
    } else {
      color = color.substring(1);
      // 如果是 #f71 或者#f72e这种格式的话,转换为5字符格式
      if (color.length === 4 || color.length === 3) {
        color = color.replace(/(.)/g, "$1$1");
      }
      // 填充成9字符格式，不然android无法渲染
      colorHex += color.padEnd(8, "F");
    }
  }
  return colorHex as Color.RGBAHex;
}
