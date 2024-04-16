import { $Callback, Signal, createSignal } from "../../browser/js-process/module.ts";
import { once } from "../../helper/$once.ts";
import { Channel } from "./Channel.ts";

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

/**生产者构造的事件*/
class Event<T> extends Producer<T> {
  constructor(name: string, readonly data: T) {
    super(name);
  }

  /**标记事件是否被消耗 */
  private _consumed = false;
  get consumed() {
    return this._consumed;
  }

  /**事件消耗器，调用后事件将被彻底消耗，不会再进行传递*/
  consume(): T {
    if (!this._consumed) {
      this._consumed = true;
      this.buffers.delete(this);
    }
    return this.data;
  }
  /**向下传递 */
  next() {}

  /**将其消耗转换为R对象 以返回值形式继续传递*/
  consumeAs<R extends T>(c: new () => R): R | null {
    if (this.data instanceof c) {
      return this.consume() as R;
    }
    return null;
  }

  /**过滤触发 */
  consumeFilter(filter: (data: T) => boolean): T | null {
    if (filter(this.data)) {
      return this.consume();
    }
    return null;
  }
  /**将其消耗转换为R对象 以回调形式继续传递*/
  consumeAsCallback<R extends T>(c: new () => R, block: (data: R) => void) {
    if (this.data instanceof c) {
      block(this.consume() as R);
    }
  }

  emitBy(consumer: Consumer<T>) {
    if (this._consumed) {
      return;
    }
    // 事件超时告警
    const timeoutId = setTimeout(() => {
      console.warn(`emitBy TIMEOUT!! step=$i consumer=${consumer} data=${this.data}`);
    }, 1000);

    clearTimeout(timeoutId);
  }
}

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
  #collectors: FlowCollector<Event<T>>[] = [];
  // 收集并触发所有的事件
  #startCollect = once(() => {
    (async () => {
      for await (const event of this.input) {
        for (const collector of this.#collectors) {
          collector.emit(event);
        }
      }
    })();
    this.#start();
  });

  collect(collector: FlowCollector<Event<T>>) {
    this.#collectors.push(collector);
    // 事件在收集了再调用开始
    this.#startCollect();
  }

  cancel() {
    this.consumers.delete(this);
  }
}

class FlowCollector<T> {
  #signal = createSignal<$Callback<[T]>>();
  emit = this.#signal.emit;
  listen = this.#signal.listen;
}
