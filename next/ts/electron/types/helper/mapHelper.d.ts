export declare const mapHelper: {
    getOrPut<K extends object, V>(map: WeakMap<K, V>, key: K, putter: (key: K) => V): V;
    getOrPut<K_1, V_1>(map: Map<K_1, V_1>, key: K_1, putter: (key: K_1) => V_1): V_1;
};
