/// <reference lib="dom"/>
export const setupDB = async (sessionId: string) => {
  document.currentScript?.parentElement?.removeChild(document.currentScript);
  const KEY = "--plaoc-session-id--";
  if (localStorage.getItem(KEY) !== sessionId) {
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