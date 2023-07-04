import type { WebServerRequest, WebServerResponse } from "./types.ts";
const html = String.raw;
export const formatErrorToHtml = (
  statusCode: number,
  url: string = "/",
  method: string = "GET",
  headers: Record<string, string | string[] | undefined> = {},
  err: unknown = "Unknown Error",
  detail?: string
) => {
  let errorMessage = "";
  let detailMessage = detail;
  if (err instanceof Error) {
    errorMessage = err.message;
    detailMessage ??= err.stack ?? "";
  } else {
    errorMessage = String(err);
  }
  return html`
    <!DOCTYPE html>
    <html>
      <head>
        <meta charset="UTF-8" />
        <meta http-equiv="X-UA-Compatible" content="IE=edge" />
        <meta name="viewport" content="width=device-width, initial-scale=1.0" />
        <title>${statusCode}</title>
      </head>
      <body>
        <h1 style="color:red">[${statusCode}] ${errorMessage}</h1>
        ${detailMessage ? html`<blockquote>${detailMessage}</blockquote>` : ""}
        <div>
          <h2>URL:</h2>
          <pre>${url}</pre>
        </div>
        <div>
          <h2>METHOD:</h2>
          <pre>${method}</pre>
        </div>
        <div>
          <h2>HEADERS:</h2>
          <pre>${JSON.stringify(headers, null, 2)}</pre>
        </div>
      </body>
    </html>
  `;
};

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
export const defaultErrorResponse = (
  req: WebServerRequest,
  res: WebServerResponse,
  statusCode: number,
  errorMessage: string,
  detailMessage?: string
) => {
  const body = formatErrorToHtml(
    statusCode,
    req.url,
    req.method,
    req.headers,
    errorMessage,
    detailMessage
  );

  res.statusCode = statusCode;
  res.statusMessage = errorMessage;
  res.end(body);
  return body;
};
