interface Row<V> {
  [key: string]: V;
}

export class MemoryTable<T> {
  private rows: Row<T>[] = [];

  set(criteria: Row<T>) {
    this.rows.push(criteria);
  }

  delete(criteria: string) {
    for (let i = 0; i < this.rows.length; i++) {
      const row = this.rows[i];
      for (const key in row) {
        if (key === criteria) {
          return this.rows.splice(i, 1);
        }
      }
    }
  }

  get(criteria: string) {
    for (const row of this.rows) {
      for (const key of Object.keys(row)) {
        if (key === criteria) {
          return row[key];
        }
      }
    }
  }

  has(criteria: string) {
    return this.get(criteria) ? true : false;
  }
}
