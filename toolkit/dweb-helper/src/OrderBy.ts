import { mapHelper } from "./fun/mapHelper.ts";

export class OrderInvoker {
  #queues = new Map<number, QueuePromise>();
  tryInvoke<R>(order: number | undefined, action: () => R): R | Promise<Awaited<R>>;
  tryInvoke<R>(order: object | undefined, action: () => R): R | Promise<Awaited<R>>;
  tryInvoke<R>(order: unknown, action: () => R) {
    if (typeof order === "object" && order !== null) {
      if ("order" in order && typeof order.order === "number") {
        order = order.order;
      } else {
        order = undefined;
      }
    }
    if (typeof order === "number") {
      return mapHelper.getOrPut(this.#queues, order, () => new QueuePromise()).queue(action);
    }
    return action();
  }
}

export class QueuePromise {
  constructor(private current?: Promise<unknown>) {}
  queue<R>(handler: () => R) {
    const pre = this.current;
    const next = (async () => {
      if (pre) {
        await pre;
      }
      return handler();
    })();
    this.current = next;
    next.finally(() => {
      if (this.current === next) {
        this.current = undefined;
      }
    });
    return next as Promise<Awaited<R>>;
  }
}

export type OrderBy = {
  [key: string]: unknown;
  order: number | undefined;
};
