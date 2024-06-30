import { defineConfig } from 'vite';
import { svelte } from '@sveltejs/vite-plugin-svelte';

export default defineConfig({
  plugins: [svelte()],
  build: {
    target: 'esnext',
    lib: {
      entry: {
        indexeddb: './src/indexeddb.ts',
        localstorage: './src/localstorage.ts'
      },
      // 多入口不支持 iife
      formats: ['es' as const]
    },
    minify: false,
    emptyOutDir: true
  }
});
