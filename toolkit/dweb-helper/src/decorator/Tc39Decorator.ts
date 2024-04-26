export type ClassGetterDecorator = <T, R>(
  origFn: (this: T) => R,
  ctx: ClassGetterDecoratorContext<T, R>
) => undefined | ((this: T) => R);
export type ClassSetterDecorator = (
  value: Function,
  context: {
    kind: "setter";
    name: string | symbol;
    access: { set(value: unknown): void };
    static: boolean;
    private: boolean;
    addInitializer(initializer: () => void): void;
  }
) => Function | void;