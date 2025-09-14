import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react' // Ujistěte se, že tento balíček je nainstalován: npm install @vitejs/plugin-react --save-dev

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    port: 3000, // Nastavení portu na 3000
    host: true,
    strictPort: true,
    hmr: {
      host: 'localhost',
      clientPort: 3000,
      protocol: 'ws',
    },
    proxy: {
      '/api': {
        target: process.env.VITE_API_TARGET || 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
})
