/// <reference lib="dom"/>

export const setupDB = async (sessionId: string) => {
  document.currentScript?.parentElement?.removeChild(document.currentScript);
  const KEY = "--plaoc-session-id--";
  // deno-lint-ignore no-explicit-any
  const APP_VERSION = (globalThis as any)?.APP_VERSION;

  if (APP_VERSION !== undefined && localStorage.getItem(KEY) == undefined) {
    console.log("触发清空", APP_VERSION, sessionId);
    localStorage.setItem(KEY, APP_VERSION);
    localStorage.clear();
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
