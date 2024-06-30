import * as idb from 'idb';
import * as $IndexeddbBackup from './indexeddb-types.ts';

export const getIndexeddbInfo = async () => {
  const databases = await indexedDB.databases();
  return { databases };
};

export const exportIndexeddbV1 = async () => {
  const export_backup: $IndexeddbBackup.BackupV1 = { version: 1, dbs: [] };
  for (const db_info of await indexedDB.databases()) {
    const db = await idb.openDB(db_info.name!);
    const read = await db.transaction(db.objectStoreNames, 'readonly');

    const export_db: $IndexeddbBackup.Database = {
      name: db.name,
      version: db.version,
      stores: []
    };
    export_backup.dbs.push(export_db);

    for (const storeName of read.objectStoreNames) {
      const storeObject = read.objectStore(storeName);
      const export_store: $IndexeddbBackup.Store = {
        name: storeObject.name,
        autoIncrement: storeObject.autoIncrement,
        keyPath: Array.isArray(storeObject.keyPath) ? storeObject.keyPath : [storeObject.keyPath],
        indexs: [],
        cols: []
      };
      export_db.stores.push(export_store);

      /// 导出索引信息
      for (const indexName of storeObject.indexNames) {
        const index = await storeObject.index(indexName);
        export_store.indexs.push({
          name: index.name,
          keyPath: Array.isArray(index.keyPath) ? index.keyPath : [index.keyPath],
          unique: index.unique,
          multiEntry: index.multiEntry
        });
      }

      /// 导出数据
      let cursor = await storeObject.openCursor();
      while (cursor !== null) {
        export_store.cols.push({
          key: cursor.key,
          primaryKey: cursor.primaryKey,
          value: cursor.value
        });
        cursor = await cursor.continue();
      }
    }

    db.close();
  }
  return export_backup;
};

/**
 * 清理原有的 indexeddb 数据库
 */
export const clearIndexeddb = async () => {
  for (const db_info of await indexedDB.databases()) {
    await idb.deleteDB(db_info.name!);
  }
};

/**
 * 导出完整的 indexeddb 数据库
 */
export const exportIndexeddb = exportIndexeddbV1;

export const importIndexdbV1 = async (import_backup: $IndexeddbBackup.BackupV1) => {
  await clearIndexeddb();

  for (const import_db of import_backup.dbs) {
    /// 建表
    const db = await idb.openDB(import_db.name, import_db.version, {
      upgrade: (db, _oldVersion, _newVersion, _transaction, _event) => {
        for (const import_store of import_db.stores) {
          const store = db.createObjectStore(import_store.name, {
            autoIncrement: import_store.autoIncrement,
            keyPath: import_store.keyPath
          });
          for (const import_index of import_store.indexs) {
            store.createIndex(import_index.name, import_index.keyPath, {
              multiEntry: import_index.multiEntry,
              unique: import_index.unique
            });
          }
        }
      }
    });

    /// 导入数据
    const write = await db.transaction(db.objectStoreNames, 'readwrite');
    for (const import_store of import_db.stores) {
      const storeObject = write.objectStore(import_store.name);
      for (const import_row of import_store.cols) {
        void storeObject.add(import_row.value, import_row.primaryKey);
      }
    }
    write.commit();
    db.close();
  }
};
/**
 * 覆盖性导入 indexeddb 数据库
 */
export const importIndexdb = (import_backup: $IndexeddbBackup.BackupV1) => {
  if (import_backup.version === 1) {
    return importIndexdbV1(import_backup);
  }
  throw new Error(`unknown backup file version: ${import_backup.version}`);
};
