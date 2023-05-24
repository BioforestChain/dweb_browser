import assert from "node:assert";
import test from "node:test";
import { setTimeout } from "node:timers/promises";
import { Mutex } from "./Mutex.ts";
const DUR = 100;

test("Mutex queue", async () => {
  const mutex = new Mutex();
  let step = 0;
  mutex.withLock(async () => {
    assert.equal(step, 0);
    step = 1;
    await setTimeout(DUR);
  });
  await mutex.withLock(async () => {
    assert.equal(step, 1);
    step = 2;
    await setTimeout(DUR);
  });
  assert.equal(step, 2);
});
test("Mutex isLocked", async (t) => {
  const mutex = new Mutex();
  assert.equal(mutex.isLocked, false);
  const task = mutex.withLock(async () => {
    assert.equal(mutex.isLocked, true);
    await setTimeout(DUR);
  });
  assert.equal(mutex.isLocked, true);
  await task;
  assert.equal(mutex.isLocked, false);
});
test("Mutex lock start", async () => {
  const mutex = new Mutex(true);
  setTimeout(DUR * 1.5).then(() => {
    assert.equal(step, -1);
    step = 0;
    mutex.unlock();
  });
  assert.equal(mutex.isLocked, true);
  let step = -1;
  mutex.withLock(async () => {
    assert.equal(step, 0);
    step = 1;
    await setTimeout(DUR);
  });
  await mutex.withLock(async () => {
    assert.equal(step, 1);
    step = 2;
    await setTimeout(DUR);
  });
  assert.equal(step, 2);
  assert.equal(mutex.isLocked, false);
});
