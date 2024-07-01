<script lang="ts">
  // logic goes here
  import { exportIndexeddb, importIndexdb, clearIndexeddb } from './core/indexeddb.ts';
  import { BACKUP_FILE_EXT, download, encodeToBlob, getDateTimeString } from './core/download.ts';
  const doBackup = async () => {
    console.log('开始备份');
    const export_backup = await exportIndexeddb();
    console.log('备份完成', export_backup);
    const blob = await encodeToBlob(export_backup);
    console.log('编码完成', blob);
    download(URL.createObjectURL(blob), getDateTimeString() + BACKUP_FILE_EXT);
  };
  import * as idb from 'idb';
  const keyList: IDBValidKey[] = [
    1111,
    -0,
    // 100n,
    Infinity,
    -Infinity,
    // NaN,
    new Date(10000),
    'strin中文😀',
    new Uint16Array(17),
    new Uint8Array(27),
    new Uint8ClampedArray(37),
    new Int8Array(47),
    new Int16Array(57),
    new Int32Array(67),
    new Float32Array(77),
    new Float64Array(87),
    new BigInt64Array(97),
    new BigUint64Array(107),
    new ArrayBuffer(117),
  ];
  for (const item of keyList.slice()) {
    keyList.push([item, item]);
  }
  const valueList: any[] = [
    new Blob(['aaaaaaaaaa']),
    new Uint16Array(100),
    new Uint8Array(100),
    new Uint8ClampedArray(100),
    new Int8Array(100),
    new Int16Array(100),
    new Int32Array(100),
    new Float32Array(100),
    new Float64Array(100),
    new BigInt64Array(100),
    new BigUint64Array(100),
    new ArrayBuffer(100),
    'strin中文😀',
    new String('xxx'),
    1111,
    -0,
    new Number(1111),
    new Number(-0),
    100n,
    Infinity,
    -Infinity,
    // TODO wait for bug fixed
    // NaN,
    new Date(10000),
    /./,
    false,
    true,
    new Boolean(false),
    undefined,
  ];
  for (const item of valueList.slice()) {
    valueList.push([item, item]);
  }
  keyList.push(
    ...Array.from(
      { length: valueList.length - keyList.length },
      (_, i) => keyList.length + i + Math.random(),
    ),
  );

  console.log(keyList);
  const doGenerate = async () => {
    const db = await idb.openDB(crypto.randomUUID(), 1, {
      upgrade: (db) => {
        db.createObjectStore('qaq', { autoIncrement: true });
      },
    });
    const write = db.transaction('qaq', 'readwrite');
    for (const [index, item] of valueList.entries()) {
      const key = keyList[index];
      //   if (typeof key === 'number' && key.toString().startsWith('0.')) {
      //     continue;
      //   }
      console.log('add', item, key);
      await write.store.add(item, key);
    }
    db.close();
  };
</script>

<h1>Backup ~~</h1>

<button on:click={clearIndexeddb}>清理数据</button>
<button on:click={doGenerate}>生成数据</button>
<button on:click={doBackup}>开始备份</button>

<style>
  /* styles go here */
</style>
