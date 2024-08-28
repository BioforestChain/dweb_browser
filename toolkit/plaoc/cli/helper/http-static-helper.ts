import mime from "mime";
import { colors } from "../deps/cliffy.ts";
import { node_fs, node_http, node_os, node_path, node_process } from "../deps/node.ts";
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
export const staticServe = (dir: string, req: node_http.IncomingMessage, res: node_http.ServerResponse) => {
  try {
    const filepath = node_path.join(dir, req.url || "/");
    if (node_fs.existsSync(filepath)) {
      if (node_fs.statSync(filepath).isFile()) {
        console.log("file:", filepath);
        node_fs.createReadStream(filepath).pipe(res as NodeJS.WritableStream);
      } else {
        res.setHeader("Content-Type", "text/html");
        res.end(
          html`<ol>
            ${node_fs
              .readdirSync(filepath)
              .map((name) => {
                const is_dir = node_fs.statSync(filepath + "/" + name).isDirectory();
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
export const startStaticFileServer = (
  baseDir: string,
  hostname: string,
  port: number,
  callback: (address: string) => void
) => {
  // 准备注入到html的刷新代码
  const connectHtml = html`<script>
    const ws = new WebSocket("wss://${hostname}:${port + 1}");
    ws.onmessage = (event) => {
      if (event.data === "reload") {
        console.log("检测到文件变化，刷新页面...");
        location.reload();
      }
    };
  </script>`;
  const server = node_http
    .createServer((req, res) => {
      staticFactory(connectHtml, baseDir, req, res);
    })
    .listen(port, "0.0.0.0", () => {
      const host = `http://${hostname}:${port}`;
      console.log("Listen to the service address：", colors.gray(host));
      console.log("The folder for the listener is located at:", colors.gray(baseDir));
      callback(host);
    })
    .on("error", (err) => {
      console.error("Server encountered an error:", err.message);
      node_process.exit(1);
    });
  return server;
};

/**静态文件服务读取工厂 */
const staticFactory = (
  connectHtml: string,
  baseDir: string,
  req: node_http.IncomingMessage,
  res: node_http.ServerResponse
) => {
  //去掉后面的其他参数，防止read不到文件
  const requestUrl = new URL(req.url || "/", `http://${req.headers.host}`);
  const urlPath = requestUrl.pathname;
  const filePath = node_path.join(baseDir, urlPath === "/" ? "index.html" : urlPath);
  // 检查文件是否存在并发送响应头
  node_fs.stat(filePath, (err, stats) => {
    if (err) {
      if (err.code === "ENOENT") {
        res.writeHead(404, { "Content-Type": "text/plain" });
        res.end("404 Not Found");
      } else {
        res.writeHead(500, { "Content-Type": "text/plain" });
        res.end("Server Error");
      }
      return;
    }
    // 创建文件读取流，并通过管道传输给响应对象
    const contentType = mime.getType(filePath) || "application/octet-stream";
    // 如果是 HTML 文件，创建一个 Transform 流来插入 WebSocket 脚本
    if (contentType === "text/html") {
      let data = node_fs.readFileSync(filePath, "utf-8");
      // 将脚本插入到 `</body>` 标签前面
      data = data.replace(/<\/body>/, `${connectHtml}</body>`);
      res.setHeader("Content-Type", contentType);
      return res.end(data);
    }
    // 创建文件的 ETag
    const fileStream = node_fs.createReadStream(filePath);
    res.writeHead(200, {
      "Content-Type": contentType,
      "Content-Length": stats.size,
      "Access-Control-Allow-Origin": "*",
      "Access-Control-Allow-Headers": "*",
      "Access-Control-Allow-Methods": "*",
    });
    // 处理读取流中的错误
    fileStream.on("error", (streamErr) => {
      console.error("Stream Error:", streamErr);
      res.writeHead(500, { "Content-Type": "text/plain" });
      res.end("ReadStream Server Error");
    });
    // 将读取流的数据直接管道到响应
    fileStream.pipe(res);
  });
};

/** 获取服务IP */
export const getLocalIP = () => {
  for (const netInterface of Object.values(node_os.networkInterfaces())) {
    if (netInterface)
      for (const addr of netInterface) {
        if (addr.family === "IPv4" && !addr.address.startsWith("127")) {
          return addr.address;
        }
      }
  }
  return "0.0.0.0";
};
