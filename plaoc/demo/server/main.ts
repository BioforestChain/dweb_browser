import { ApiRouter } from "@plaoc/server/middleware";

const app = new ApiRouter();

app.use((event) => {
  console.log("server:=>", event.request.url);
});

console.log("init backend");

export default app;
