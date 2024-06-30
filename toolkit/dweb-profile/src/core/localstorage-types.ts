export type Row = { key: string; value: string };

export type BackupV1 = {
  version: number;
  items: Array<Row>;
};
