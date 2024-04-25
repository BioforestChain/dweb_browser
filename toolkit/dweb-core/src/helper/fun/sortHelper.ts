export class Compareable<T> {
  constructor(readonly value: T, readonly getScore: (val: T) => Record<string, number>) {}
  private _score?: Record<string, number>;
  get score() {
    return (this._score ??= this.getScore(this.value));
  }
  private get _scope_keys() {
    return Object.keys(this.score);
  }
  /**
   * 等价于 `(this as a) - b`
   * @param b
   * @returns
   */
  compare(b: Compareable<T>) {
    const aScore = this.score;
    const bScore = b.score;
    for (const key of this._scope_keys) {
      const aValue = aScore[key] || 0;
      const bValue = bScore[key] || 0;
      if (aValue !== bValue) {
        return aValue - bValue;
      }
    }
    return 0;
  }
}

export const enumToCompareable = <T>(enumValue: T, enumList: readonly T[]) => {
  return enumList.indexOf(enumValue);
};
