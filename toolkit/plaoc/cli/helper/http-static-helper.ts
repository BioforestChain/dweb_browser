import mime from "mime";
import fs from "node:fs";
import type http from "node:http";
import node_path from "node:path";
export const getMimeType = (name: string) => {
  return mime.getType(name) || "application/octet-stream";
};
const html = String.raw;
/**
 * 提供静态文件服务
 * @param dir
 * @param req
 * @param res
 * @returns
 */
// deno-lint-ignore require-await
export const staticServe = async (dir: string, req: http.IncomingMessage, res: http.ServerResponse) => {
  try {
    const filepath = node_path.join(dir, req.url || "/");
    if (fs.existsSync(filepath)) {
      if (fs.statSync(filepath).isFile()) {
        console.log("file:", filepath);
        fs.createReadStream(filepath).pipe(res as NodeJS.WritableStream);
      } else {
        res.setHeader("Content-Type", "text/html");
        res.end(
          html`<ol>
            ${fs
              .readdirSync(filepath)
              .map((name) => {
                const is_dir = fs.statSync(filepath + "/" + name).isDirectory();
                return html`<li>
                  ${is_dir ? html`<code> > </code>` : ""}
                  <a href="./${name}${is_dir ? "/" : ""}">${name}</a>
                </li>`;
              })
              .join("")}
          </ol>`
        );
      }
      res.setHeader("Content-Type", getMimeType(filepath));
      return true;
    }
  } catch (err) {
    res.write(err?.message ?? String(err));
    return false;
  }

  res.statusCode = 404;
  res.end();
};
