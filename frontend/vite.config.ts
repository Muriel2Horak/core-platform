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
        // 🔒 SSL/HTTPS podpora pro lokální vývoj
        allowedHosts: [
            'localhost',
            '127.0.0.1',
            'core-platform.local', // 🔑 Naše lokální SSL doména
            '.core-platform.local'   // Wildcard pro subdomény
        ],
        // Pro případné budoucí HTTPS nastavení přímo ve Vite
        // https: {
        //     key: fs.readFileSync('../ssl/key.pem'),
        //     cert: fs.readFileSync('../ssl/cert.pem'),
        // },
        // 🔧 ŽÁDNÁ PROXY - Nginx se postará o routing!
        // V nové architektuře Vite slouží jen jako dev server pro frontend
        // Všechny API requesty jdou přes Nginx proxy
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
