<script lang="ts">
  import { BACKUP_FILE_EXT, decodeFromBlob } from './core/download';
  import { importIndexdb } from './core/indexeddb';

  let files: FileList | undefined;
  // logic goes here
  const doRecovery = async () => {
    const file = files![0];
    const result = await decodeFromBlob(file);
    console.log('解码完成', result);
    await importIndexdb(result);
    console.log('恢复完成');
  };
</script>

<h1>Recovery ~~</h1>
<input
  type="file"
  bind:files
  title="请选择备份 .dweb-profile.cbor.7z 文件"
  accept={BACKUP_FILE_EXT}
/>
<button disabled={(files?.length ?? 0) == 0} on:click={doRecovery}>开始恢复备份</button>

<style>
  /* styles go here */
</style>
