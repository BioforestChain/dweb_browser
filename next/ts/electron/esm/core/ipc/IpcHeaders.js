export class IpcHeaders extends Headers {
    init(key, value) {
        if (this.has(key)) {
            return;
        }
        this.set(key, value);
    }
    toJSON() {
        const record = {};
        this.forEach((value, key) => {
            // 单词首字母大写
            record[key.replace(/\w+/g, (w) => w[0].toUpperCase() + w.slice(1))] =
                value;
        });
        return record;
    }
}
