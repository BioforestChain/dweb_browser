import type { $OnFetch } from "npm:@dweb-browser/js-process";

export const WWW_onFetchHandlers: $OnFetch[] = [];
export const API_onFetchHandlers: $OnFetch[] = [];
export const EXTERNAL_onFetchHandlers: $OnFetch[] = [];
export const defineWWWOnFetch = (...handlers: $OnFetch[]) => {
  WWW_onFetchHandlers.push(...handlers);
};

export const defineAPIOnFetch = (...handlers: $OnFetch[]) => {
  API_onFetchHandlers.push(...handlers);
};

export const defineEXTERNALOnFetch = (...handlers: $OnFetch[]) => {
  EXTERNAL_onFetchHandlers.push(...handlers);
};

// 导入中间件
// export class MiddlewareConfig {
//   constructor() {}
//   static async init() {
//     try {
//       const path = "./middleware/main.js";
//       const middleware = await import(path);
//       console.log("middleware=>", middleware);
//       return middleware;
//     } catch (e) {
//       console.log("MiddlewareConfig=>", e);
//     }
//   }
// }
