import mime from "mime";
import fs from "node:fs";
import http from "node:http";
import node_path from "node:path";
import process from "node:process";
import { parse } from "node:url";
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

/**
 * 启动静态文件服务器
 * @param webPublic
 * @param hostname
 * @param port
 */
export const startStaticFileServer = (webPublic: string, hostname: string, port: number) => {
  const baseDir = node_path.resolve(process.cwd(), webPublic);
  http
    .createServer((req, res) => {
      const urlPath = parse(req.url ?? "").pathname ?? "";
      const filePath = node_path.join(baseDir, urlPath === "" || urlPath === "/" ? "index.html" : urlPath);

      fs.readFile(filePath, (err, data) => {
        if (err) {
          if (err.code === "ENOENT") {
            res.writeHead(404, { "Content-Type": "text/plain" });
            res.end("404 Not Found");
          } else {
            res.writeHead(500, { "Content-Type": "text/plain" });
            res.end("Server Error");
          }
          return;
        } else {
          res.setHeader("Access-Control-Allow-Origin", "*");
          res.setHeader("Access-Control-Allow-Headers", "*");
          res.setHeader("Access-Control-Allow-Methods", "*");
          const contentType = mime.getType(filePath);
          if (contentType) {
            res.writeHead(200, { "Content-Type": mime.getType(filePath)! });
          }
          res.end(data);
        }
      });
    })
    .listen(port, hostname, () => {
      console.log(`静态文件服务已启动: http://${hostname}:${port}`);
    })
    .on("error", (err) => {
      console.error(err.cause);
      process.exit(1);
    });
};
