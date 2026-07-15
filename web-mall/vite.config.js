import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'path'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [vue()],
  define: {
    'typeof global': 'typeof globalThis',
  },
  resolve: {
    alias: {
      '@': path.resolve(__dirname, 'src'),
      '@mall/shared-ui': path.resolve(__dirname, '../shared-ui')
    }
  },
  server: {
    host: '0.0.0.0',
    port: 5176,
    strictPort: true,
    open: true,
    historyApiFallback: true,
    proxy: {
      '/api': {
        target: 'http://localhost:8081',
        changeOrigin: true,
        secure: false
      },
      '/images': {
        target: 'http://localhost:8081',
        changeOrigin: true,
        secure: false
      },
      '/uploads': {
        target: 'http://localhost:8081',
        changeOrigin: true,
        secure: false
      },
      '/ws-chat': {
        target: 'http://localhost:8081',
        ws: true,
        changeOrigin: true
      }
    }
  }
})
