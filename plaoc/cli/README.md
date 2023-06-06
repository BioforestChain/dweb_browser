### cli

1. `deno install -A https://deno.land/x/plaoc/cli/plaoc.ts`

1. `plaoc bundle ./dir`
   会打包成以下的文件夹结构，并输出压缩文件 `.zip` 和一个 `plaoc.metadata.json`

1. `plaoc preview http://localhost:1231` 或者 `plaoc preview ./dir`
   > 会将该 url 放在一个 iframe 中被预览
   > 该命令会输出一行命令：
   ```bash
   dweb-browser-dev install --url http://172.30.90.240:8080/usr/metadata.json
   ```
