import { build } from 'esbuild';
import { copyFileSync, existsSync, mkdirSync, cpSync } from 'fs';
import { join } from 'path';

// Ensure dist directory exists
if (!existsSync('dist')) {
  mkdirSync('dist', { recursive: true });
}
if (!existsSync('dist/assets')) {
  mkdirSync('dist/assets', { recursive: true });
}

// 🚧 DEVELOPMENT BUILD - diagnostika React error #130
const isDevelopment = process.env.NODE_ENV !== 'production';

console.log(`🔧 Building in ${isDevelopment ? 'DEVELOPMENT' : 'PRODUCTION'} mode`);

await build({
  entryPoints: ['src/main.jsx'],
  bundle: true,
  sourcemap: true, // Vždy zapnuto pro diagnostiku
  minify: !isDevelopment, // Vypnuto v development
  outdir: 'dist/assets',
  entryNames: 'main',
  loader: {
    '.js': 'jsx',
    '.jsx': 'jsx',
    '.png': 'file',
    '.svg': 'file',
    '.jpg': 'file',
    '.css': 'css'
  },
  define: {
    // Správné nastavení NODE_ENV pro diagnostiku
    'process.env.NODE_ENV': isDevelopment ? '"development"' : '"production"'
  },
  // Development optimalizace
  ...(isDevelopment && {
    logLevel: 'verbose',
    keepNames: true, // Zachovat názvy funkcí pro debugging
    target: ['es2020'],
  })
});

// Copy index.html to dist
copyFileSync('index.html', 'dist/index.html');

// Copy all files from public/ directory to dist/
if (existsSync('public')) {
  try {
    cpSync('public', 'dist', { recursive: true });
    console.log('✅ Public files copied successfully!');
  } catch (error) {
    console.warn('⚠️ Warning: Could not copy public files:', error.message);
  }
}

console.log('✅ Build completed successfully!');