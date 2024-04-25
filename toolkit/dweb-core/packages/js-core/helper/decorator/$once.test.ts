import { assert, test } from "vitest";
import { once } from "./$once.ts";

test("@once", () => {
  class A {
    @once()
    get qaq() {
      return Math.random();
    }
  }
  console.log(A.toString());
  const a = new A();
  assert.equal(a.qaq, a.qaq);
});
