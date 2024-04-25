import { Router } from "@plaoc/server/middleware";
const app = new Router();

app.use((event) => {
  console.log("www server:=>", event.request.url);
});

console.log("www init backend");

export default app;
