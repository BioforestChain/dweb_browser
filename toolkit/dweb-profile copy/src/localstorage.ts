import * as $LocalStorageBackup from "./localstorage-types.ts";

/**
 * 导出完整的 localStorage 数据库
 */
export const exportLocalStorage = async () => {
  const export_backup: $LocalStorageBackup.Backup = { items: [] };
  for (let i = 0; i < localStorage.length; i++) {
    const key = localStorage.key(i)!;
    const value = localStorage.getItem(key)!;
    export_backup.items.push({ key, value });
  }
  return export_backup;
};

/**
 * 覆盖性导入 localStorage 数据库
 */
export const importLocalStorage = async (import_backup: $LocalStorageBackup.Backup) => {
  localStorage.clear();
  for (const item of import_backup.items) {
    localStorage.setItem(item.key, item.value);
  }
};
