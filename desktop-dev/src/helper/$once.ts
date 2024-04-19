export const $once = <T extends Function>(fn: T) => {
  let first = true;
  let resolved: any;
  let rejected: any;
  let success = false;
  return function (this: any, ...args: unknown[]) {
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
  } as unknown as T;
};

// export const once = ((target, ctx) => {
//   let first = true;
//   let cache: any;
//   console.log(target, ctx);
//   return target;
// }) as ClassGetterDecorator;
export const once = () => {
  return (target: object, prop: string, desp: PropertyDescriptor) => {
    const source_fun = desp.get;
    if (source_fun === undefined) {
      throw new Error(`${target}.${prop} should has getter`);
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
      Object.defineProperty(this, prop, desp);
      return result;
    };
    return desp;
  };
};
