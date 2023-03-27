// 把 RGB 颜色转为 16进制颜色
export function converRGBAToHexa(r:string, g:string, b:string, a:string){
    let hexaR = parseInt(r).toString(16).toUpperCase()
    let hexaG = parseInt(g).toString(16).toUpperCase()
    let hexaB = parseInt(b).toString(16).toUpperCase()
    let hexaA = parseInt(a).toString(16).toUpperCase()
    hexaR = hexaR.length === 1 ? `0${hexaR}` : hexaR;
    hexaG = hexaG.length === 1 ? `0${hexaG}` : hexaG;
    hexaB = hexaB.length === 1 ? `0${hexaB}` : hexaB;
    hexaA = hexaA.length === 1 ? `0${hexaA}` : hexaA;
    return `#${hexaR}${hexaG}${hexaB}${hexaA}`
}