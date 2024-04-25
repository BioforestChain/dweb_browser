// vite.config.js
import { resolve } from 'path'
import { defineConfig } from 'vite'

export default defineConfig({
  build: {
    lib: {
      // Could also be a dictionary or array of multiple entry points
      entry: resolve(__dirname, 'src/index.ts'),
      name: '@dweb-browser/core',
      formats: ['es'], // 打包模式，默认是es和umd都打
    },
    rollupOptions: {
      input: {
        main: 'src/index.ts',
        process: "worker-process/worker/index.ts",
        helper: 'helper/index.ts'
      },
      // 确保外部化处理那些你不想打包进库的依赖
      // external: ['vue'],
      output: {
        // Provide global variables to use in the UMD build
        // for externalized deps
        globals: {
          // vue: 'Vue',
        },
      },
    },
    outDir: "build", // 打包后存放的目录文件
  },
})