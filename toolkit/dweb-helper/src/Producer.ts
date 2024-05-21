import { Mutex } from "./Mutex.ts";
import { PromiseOut } from "./PromiseOut.ts";
import { Signal, createSignal, type $Callback } from "./createSignal.ts";
import { CUSTOM_INSPECT, customInspect, logger } from "./logger.ts";

type Event<T> = ReturnType<Producer<T>["event"]>;
type Consumer<T> = ReturnType<Producer<T>["consumer"]>;
type $FlowCollector<T> = (event: Event<T>) => void;
//#region Producer
/**生产者 */
export class Producer<T> {
  readonly console = logger(this);
  constructor(
    readonly name: string,
    options: { bufferLimit?: number; bufferOverflowBehavior?: "warn" | "throw" | "slient" } = {}
  ) {
    this.bufferLimit = options.bufferLimit ?? 10;
    this.bufferOverflowBehavior = options.bufferOverflowBehavior || "warn";
  }
  readonly bufferLimit;
  readonly bufferOverflowBehavior;
  toString() {
    return `Producer<${this.name}>`;
  }
  [CUSTOM_INSPECT]() {
    return this.toString();
  }
  //#region Event
  static #Event = class Event<T> {
    constructor(readonly data: T, readonly producer: Producer<T>) {}
    toString() {
      return `Event<${customInspect(this.data)}>`;
    }

    [CUSTOM_INSPECT]() {
      return this.toString();
    }
    #job = new PromiseOut<void>();
    get job() {
      return this.#job.promise;
    }

    /**标记事件是否被消耗 */
    #consumeTimes = 0;
    get consumed() {
      return this.#consumeTimes > 0;
    }

    /**
     * 事件消耗器，调用后事件被标记成已消耗，这意味着：
     * 1. 现有的消费者还是能看到它
     * 2. 其它后来的消费者不会再看到它
     */
    consume(): T {
      this.#consumeTimes++;
      return this.data;
    }

    /**
     * 事件停止传播，调用后事件不会被其他消费者触发
     */
    #stoped = false;
    get stoped() {
      return this.#stoped;
    }
    /**
     * 停止传播
     *
     * 事件消费，并停止向其它消费器继续传播
     */
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
      if (result !== null && result !== undefined) {
        this.consume();
        return result;
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
      return this.#emitLock.withLock(async () => {
        // 事件超时告警
        const timeoutId = setTimeout(() => {
          console.warn(`emitBy TIMEOUT!!`, `consumer=`, consumer, `data=`, this.data);
        }, 1000);
        const beforeConsumeTimes = this.#consumeTimes;
        // 触发单个消费者中的单个消息
        try {
          await consumer.input.emit(this);
        } catch (e) {
          console.error(`emitBy ERROR!!`, `consumer=`, consumer, `data=`, this.data, e);
        }
        clearTimeout(timeoutId);

        if (this.consumed) {
          this.complete();
          if (this.#consumeTimes != beforeConsumeTimes) {
            this.producer.console.verbose(
              "emitBy",
              `event=`,
              this,
              `consumed[${beforeConsumeTimes}>>${this.#consumeTimes}] by consumer=`,
              consumer
            );
          }
        }
      });
    }
  };

  private event(data: T) {
    /**生产者构造的事件*/
    return new Producer.#Event<T>(data, this);
  }
  //#endregion
  // 生产者中全部的消费者
  protected consumers = new Set<Consumer<T>>();
  // 事件缓存
  protected buffers = new Set<Event<T>>();
  /** 保证发送 */
  send(value: T) {
    this.#ensureOpen();
    this.#doSend(value);
  }

  #doSend(value: T) {
    const event = this.event(value);
    this.buffers.add(event);
    if (this.buffers.size > this.bufferLimit) {
      if (this.bufferOverflowBehavior === "warn") {
        this.console.warn(`${this} buffers overflow maybe leak: ${this.buffers.size}`);
      } else if (this.bufferOverflowBehavior === "throw") {
        throw new Error(`${this} buffers overflow: ${this.buffers.size}/${this.bufferLimit}`);
      }
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
      return this.sendBeacon(value);
    } else {
      return this.#doSend(value);
    }
  }

  /**触发该消息 */
  protected async doEmit(event: Event<T>) {
    // 触发所有消费者的消息
    for (const consumer of this.consumers) {
      // 如果消费者没启动，或者没有开始读
      if (!consumer.started || consumer.startingBuffers?.has(event) == true) {
        continue;
      }
      await event.emitBy(consumer);
      if (event.stoped) {
        break;
      }
    }
  }

  /**
   * 创建一个消费者
   */
  consumer(name: string) {
    this.#ensureOpen();
    const consumer = new Producer.#Consumer(name, this);
    return consumer;
  }
  static EasyFlow = class EasyFlow<T> {
    #cb?: (event: Event<T>) => unknown;
    close() {
      this.#cb = undefined;
    }
    /**收集消息 */
    collect(cb: (event: Event<T>) => unknown) {
      this.#cb = cb;
    }
    async emit(event: Event<T>) {
      await this.#cb?.(event);
    }
  };
  //#region Consumer
  /**消费者 */
  static #Consumer = class Consumer<T> {
    constructor(readonly name: string, readonly producer: Producer<T>) {}

    toString() {
      return `Consumer<[${this.producer.name}]${this.name}>`;
    }

    [CUSTOM_INSPECT]() {
      return this.toString();
    }
    /**输入 */
    readonly input = new Producer.EasyFlow<T>();

    /**标记是否开始消费 */
    #started = false;
    get started() {
      return this.#started;
    }

    // 消费过的事件
    startingBuffers: Set<Event<T>> | null = null;
    #errorCatcher = new PromiseOut<string | undefined>();

    /**开始触发之前的 */
    #start() {
      // 把自己添加进入消费者队列
      this.producer.consumers.add(this);
      this.#started = true;
      this.startingBuffers = new Set(this.producer.buffers);
      for (const event of this.startingBuffers) {
        event.emitBy(this);
      }
      this.startingBuffers.clear();
      this.startingBuffers = null;
    }

    /**收集事件 */
    collect(collector: (event: Event<T>) => void) {
      this.input.collect(collector);
      // 事件在收集了再调用开始
      return this.#start();
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
    close(cause?: string) {
      this.input.close();
      this.producer.consumers.delete(this);
      this.#errorCatcher.resolve(cause);
    }
  };
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

  /**全部关闭 */
  // deno-lint-ignore require-await
  async close(cause?: string) {
    if (this.#isClosedForSend) {
      return;
    }
    this.#isClosedForSend = true;
    const bufferEvents = this.buffers;
    for (const event of bufferEvents) {
      if (!event.consumed) {
        event.consume();
        event.complete();
        console.debug("closeWrite", `event=${event} consumed by close`);
      }
    }

    // 关闭消费者channel，表示彻底无法再发数据
    for (const consumer of this.consumers) {
      consumer.close(cause);
    }
    this.consumers.clear();
    this.buffers.clear();
    cause && this.console.log("producer-close", cause);
  }

  #closeSignal = createSignal<() => unknown>(false);
  onClosed = this.#closeSignal.listen;
  //#endregion
}
