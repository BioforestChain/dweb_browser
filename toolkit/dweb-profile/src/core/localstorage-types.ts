export type Row = { key: string; value: string };

export type BackupV1 = {
  dbtype: 'localstorage';
  version: 1;
  items: Array<Row>;
};
