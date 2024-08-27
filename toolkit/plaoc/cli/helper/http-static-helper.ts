import { debounce } from "jsr:@std/async/debounce";
import mime from "mime";
import fs from "node:fs";
import http from "node:http";
import os from "node:os";
import node_path from "node:path";
import process from "node:process";
import { WebSocketServer } from "npm:ws";
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
export const staticServe = (dir: string, req: http.IncomingMessage, res: http.ServerResponse) => {
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
export const startStaticFileServer = (webPublic: string, port: number, callback: (address: string) => void) => {
  const baseDir = node_path.resolve(process.cwd(), webPublic);
  const hostname = getLocalIP();

  // 准备注入数据
  const html = String.raw;
  const connectHtml = html`<script>
    const ws = new WebSocket("ws://${hostname}:${port + 1}");
    ws.onmessage = (event) => {
      if (event.data === "reload") {
        console.log("检测到文件变化，刷新页面...");
        location.reload();
      }
    };
  </script>`;
  const server = http
    .createServer((req, res) => {
      staticFactory(connectHtml, baseDir, req, res);
    })
    .listen(port, "0.0.0.0", () => {
      const host = `http://${hostname}:${port}`;
      console.log("Listen to the service address：", host);
      console.log("Serving files from:", baseDir);
      callback(host);
    })
    .on("error", (err) => {
      console.error("Server encountered an error:", err.message);
      process.exit(1);
    });
  // 启动socket
  socketHandle(hostname, port, baseDir);

  return server;
};

/**监听文件改变重新加载 */
const socketHandle = (hostname: string, port: number, baseDir: string) => {
  // 创建 WebSocket 服务器，依赖现有的 HTTP 服务器
  const wss = new WebSocketServer({ noServer: true });
  // 监听 WebSocket 连接
  wss.on("connection", () => {
    console.log("WebSocket 客户端已连接");
  });
  // 创建一个 HTTP 服务器
  const server = http.createServer((_req, res) => {
    res.writeHead(200);
    res.end("Hello World");
  });

  // 处理 WebSocket 升级请求
  server.on("upgrade", (request, socket, head) => {
    wss.handleUpgrade(request, socket, head, (ws: WebSocket) => {
      wss.emit("connection", ws, request);
    });
  });
  server.listen(port + 1, hostname, () => {
    const address = server.address();
    console.log(`WebSocket server is running at ${address}`);
  });
  // 监听文件变化，如果入口文件变化则重新加载
  const reload = debounce(() => {
    wss.clients.forEach((client: WebSocket) => {
      console.log("文件变化");
      if (client.readyState === WebSocket.OPEN) {
        client.send("reload");
      }
    });
  }, 500);

  async () => {
    const watcher = Deno.watchFs(baseDir);
    for await (const _event of watcher) {
      reload();
    }
  };
  return wss;
};

const staticFactory = (connectHtml: string, baseDir: string, req: http.IncomingMessage, res: http.ServerResponse) => {
  //去掉后面的其他参数，防止read不到文件
  const requestUrl = new URL(req.url || "/", `http://${req.headers.host}`);
  const urlPath = requestUrl.pathname;
  const filePath = node_path.join(baseDir, urlPath === "/" ? "index.html" : urlPath);
  // 检查文件是否存在并发送响应头
  fs.stat(filePath, (err, stats) => {
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
      let data = fs.readFileSync(filePath, "utf-8");
      // 将脚本插入到 `</body>` 标签前面
      data = data.replace(/<\/body>/, `${connectHtml}</body>`);
      res.setHeader("Content-Type", contentType);
      return res.end(data);
    }
    // 创建文件的 ETag
    const fileStream = fs.createReadStream(filePath);
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
  for (const netInterface of Object.values(os.networkInterfaces())) {
    if (netInterface)
      for (const addr of netInterface) {
        if (addr.family === "IPv4" && !addr.address.startsWith("127")) {
          return addr.address;
        }
      }
  }
  return "0.0.0.0";
};

// // 获取入口脚本的绝对路径
// const entryDir = node_path.dirname(fileURLToPath(import.meta.url));
// // 构建相对于入口脚本目录的路径
// const certPath = node_path.join(entryDir, "ws/cert.pem");
// const keyPath = node_path.join(entryDir, "ws/key.pem"); // openssl req -x509 -newkey rsa:4096 -keyout key.pem -out cert.pem -days 365 -nodes

// {
//   cert: fs.readFileSync(certPath),
//   key: fs.readFileSync(keyPath),
// },
