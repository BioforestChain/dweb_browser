import http from "node:http";

export class ReqResCtx {
  #req;
  #res;
  constructor(req, res) {
    this.#req = req;
    this.#res = res;
  }
  get url() {
    return this.#req.url;
  }
  get headers() {
    return this.#req.headers;
  }
  setHeader(key, value) {
    this.#res.setHeader(key, value);
  }
  write(data) {
    this.#res.write(data);
  }
  end() {
    this.#res.end();
  }
}

export function createHttpServer(port = 0, onReq) {
  return new Promise((resolve, reject) => {
    const server = http
      .createServer((req, res) => {
        onReq(new ReqResCtx(req, res));
      })
      .on("error", reject)
      .on("close", () => {
        console.log("closed");
      })
      .listen({ port, host: "0.0.0.0" }, () => {
        const address = server.address();
        resolve(address.port);
        // console.log(address);
      });
  });
}
