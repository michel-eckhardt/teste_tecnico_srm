import { fileURLToPath, URL } from 'node:url';

import react from '@vitejs/plugin-react';
import { loadEnv, type ProxyOptions } from 'vite';
import { defineConfig } from 'vitest/config';

export default defineConfig(({ mode }) => {
  // The SPA only ever calls same-origin `/api/...`; in development the Vite server proxies it to the
  // backend (in production, nginx does the same), so there is no CORS or backend URL in the bundle.
  const env = loadEnv(mode, process.cwd(), '');
  const proxy: Record<string, ProxyOptions> = {
    '/api': { target: env.API_PROXY_TARGET ?? 'http://localhost:8080', changeOrigin: true },
  };

  return {
    plugins: [react()],
    resolve: {
      alias: { '@': fileURLToPath(new URL('./src', import.meta.url)) },
    },
    server: { port: 5173, strictPort: true, proxy },
    preview: { port: 4173, strictPort: true, proxy },
    build: {
      rolldownOptions: {
        output: {
          // Libraries change far less often than the app: separate chunks keep them cached
          // across deployments (file names are content-hashed).
          codeSplitting: {
            groups: [
              {
                name: 'react',
                test: /node_modules[\\/](react|react-dom|react-router|scheduler)[\\/]/,
                priority: 3,
              },
              {
                name: 'mantine',
                test: /node_modules[\\/](@mantine|@floating-ui)[\\/]/,
                priority: 2,
              },
              { name: 'vendor', test: /node_modules[\\/]/, priority: 1 },
            ],
          },
        },
      },
    },
    test: {
      environment: 'jsdom',
      setupFiles: ['./src/test/setup.ts'],
      include: ['src/**/*.test.{ts,tsx}'],
      restoreMocks: true,
      unstubGlobals: true,
      coverage: {
        provider: 'v8',
        include: ['src/**/*.{ts,tsx}'],
        exclude: ['src/**/*.test.{ts,tsx}', 'src/test/**', 'src/main.tsx', 'src/**/*.d.ts'],
      },
    },
  };
});
