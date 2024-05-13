Object.assign(globalThis, {
  __pick_web_message_port__: (port: MessagePort) => {
    /**
     * Promise.withResolvers;
     */
    const withResolvers = <T>() => {
      const out = {
        promise: undefined as unknown as Promise<T>,
        resolve: undefined as unknown as (value: T | PromiseLike<T>) => void,
        reject: undefined as unknown as (reason?: unknown) => void,
      };
      out.promise = new Promise((resolve_, reject_) => {
        out.resolve = resolve_;
        out.reject = reject_;
      });
      return out;
    };
    type Msg = MessageEvent | string;
    const messages = Object.assign(Object.create(null) as Record<number, Msg>, {
      length: 0,
      push: (item: Msg) => {
        messages[messages.length++] = item;
      },
    });
    const waiters: ReturnType<typeof withResolvers<Msg>>[] = [];
    /// 允许重新获取，但是不可 seek
    Reflect.set(port, "pickEvent", (i: number) => {
      delete messages[i - 1];
      const event = messages[i];
      if (event !== undefined) {
        return event;
      }
      const po = withResolvers<Msg>();
      waiters.push(po);
      return po.promise;
    });

    Object.assign(port, { messages, waiters });
    const listener = (event: MessageEvent) => {
      messages.push(event);

      const waiter = waiters.shift();
      if (waiter) {
        waiter.resolve(event);
      }
    };
    port.addEventListener("message", listener);
    port.start();
    const port_close = port.close;
    let closed = false;
    port.close = () => {
      if (closed) {
        return;
      }
      closed = true;
      messages.push("close for send");
      // port.removeEventListener("message", listener);
      for (const po of waiters) {
        po.resolve("close for send");
      }
      waiters.length = 0;
      // 最后执行原生的关闭
      // 这里会同时关闭读取与写入，所以即便消息还没被接收完成，也意味着将会被丢弃
      port_close.call(port);
    };
  },
});
