import { assertEquals } from "https://deno.land/std@0.205.0/assert/mod.ts";
import { API_onFetchHandlers } from "../middleware-config.ts";
import { ApiRouter } from "./index.ts";

Deno.test("test apiRouter", () => {
  const api = new ApiRouter();
  api.use((event) => {
    console.log(event.pathname);
  });
  assertEquals(API_onFetchHandlers.length, 1);
});
