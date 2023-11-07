import type { Router } from "./middlewares/router.ts";

// 导入中间件
export class MiddlewareImporter {
  static async init(path ?:string) {
    if(!path){
      return []
    }
    try {
      const middleware = await import(path);
      console.log("middleware=>", middleware);
      return (middleware.default as Router).handlers;
    } catch (e) {
      console.log("MiddlewareConfig=>", e);
    }
  }
}
