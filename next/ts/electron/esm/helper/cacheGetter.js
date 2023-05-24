export const cacheGetter = () => {
    return (target, prop, desp) => {
        const source_fun = desp.get;
        if (source_fun === undefined) {
            throw new Error(`${target}.${prop} should has getter`);
        }
        desp.get = function () {
            const result = source_fun.call(this);
            if (desp.set) {
                desp.get = () => result;
            }
            else {
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
