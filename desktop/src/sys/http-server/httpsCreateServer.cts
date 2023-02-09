import https from "node:https";
export const httpsCreateServer = (
  options: https.ServerOptions,
  listenOptions: { port: number; hostname?: string }
) => {
  return new Promise<{
    origin: string;
    server: https.Server;
  }>((resolve, reject) => {
    const { port, hostname = "localhost" } = listenOptions;
    const server = https
      .createServer(options)
      .on("error", reject)
      .listen(port, hostname, () => {
        const origin = `https://${hostname}:${port}`;
        resolve({ origin, server });
      });
  });
};
