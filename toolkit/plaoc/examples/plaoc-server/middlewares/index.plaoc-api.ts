import { Router } from "@plaoc/server/middlewares";

const app = new Router();

app.use((event) => {
  console.log("api server:=>", event.request.url);
});

console.log("可编程后端加载成功", "api init backend");

export default app;
