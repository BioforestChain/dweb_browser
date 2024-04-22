import { $once } from "../../helper/$once.ts";
import { Mutex } from "../../helper/Mutex.ts";
import { PromiseOut } from "../../helper/PromiseOut.ts";
import { Signal, createSignal, type $Callback } from "../../helper/createSignal.ts";
import { logger } from "../../helper/logger.ts";
import { Channel } from "./Channel.ts";

type Event<T> = ReturnType<Producer<T>["event"]>;
type Consumer<T> = ReturnType<Producer<T>["consumer"]>;
type $FlowCollector<T> = (event: Event<T>) => void;
//#region Producer
/**生产者 */
export class Producer<T> {
  constructor(readonly name: string) {}
  toString() {
    return `Producer<${this.name}>`;
  }
  console = logger(this);
  //#region Event
  static #Event = class Event<T> {
    constructor(readonly data: T, private producer: Producer<T>) {}
    #job = new PromiseOut<void>();
    get job() {
      return this.#job.promise;
    }

    /**标记事件是否被消耗 */
    #consumed = false;
    get consumed() {
      return this.#consumed;
    }

    /**
     * 事件消耗器，调用后事件被标记成已消耗，这意味着：
     * 1. 现有的消费者还是能看到它
     * 2. 其它后来的消费者不会再看到它
     */
    consume(): T {
      this.#consumed = true;
      return this.data;
    }

    /**
     * 事件停止传播，调用后事件不会被其他消费者触发
     */
    #stoped = false;
    get stoped() {
      return this.#stoped;
    }
    stopImmediatePropagation() {
      this.consume();
      this.#stoped = true;
    }

    /**
     * 在 complete 的时候再进行 remove
     */
    complete() {
      this.#job.resolve();
      this.producer.buffers.delete(this);
    }
    /**过滤触发 */
    consumeMapNotNull<R>(mapNotNull: (data: T) => R | undefined): R | undefined {
      const result = mapNotNull(this.data);
      if (result !== null) {
        this.consume();
        return result as R;
      }
    }
    async consumeAwaitMapNotNull<R>(mapNotNull: (data: T) => R | undefined) {
      const result = await mapNotNull(this.data);
      if (result !== null) {
        this.consume();
        return result as Awaited<R>;
      }
    }
    consumeFilter<R extends T = T>(filter: (data: T) => boolean) {
      if (filter(this.data)) {
        return this.consume() as R;
      }
    }

    #emitLock = new Mutex();
    emitBy(consumer: Consumer<T>) {
      if (this.#stoped) {
        return;
      }
      return this.#emitLock.withLock(() => {
        // 事件超时告警
        const timeoutId = setTimeout(() => {
          console.warn(`emitBy TIMEOUT!! step=$i consumer=${consumer} data=${this.data}`);
        }, 1000);
        consumer.input.send(this);
        clearTimeout(timeoutId);

        if (this.#consumed) {
          this.complete();
          this.producer.console.debug("emitBy", `event=${this} consumed by consumer=${consumer}`);
        }
      });
    }
  };

  private event(data: T) {
    /**生产者构造的事件*/
    return new Producer.#Event<T>(data, this);
  }
  //#endregion

  protected consumers = new Set<Consumer<T>>();
  protected buffers = new Set<Event<T>>();
  /** 保证发送 */
  send(value: T) {
    this.#ensureOpen();
    this.#doSend(value);
  }

  #doSend(value: T) {
    const event = this.event(value);
    this.buffers.add(event);
    if (this.buffers.size > 10) {
      this.console.warn(`${this} buffers overflow maybe leak: ${this.buffers.size}`);
    }
    return this.doEmit(event);
  }

  /**无保证发送 */
  sendBeacon(value: T) {
    const event = this.event(value);
    return this.doEmit(event);
  }

  trySend(value: T) {
    if (this.isClosedForSend) {
      this.sendBeacon(value);
    } else {
      this.#doSend(value);
    }
  }

  /**触发该消息 */
  protected async doEmit(event: Event<T>) {
    this.#ensureOpen();
    const consumers = this.consumers;
    for (const consumer of consumers) {
      // 如果消费者没启动，或者没有开始读，或者是写通道还是关闭的
      if (!consumer.started || consumer.startingBuffers?.has(event) == true || consumer.input.isClosedForSend) {
        continue;
      }
      await event.emitBy(consumer);
      if (event.stoped) {
        break;
      }
    }
  }
  //#region Consumer
  /**消费者 */
  static #Consumer = class Consumer<T> {
    constructor(
      readonly name: string,
      readonly input: Channel<Event<T>> = new Channel(),
      private producer: Producer<T>
    ) {
      producer.consumers.add(this);
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
      const starting = this.producer.buffers;
      this.startingBuffers = starting;
      for (const event of starting) {
        event.emitBy(this);
      }
      this.startingBuffers = null;
    }
    #collectors = new Set<$FlowCollector<T>>();
    // 收集并触发所有的事件
    #startCollect = $once(() => {
      const job = (async () => {
        for await (const event of this.input) {
          for (const collector of this.#collectors) {
            await collector(event);
          }
        }
      })();
      this.#start();
      return job;
    });
    /**收集事件 */
    collect(collector: (event: Event<T>) => void) {
      this.#collectors.add(collector);
      // 事件在收集了再调用开始
      this.#startCollect();
      return () => this.#collectors.delete(collector);
    }

    mapNotNull<R>(transform: (value: T) => R | undefined) {
      const signal = new Signal<$Callback<[R]>>();
      this.collect((event) => {
        const result = transform(event.data);
        if (result !== undefined) {
          signal.emit(result);
        }
      });

      return signal.listen;
    }

    #destroySignal = createSignal();
    onDestroy = this.#destroySignal.listen;
    cancel = $once(() => {
      this.producer.consumers.delete(this);
      this.#destroySignal.emitAndClear();
    });
  };
  // 创建一个消费者
  consumer(name: string) {
    this.#ensureOpen();
    const consumer = new Producer.#Consumer(name, undefined, this);
    return consumer;
  }
  //#endregion

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
  private closeWrite() {
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
    cause && this.console.debug("producer-close", cause);
  }

  #closeSignal = createSignal<() => unknown>(false);
  onClosed = this.#closeSignal.listen;
  //#endregion
}
