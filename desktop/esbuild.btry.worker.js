// 用来bundle statusbar.worker 文件
const esbuild = require('esbuild')
const htmlModulesPlugin = require('esbuild-plugin-html-modules')
const options = {
    bundle: true,
    format: 'esm',
    entryPoints: ['./src/user/btry/btry.worker.mts'],
    outfile: './bundle/btry.worker.js',
    loader:{
      ".html": "text"
    }

    // plugins: [
    //   htmlModulesPlugin()
    // ]
  }

async function main(){
    const ctx = await esbuild.context(options);
    await ctx.watch();
}

main()
// esbuild.build()
// .catch(() => process.exit(1))