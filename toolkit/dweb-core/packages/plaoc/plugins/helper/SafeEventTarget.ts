export class SafeEventTarget<T extends Record<string, Event>> extends EventTarget {
  override addEventListener<K extends keyof T>(
    type: K,
    callback: $SafeEventListenerOrEventListenerObject<T[K]> | null,
    options?: boolean | AddEventListenerOptions | undefined
  ): void;
  override addEventListener<K extends keyof T>(
    type: K,
    callback: $SafeEventListenerOrEventListenerObject<T[K]> | null,
    options?: boolean | AddEventListenerOptions | undefined
  ): void;
  override addEventListener<K extends keyof T>(
    type: K,
    listener: $SafeEventListenerOrEventListenerObject<T[K]> | null,
    options?: boolean | AddEventListenerOptions | undefined
  ): void;
  override addEventListener(type: any, listener: any, options?: any): void {
    super.addEventListener(type, listener, options);
  }
  override removeEventListener<K extends keyof T>(
    type: K,
    callback: $SafeEventListenerOrEventListenerObject<T[K]> | null,
    options?: boolean | EventListenerOptions | undefined
  ): void;
  override removeEventListener<K extends keyof T>(
    type: K,
    callback: $SafeEventListenerOrEventListenerObject<T[K]> | null,
    options?: boolean | EventListenerOptions | undefined
  ): void;
  override removeEventListener<K extends keyof T>(
    type: K,
    callback: $SafeEventListenerOrEventListenerObject<T[K]> | null,
    options?: boolean | EventListenerOptions | undefined
  ): void;
  override removeEventListener(type: any, callback: any, options?: any): void {
    super.removeEventListener(type, callback, options);
  }
  override dispatchEvent(event: T[keyof T]): boolean {
    const hanlder = Reflect.get(this, "on" + event.type);
    if (typeof hanlder === "function") {
      try {
        hanlder.call(this, event);
      } catch {}
    }
    return super.dispatchEvent(event);
  }
}

export interface $SafeEventListener<E extends Event> {
  (evt: E): void;
}
export interface $SafeEventListenerObject<E extends Event> {
  handleEvent(object: E): void;
}
export type $SafeEventListenerOrEventListenerObject<E extends Event> =
  | $SafeEventListener<E>
  | $SafeEventListenerObject<E>;

export class SafeMessageEvent<T extends string, D> extends MessageEvent<D> {
  constructor(type: T, eventInitDict: MessageEventInit<D>) {
    super(type, eventInitDict);
  }
}

export class SafeEvent<T extends string> extends Event {
  constructor(type: T, eventInitDict?: EventInit) {
    super(type, eventInitDict);
  }
}

export class SafeStateEvent<T extends string, S> extends Event {
  readonly state: S;
  constructor(type: T, eventInitDict: EventInit & { state: S }) {
    super(type, eventInitDict);
    this.state = eventInitDict.state;
  }
}
