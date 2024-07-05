/// <reference lib="dom"/>

/**
 * @param sessionId 缓存id 如果本地存储不同则清除数据
 * @param patch 布丁程序，为1的时候已经实现了数据管理，卸载掉时候让用户选择是否清除数据
 * @param brands 是否开启预览版
 * @returns
 */

export const setupDB = async (sessionId: string, patch = 0, brands = 0) => {
  document.currentScript?.parentElement?.removeChild(document.currentScript);
  const KEY = "--plaoc-session-id--";
  console.log("setupDB=>", sessionId, patch, brands);
  // 当开启预览版或者当布丁程序开启的时候  用户将自己管理自己的数据
  if (brands !== 0 || patch !== 0) {
    return;
  }
  console.log("是否清空数据：", localStorage.getItem(KEY) !== sessionId);

  if (localStorage.getItem(KEY) !== sessionId) {
    console.log("clearSession=>", sessionId);
    localStorage.clear();
    localStorage.setItem(KEY, sessionId);
    sessionStorage.clear();
    const tasks = [];
    const t1 = indexedDB.databases().then((dbs) => {
      for (const db of dbs) {
        if (db.name) {
          indexedDB.deleteDatabase(db.name);
        }
      }
    });
    tasks.push(t1.catch(console.error));
    if (typeof cookieStore === "object") {
      const t2 = cookieStore.getAll().then((cookies) => {
        for (const c of cookies) {
          cookieStore.delete(c.name);
        }
      });
      tasks.push(t2.catch(console.error));
    }
    await Promise.all(tasks);
    (location as Location).replace(location.href);
  }
};
