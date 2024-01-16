import { createHttpServer } from "./http-server.mjs";
createHttpServer(8082, (ctx) => {
  console.log(ctx.url);
  ctx.write("hi~" + ctx.url);
  ctx.end();
});
