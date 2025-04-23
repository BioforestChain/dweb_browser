import { defineConfig } from 'vite';
import { svelte } from '@sveltejs/vite-plugin-svelte';
import { toolkitResolverPlugin } from '../scripts/vite-npm-resolver-plugin.mts';
import path from 'node:path';

export default defineConfig({
  plugins: [toolkitResolverPlugin(), svelte()],
  build: {
    target: 'esnext',
    rollupOptions: {
      input: {
        backup: path.join(__dirname, '/backup.html'),
        recovery: path.join(__dirname, '/recovery.html'),
      },
      output: {
        assetFileNames: `assets/F[name]-[hash].[ext]`,
        chunkFileNames: `F[name]-[hash].js`,
      },
    },
    // lib: {
    //   entry: {
    //     indexeddb: './src/core/indexeddb.ts',
    //     localstorage: './src/core/localstorage.ts'
    //   },
    //   // 多入口不支持 iife
    //   formats: ['es' as const]
    // },
    minify: false,
    emptyOutDir: true,
  },
});
