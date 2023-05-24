export const mapHelper = new (class {
    getOrPut(map, key, putter) {
        if (map.has(key)) {
            return map.get(key);
        }
        const put = putter(key);
        map.set(key, put);
        return put;
    }
})();
