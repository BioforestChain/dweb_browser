export const $once = <T extends (...args: any) => unknown>(fn: T) => {
  let first = true;
  let resolved: any;
  let rejected: any;
  let success = false;
  return Object.defineProperties(
    function (this: any, ...args: unknown[]) {
      if (first) {
        first = false;
        try {
          resolved = fn.apply(this, args);
          success = true;
        } catch (err) {
          rejected = err;
        }
      }
      if (success) {
        return resolved;
      }
      throw rejected;
    } as unknown as T,
    {
      hasRun: {
        get() {
          return !first;
        },
      },
      result: {
        get() {
          if (success) {
            return resolved as ReturnType<T>;
          } else {
            throw rejected;
          }
        },
      },
      origin: {
        configurable: true,
        writable: false,
        value: fn,
      },
      reset: {
        configurable: true,
        writable: true,
        value: () => {
          first = true;
          resolved = undefined;
          rejected = undefined;
          success = false;
        },
      },
    }
  ) as T & {
    readonly fn:T
    readonly hasRun: boolean;
    readonly result: ReturnType<T>;
    reset(): void;
  };
};

// export const once = ((target, ctx) => {
//   let first = true;
//   let cache: any;
//   console.log(target, ctx);辅助函数
//   return target;
// }) as ClassGetterDecorator;
export const once = () => {
  return (target: any, propertyKey: string, desp: PropertyDescriptor): any => {
    const source_fun = desp.get;
    if (source_fun === undefined) {
      throw new Error(`${target}.${propertyKey} should has getter`);
    }
    desp.get = function () {
      const result = source_fun.call(this);
      if (desp.set) {
        desp.get = () => result;
      } else {
        delete desp.set;
        delete desp.get;
        desp.value = result;
        desp.writable = false;
      }
      Object.defineProperty(this, propertyKey, desp);
      return result;
    };
    return desp;
  };
};
// return (target: object, context: ClassGetterDecoratorContext): any => {
//   const name = String(context.name);
//   const source_fun = context.access.get;
//   if (source_fun === undefined) {
//     throw new Error(`${name} should has getter`);
//   }
//   context.access.get = function (...args: any) {
//     const result = source_fun.call(this, args);
//     if (context.access.get) {
//       context.access.get = () => result;
//     } else {
//       // delete context.access.get;
//       context.access.get = () => result;
//     }
//     Object.defineProperty(this, name, {
//       // get: context.access.get(target),
//       set: undefined,
//       enumerable: true,
//       configurable: true,
//     });
//     return result;
//   };
//   return context;
// };
