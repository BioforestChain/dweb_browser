"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.hexaToRGBA = exports.converRGBAToHexa = void 0;
/**
 * 把 RGB 颜色转为 16进制颜色
 * @param r
 * @param g
 * @param b
 * @param a
 * @returns
 */
function converRGBAToHexa(r, g, b, a) {
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
exports.converRGBAToHexa = converRGBAToHexa;
/**
 * 把十六进制的颜色转为  AgbaColor
 * @param str
 */
function hexaToRGBA(str) {
    return {
        red: parseInt(str.slice(1, 3), 16),
        green: parseInt(str.slice(3, 5), 16),
        blue: parseInt(str.slice(5, 7), 16),
        alpha: parseInt(str.slice(7), 16)
    };
}
exports.hexaToRGBA = hexaToRGBA;
