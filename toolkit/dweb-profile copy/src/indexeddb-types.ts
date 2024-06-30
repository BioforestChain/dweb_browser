export type StoreRow = { key: IDBValidKey; primaryKey: IDBValidKey; value: unknown };
export type StoreIndex = {
  name: string;
  keyPath: string[];
  multiEntry: boolean;
  unique: boolean;
};
export type Store = {
  name: string;
  autoIncrement: boolean;
  keyPath: string[];
  indexs: Array<StoreIndex>;
  cols: Array<StoreRow>;
};
export type Database = {
  name: string;
  version: number;
  stores: Array<Store>;
};

export type Backup = {
  dbs: Array<Database>;
};
