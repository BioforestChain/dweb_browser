import fs from "node:fs";
import http from "node:http";
import path from "node:path";
import mime from "npm:mime";
export const getMimeType = (name: string) => {
  return mime.getType(name) || "application/octet-stream";
};
/**
 * 提供静态文件服务
 * @param dir
 * @param req
 * @param res
 * @returns
 */
export const staticServe = async (dir: string, req: http.IncomingMessage, res: http.ServerResponse) => {
  try {
    const filepath = path.join(dir, req.url || "/");
    if (fs.existsSync(filepath)) {
      if (fs.statSync(filepath).isFile()) {
        console.log("file:", filepath);
        fs.createReadStream(filepath).pipe(res as NodeJS.WritableStream);
      } else {
        res.setHeader("Content-Type", "text/html");
        res.end(
          `<ol>${fs
            .readdirSync(filepath)
            .map((name) => {
              const is_dir = fs.statSync(filepath + "/" + name).isDirectory();
              return `<li>
                ${is_dir ? `<span> > </span>` : ""}
                <a href="./${name}${is_dir ? "/" : ""}">${name}</a>
              </li>`;
            })
            .join("")}</ol>`
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
