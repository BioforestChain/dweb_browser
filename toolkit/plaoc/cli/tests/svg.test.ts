import { convertSvgToWebp } from "../bundle/verify.ts";

Deno.test("测试 svg to webp", async () => {
  console.log("input", "./img/test.svg");
  const path = new URL("./img/test.svg", import.meta.url);
  console.log(path.pathname);
  await convertSvgToWebp(path.pathname);
});
