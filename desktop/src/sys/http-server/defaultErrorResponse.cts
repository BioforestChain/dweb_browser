import type { WebServerRequest, WebServerResponse } from "./types.cjs";
const html = String.raw;

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
  const body = html`
    <!DOCTYPE html>
    <html>
      <head>
        <meta charset="UTF-8" />
        <meta http-equiv="X-UA-Compatible" content="IE=edge" />
        <meta name="viewport" content="width=device-width, initial-scale=1.0" />
        <title>${statusCode}</title>
      </head>
      <body>
        <h1 style="color:red">[${statusCode}] ${res.statusMessage}</h1>
        ${detailMessage ? html`<blockquote>${detailMessage}</blockquote>` : ""}
        <div>
          <h2>URL:</h2>
          <pre>${req.url}</pre>
        </div>
        <div>
          <h2>METHOD:</h2>
          <pre>${req.method}</pre>
        </div>
        <div>
          <h2>HEADERS:</h2>
          <pre>${JSON.stringify(req.headers, null, 2)}</pre>
        </div>
      </body>
    </html>
  `;

  res.statusCode = statusCode;
  res.statusMessage = errorMessage;
  res.end(body);
  return body;
};
