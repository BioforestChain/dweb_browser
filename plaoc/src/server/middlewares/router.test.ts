import { assertEquals } from "https://deno.land/std@0.205.0/assert/mod.ts";
import { Router } from "./index.ts";

Deno.test("test apiRouter", () => {
  const api = new Router();
  api.use((event) => {
    console.log(event.pathname);
  });
  assertEquals(api.handlers.length, 1);
});
