import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vitejs.dev/config/
export default defineConfig(({ mode }) => {
  const devPort = mode === 'development' ? 3002 : undefined;

  return {
    plugins: [react()],
    build: {
      outDir: 'dist',
      rollupOptions: {
        input: {
          // Keycloak theme entry points
          'login-entry': './src/login-entry.tsx',
          'account-entry': './src/account-entry.tsx',
        },
        output: {
          // Keycloak očekává specifickou strukturu souborů
          entryFileNames: 'resources/js/[name].js',
          chunkFileNames: 'resources/js/[name]-[hash].js',
          assetFileNames: (assetInfo) => {
            if (assetInfo.name?.endsWith('.css')) {
              return 'resources/css/[name].[ext]'
            }
            return 'resources/[name].[ext]'
          }
        }
      },
      // Optimalizace pro produkci
      minify: true,
      sourcemap: false,
      // Kompatibilita s Keycloak
      target: 'es2015'
    },
    // Pro development server
    server: {
      port: devPort,
      host: true
    }
  }
})