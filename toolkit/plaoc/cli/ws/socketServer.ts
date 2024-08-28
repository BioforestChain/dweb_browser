import { debounce } from "jsr:@std/async/debounce";
import type { WebSocketServer as $WebSocketServer } from "npm:@types/ws";
import { WebSocketServer } from "npm:ws";
import { fileURLToPath, node_fs, node_https, node_path } from "../deps/node.ts";

/**创建websocket服务器，该端口从静态端口中加1 */
export const createListenScoket = (hostname: string, port: number, baseDir: string) => {
  // 获取入口脚本的绝对路径
  const entryDir = node_path.dirname(fileURLToPath(import.meta.url));
  // 构建相对于入口脚本目录的路径
  const certPath = node_path.join(entryDir, "./cert.pem");
  const keyPath = node_path.join(entryDir, "./key.pem");
  // 创建 WebSocket 服务器，依赖现有的 HTTP 服务器
  const wss: $WebSocketServer = new WebSocketServer({ noServer: true });
  //监听文件服务变化
  watcherChange(baseDir, wss);
  // 监听 WebSocket 连接
  wss.on("connection", () => {
    // console.log("WebSocket 客户端已连接");
  });

  // 创建一个 HTTP 服务器
  const server = node_https.createServer(
    {
      cert: node_fs.readFileSync(certPath),
      key: node_fs.readFileSync(keyPath),
    },
    (_req, res) => {
      res.writeHead(200);
      res.end("Hello World");
    }
  );

  // 处理 WebSocket 升级请求
  server.on("upgrade", (request, socket, head) => {
    wss.handleUpgrade(request, socket, head, (ws) => {
      wss.emit("connection", ws, request);
    });
  });

  server.listen(port + 1, hostname, () => {
    // const address = server.address();
    // if (typeof address === "string") {
    //   console.log(`WebSocket server is running at ${address}`);
    // } else if (address !== null) {
    //   console.log(`WebSocket server is running at ws://${address.address}:${address.port}`);
    // }
  });

  return wss;
};

/**监听文件服务变化 */
export const watcherChange = (baseDir: string, wss: $WebSocketServer) => {
  // 监听文件变化，如果入口文件变化则重新加载
  let isReloading = false;
  const reload = debounce(() => {
    if (!isReloading) {
      isReloading = true;
      // console.log("size", wss.clients.size);
      wss.clients.forEach((client) => {
        // console.log("send", client.url);
        if (client.readyState === WebSocket.OPEN) {
          client.send("reload");
        }
      });
      // 在1秒后重置标志，允许下一次的文件变化触发消息
      setTimeout(() => {
        isReloading = false;
      }, 2000);
    }
  }, 1000);
  (async () => {
    const watcher = Deno.watchFs(baseDir);
    for await (const _event of watcher) {
      reload();
    }
  })();
};

// openssl req -x509 -newkey rsa:4096 -keyout key.pem -out cert.pem  -nodes
