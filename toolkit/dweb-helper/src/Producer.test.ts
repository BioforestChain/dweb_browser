import { setTimeout } from "node:timers/promises";
import { assert, test } from "vitest";
import { Producer } from "./Producer.ts";

test("producer & consumer", async () => {
  const producer = new Producer<number>("qaq", { bufferOverflowBehavior: "throw" });
  const input = Array.from({ length: 5 }, (_, i) => i);
  console.log("start send-head");
  for (const value of input) {
    await producer.send(value);
  }
  console.log("end send-head");

  console.log("start consumer");
  const consumer = producer.consumer("test1");
  const output: number[] = [];
  await consumer.collect((event) => {
    output.push(event.consume());
  });
  console.log("end consumer");

  console.log("start send-body");
  for (let index = 0; index < 20; index++) {
    input.push(index);
    await producer.send(index);
  }
  console.log("end send-body");

  await setTimeout(100);

  assert.deepEqual(output, input);
});
