import type { WebServerRequest, WebServerResponse } from "./types.js";
/**
 * 这是默认的错误页
 *
 * /// TODO 将会开放接口来让开发者可以自定义错误页的模板内容，也可以基于错误码范围进行精确匹配
 *
 * @param req
 * @param res
 * @param statusCode
 * @param errorMessage
 * @returns
 */
export declare const defaultErrorResponse: (req: WebServerRequest, res: WebServerResponse, statusCode: number, errorMessage: string, detailMessage?: string) => string;
