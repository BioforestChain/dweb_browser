/**
 * 把 RGB 颜色转为 16进制颜色
 * @param r
 * @param g
 * @param b
 * @param a
 * @returns
 */
export declare function converRGBAToHexa(r: string, g: string, b: string, a: string): string;
/**
 * 把十六进制的颜色转为  AgbaColor
 * @param str
 */
export declare function hexaToRGBA(str: string): $AgbaColor;
export type $AgbaColor = {
    red: number;
    green: number;
    blue: number;
    alpha: number;
};
