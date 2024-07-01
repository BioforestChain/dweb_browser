import { Router, jsProcess } from "@plaoc/server/middlewares";

const app = new Router();

app.use(async (event) => {
  console.log("api server:=>", event.request.url, jsProcess.mmid);
  if (event.url.pathname.includes("/barcode-scanning")) {
    const response = await jsProcess.nativeFetch("file://barcode-scanning.sys.dweb/process", {
      method: event.method,
      body: event.body,
    });
    if (response.ok) {
      return response;
    } else {
      return Response.json(`decode error:${await response.text()}`);
    }
  }
});

export default app;
