import { Router } from "@plaoc/server/middleware";

const app = new Router();

app.use((event) => {
  console.log("external:=>", event.request.url);
});

console.log("init backend");

export default app;
