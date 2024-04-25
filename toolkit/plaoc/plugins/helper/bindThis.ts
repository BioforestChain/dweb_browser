/**
 * 
 * @param target 对应类的prototype
 * @param prop 方法名
 * @param desp 属性描述
 * @returns 
 */
export const bindThis = (target: object, prop: string, desp: PropertyDescriptor) => {
  const source_fun = desp.value;
  if (typeof source_fun !== "function") {
    throw new Error(`${target}.${prop} should be function`);
  }
  desp.get = function () {
    const result = source_fun.bind(this);
    delete desp.get;
    // 当前属性值 ，对于函数表示他本身
    desp.value = result;
    // 当前属性值
    desp.writable = true;
    Object.defineProperty(this, prop, desp);
    return result;
  };
  delete desp.value;
  delete desp.writable;
  return desp;
};
