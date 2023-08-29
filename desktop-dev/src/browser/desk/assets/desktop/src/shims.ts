if (typeof Array.prototype.at !== "function") {
  Array.prototype.at = function at(index: number) {
    // 索引正常化
    if (index < 0) {
      index += this.length;
    }
    // 边界检查
    if (index >= 0 && index < this.length) {
      return this[index];
    }
    return undefined;
  };
}
