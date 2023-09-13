interface Row<T> {
  [key: string]: T;
}

export class MemoryTable<T> {
  private rows: Row<T>[] = [];

  set(criteria: string, value: T) {
    this.rows.push({ [criteria]: value });
  }


  delete(criteria: string) {
    for (const row of this.rows) {
      for (const key of Object.keys(row)) {
        if (key === criteria) {
          return delete row[key];
        }
      }
    }
    false;
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
