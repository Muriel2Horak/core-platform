import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import { resolve } from 'path';
import fs from 'fs/promises';
import svgr from '@svgr/rollup';

// https://vitejs.dev/config/
export default defineConfig({
    server: {
        port: 3000,
        host: '0.0.0.0', // Důležité pro Docker
        proxy: {
            '/api': {
                target: process.env.VITE_API_TARGET || 'http://localhost:8080',
                changeOrigin: true,
                secure: false,
                cookieDomainRewrite: {
                    "*": "" // Přepíše doménu cookies pro správné fungování v Docker
                },
                headers: {
                    'X-Forwarded-Host': 'localhost:3000',
                    'X-Forwarded-Proto': 'http'
                }
            },
            // Proxy pro Loki API - umožňuje frontend logům jít přímo do container=core-frontend
            '/loki': {
                target: 'http://core-loki:3100', // Docker network name
                changeOrigin: true,
                secure: false,
                rewrite: (path) => path.replace(/^\/loki/, ''),
                headers: {
                    'Content-Type': 'application/json'
                }
            }
        }
    },
    resolve: {
        alias: {
            src: resolve(__dirname, 'src'),
        },
    },
    esbuild: {
        loader: 'jsx',
        include: /src\/.*\.jsx?$/,
        exclude: [],
    },
    optimizeDeps: {
        esbuildOptions: {
            plugins: [
                {
                    name: 'load-js-files-as-jsx',
                    setup(build) {
                        build.onLoad(
                            { filter: /src\/.*\.js$/ },
                            async (args) => ({
                                loader: 'jsx',
                                contents: await fs.readFile(args.path, 'utf8'),
                            })
                        );
                    },
                },
            ],
        },
    },

    plugins: [svgr(), react()],
});
