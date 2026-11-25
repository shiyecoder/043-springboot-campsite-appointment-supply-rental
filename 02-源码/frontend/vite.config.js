import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// 前端端口固定 5171（通道 2），/api 与 /uploads 代理到本通道后端 8081
export default defineConfig({
  plugins: [vue()],
  server: {
    host: 'localhost',
    port: 5171,
    strictPort: true,
    proxy: {
      '/api': { target: 'http://127.0.0.1:8081', changeOrigin: true },
      '/uploads': { target: 'http://127.0.0.1:8081', changeOrigin: true }
    }
  },
  build: { outDir: 'dist', chunkSizeWarningLimit: 1500 }
})
