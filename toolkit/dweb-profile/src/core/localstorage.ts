import * as $LocalStorageBackup from './localstorage-types.ts';

export const exportLocalStorageV1 = async () => {
  const export_backup: $LocalStorageBackup.BackupV1 = { version: 1, items: [] };
  for (let i = 0; i < localStorage.length; i++) {
    const key = localStorage.key(i)!;
    const value = localStorage.getItem(key)!;
    export_backup.items.push({ key, value });
  }
  return export_backup;
};

/**
 * 导出完整的 localStorage 数据库
 */
export const exportLocalStorage = exportLocalStorageV1;

export const importLocalStorageV1 = async (import_backup: $LocalStorageBackup.BackupV1) => {
  localStorage.clear();
  for (const item of import_backup.items) {
    localStorage.setItem(item.key, item.value);
  }
};
/**
 * 覆盖性导入 localStorage 数据库
 */
export const importLocalStorage = (import_backup: $LocalStorageBackup.BackupV1) => {
  if (import_backup.version === 1) {
    importLocalStorageV1(import_backup);
  }
  throw new Error(`unknown backup file version: ${import_backup.version}`);
};
