import { Router } from "@plaoc/server/middleware";

const app = new Router();

app.use((event) => {
  console.log("api server:=>", event.request.url);
});

console.log("api init backend");

export default app;
