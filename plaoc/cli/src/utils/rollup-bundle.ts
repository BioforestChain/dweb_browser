import { nodeResolve, path, rollup } from "../../deps.ts";

/**
 * 编译后端项目到单文件，将依赖打包进源码，避免依赖node_modules
 * @returns 返回bfsaEntry
 */
export async function build(options: {
  entryFile: string;
  dest: string;
  bfsAppId: string;
}) {
  const { entryFile, dest, bfsAppId } = options;

  const bundle = await rollup({
    input: entryFile,
    plugins: [nodeResolve()],
  });

  const outputFile = path.join(dest, `./backend-${bfsAppId}/index.js`);

  await bundle.write({
    file: outputFile,
  });

  return `sys/backend-${bfsAppId}/index.js`;
}
