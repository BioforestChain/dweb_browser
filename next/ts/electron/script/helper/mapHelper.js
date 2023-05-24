"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.mapHelper = void 0;
exports.mapHelper = new (class {
    getOrPut(map, key, putter) {
        if (map.has(key)) {
            return map.get(key);
        }
        const put = putter(key);
        map.set(key, put);
        return put;
    }
})();
