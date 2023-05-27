import { parse } from "https://deno.land/std@0.184.0/flags/mod.ts";
import fs from "node:fs";
import http from "node:http";
import os from "node:os";
import path from "node:path";
import JSZip from "npm:jszip";
import mime from "npm:mime";
import { WalkDir } from "../../scripts/helper/WalkDir.ts";
const flags = parse(Deno.args, {
  string: ["port", "dir", "name"],
  default: { port: 8096 },
});

const port = +flags.port;

if (Number.isFinite(port) === false) {
  throw new Error(`need input '--port 8080'`);
}

const dir = flags.dir;
if (
  !dir ||
  fs.existsSync(dir) === false ||
  fs.statSync(dir).isDirectory() === false
) {
  throw new Error(`need input '--dir your/folder/for/zip'`);
}

const name = flags.name || path.basename(dir) + ".zip";

http
  .createServer((req, res) => {
    if (req.url === "/" + name) {
      const zip = new JSZip();
      for (const entry of WalkDir(dir)) {
        zip.file(entry.relativepath, entry.read());
      }
      res.setHeader(
        "Content-Type",
        mime.getType(name) || "application/octet-stream"
      );
      zip
        .generateNodeStream({ compression: "STORE" })
        .pipe(res as NodeJS.WritableStream);
    } else {
      try {
        const filepath = path.join(dir, req.url || "/index.html");
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
                  const is_dir = fs
                    .statSync(filepath + "/" + name)
                    .isDirectory();
                  return `<li>${
                    is_dir ? `<span> > </span>` : ""
                  }<a href="./${name}${is_dir ? "/" : ""}">${name}</a></li>`;
                })
                .join("")}</ol>`
            );
          }
          res.setHeader(
            "Content-Type",
            mime.getType(filepath) || "application/octet-stream"
          );
          return;
        }
      } catch (err) {
        res.write(err?.message ?? String(err));
      }
      res.statusCode = 404;
      res.end();
    }
  })
  .listen(port, () => {
    for (const info of Object.values(os.networkInterfaces())
      .flat()
      .filter((info) => info?.family === "IPv4")) {
      console.log(`open http://${info?.address}:${port}/${name}`);
    }
  });
