export class Compareable<T> {
  constructor(readonly value: T, readonly getScore: (val: T) => Record<string, number>) {}
  private _score?: Record<string, number>;
  get score() {
    return (this._score ??= this.getScore(this.value));
  }
  /**
   * 等价于 `(this as a) - b`
   * @param b
   * @returns
   */
  compare(b: Compareable<T>) {
    const aScore = this.score;
    const bScore = b.score;
    for (const key in aScore) {
      const aValue = aScore[key];
      const bValue = bScore[key];
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
