export const bindThis = (
  target: object,
  prop: string,
  desp: PropertyDescriptor
) => {
  const source_fun = desp.value;
  if (typeof source_fun !== "function") {
    throw new Error(`${target}.${prop} should be function`);
  }
  desp.get = function () {
    const result = source_fun.bind(this);
    delete desp.get;
    desp.value = result;
    desp.writable = true;
    Object.defineProperty(this, prop, desp);
    return result;
  };
  delete desp.value;
  delete desp.writable;
  return desp;
};
