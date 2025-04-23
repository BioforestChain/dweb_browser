import { defineConfig } from 'vitest/config';
import { svelte } from '@sveltejs/vite-plugin-svelte';
import { toolkitResolverPlugin } from '../scripts/vite-npm-resolver-plugin.mts';

export default defineConfig({
  plugins: [toolkitResolverPlugin(), svelte()],
  test: {
    include: ['src/**/*.{test,spec}.{js,ts}'],
  },
});
