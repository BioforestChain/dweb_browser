import { $Callback, Signal, createSignal } from "../../browser/js-process/module.ts";
import { once } from "../../helper/$once.ts";
import { Channel } from "./Channel.ts";

//#region Producer
/**生产者 */
export class Producer<T> extends Signal<$Callback<[T]>> {
  constructor(readonly name: string) {
    super();
  }

  protected consumers = new Set<Consumer<T>>();
  protected buffers = new Set<Event<T>>();
  /** 保证发送 */
  send(value: T) {
    this.#ensureOpen();
    this.#doSend(value);
  }

  #doSend(value: T) {
    const event = new Event(this.name, value);
    this.buffers.add(event);
    if (this.buffers.size > 10) {
      console.warn(`${this} buffers overflow maybe leak: ${this.buffers.size}`);
    }
    this.doEmit(event);
  }

  /**无保证发送 */
  sendBeacon(value: T) {
    const event = new Event(this.name, value);
    this.doEmit(event);
  }

  trySend(value: T) {
    if (this.isClosedForSend) {
      this.sendBeacon(value);
    } else {
      this.#doSend(value);
    }
  }

  /**触发该消息 */
  doEmit(event: Event<T>) {
    this.#ensureOpen();
    const consumers = this.consumers;
    for (const consumer of consumers) {
      // 如果消费者没启动，或者没有开始读，或者是写通道还是关闭的
      if (!consumer.started || consumer.startingBuffers?.has(event) == true || consumer.input.isClosedForSend) {
        continue;
      }
      event.emitBy(consumer);
      if (event.consumed) {
        break;
      }
    }
  }
  // 创建一个消费者
  consumer(name: string): Consumer<T> {
    this.#ensureOpen();
    return new Consumer(name);
  }

  /**确保打开 */
  #ensureOpen() {
    if (this.#isClosedForSend) {
      throw new Error(`${this} already close for emit.`);
    }
  }

  mapNotNull<R>(transform: (value: T) => R | undefined) {
    const signal = new Signal<$Callback<[R]>>();
    this.listen((value) => {
      const tran = transform(value);
      if (tran !== undefined) {
        signal.emit(tran);
      }
    });

    return signal.listen;
  }

  //#region Close
  #isClosedForSend = false;
  get isClosedForSend() {
    return this.#isClosedForSend;
  }
  /**关闭写 */
  closeWrite() {
    if (this.#isClosedForSend) {
      return;
    }
    this.#isClosedForSend = true;
    const bufferEvents = this.buffers;
    for (const event of bufferEvents) {
      if (!event.consumed) {
        event.consume();
      }
    }
  }
  /**全部关闭 */
  async close(cause?: string) {
    this.closeWrite();
    for (const consumer of this.consumers) {
      consumer.input.close();
    }
    this.consumers.clear();
    this.buffers.clear();
    cause && console.log("producer-close", cause);
  }

  #closeSignal = createSignal<() => unknown>(false);
  onClose = this.#closeSignal.listen;
  //#endregion
}

//#region Event
/**生产者构造的事件*/
class Event<T> extends Producer<T> {
  constructor(name: string, readonly data: T) {
    super(name);
  }

  /**标记事件是否被消耗 */
  #consumed = false;
  get consumed() {
    return this.#consumed;
  }

  /**事件消耗器，调用后事件将被彻底消耗，不会再进行传递*/
  consume(): T {
    if (!this.#consumed) {
      this.#consumed = true;
      this.buffers.delete(this);
    }
    return this.data;
  }

  /**
   * 在 complete 的时候再进行 remove
   */
  complete() {
    this.buffers.delete(this);
  }

  /**向下传递 */
  next() {}
  /**过滤触发 */
  consumeMapNotNull<R extends T>(mapNotNull: (data: T) => R | undefined): R | undefined {
    const result = mapNotNull(this.data);
    if (result !== null) {
      this.consume();
      return result as R;
    }
  }

  emitBy(consumer: Consumer<T>) {
    if (this.#consumed) {
      return;
    }
    // 事件超时告警
    const timeoutId = setTimeout(() => {
      console.warn(`emitBy TIMEOUT!! step=$i consumer=${consumer} data=${this.data}`);
    }, 1000);
    consumer.input.send(this);
    clearTimeout(timeoutId);

    if (this.#consumed) {
      this.complete();
      console.log("emitBy", `consumer=${consumer}`);
    }
  }
}
//#endregion

//#region Consumer
/**消费者 */
export class Consumer<T> extends Producer<T> {
  constructor(name: string, readonly input: Channel<Event<T>> = new Channel()) {
    super(name);
    this.init();
  }

  init() {
    this.consumers.add(this);
    this.onClose(() => {
      this.cancel();
    });
  }
  // 消费过的事件
  startingBuffers: Set<Event<T>> | null = null;
  /**标记是否开始消费 */
  #started = false;
  get started() {
    return this.#started;
  }
  /**开始触发之前的 */
  #start() {
    this.#started = true;
    const strting = this.buffers;
    this.startingBuffers = strting;
    for (const event of strting) {
      event.emitBy(this);
    }
    this.startingBuffers = null;
  }
  #collectors: $FlowCollector<T>[] = [];
  // 收集并触发所有的事件
  #startCollect = once(() => {
    const job = (async () => {
      for await (const event of this.input) {
        for (const collector of this.#collectors) {
          collector(event);
        }
      }
    })();
    this.#start();
    return job;
  });
  /**收集事件 */
  collect(collector: (event: Event<T>) => void) {
    this.#collectors.push(collector);
    // 事件在收集了再调用开始
    return this.#startCollect();
  }

  cancel() {
    this.consumers.delete(this);
  }
}

export type $FlowCollector<T> = (event: Event<T>) => void;
