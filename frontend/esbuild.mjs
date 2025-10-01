import { build } from 'esbuild';
import { readFileSync, writeFileSync, copyFileSync, mkdirSync, existsSync, readdirSync, statSync } from 'fs';
import { join, dirname } from 'path';

const isDev = process.argv.includes('--watch');
const isProduction = !isDev;

console.log(`🔧 Building in ${isProduction ? 'PRODUCTION' : 'DEVELOPMENT'} mode`);

// Ensure dist directory exists
if (!existsSync('dist')) {
  mkdirSync('dist', { recursive: true });
}

// Copy public files
try {
  if (existsSync('public')) {
    const copyRecursive = (src, dest) => {
      if (!existsSync(dest)) {
        mkdirSync(dest, { recursive: true });
      }
      const entries = readdirSync(src, { withFileTypes: true });
      for (const entry of entries) {
        const srcPath = join(src, entry.name);
        const destPath = join(dest, entry.name);
        if (entry.isDirectory()) {
          copyRecursive(srcPath, destPath);
        } else {
          copyFileSync(srcPath, destPath);
        }
      }
    };
    copyRecursive('public', 'dist');
  }
  console.log('✅ Public files copied successfully!');
} catch (err) {
  console.warn('⚠️ Could not copy public files:', err.message);
}

const buildOptions = {
  entryPoints: ['src/main.jsx'],
  bundle: true,
  outfile: 'dist/bundle.js',
  format: 'iife',
  target: 'es2020',
  platform: 'browser',
  sourcemap: isDev,
  minify: isProduction,
  define: {
    'process.env.NODE_ENV': JSON.stringify(isProduction ? 'production' : 'development'),
    global: 'globalThis'
  },
  jsx: 'automatic',
  jsxImportSource: 'react',
  loader: {
    '.js': 'jsx',
    '.jsx': 'jsx',
    '.ts': 'tsx',
    '.tsx': 'tsx',
    '.css': 'css',
    '.png': 'file',
    '.jpg': 'file',
    '.jpeg': 'file',
    '.svg': 'dataurl'
  },
  external: [],
  logLevel: 'info'
};

if (isDev) {
  // Development mode with watch
  const context = await build({
    ...buildOptions,
    watch: {
      onRebuild(error, result) {
        if (error) {
          console.error('❌ Build failed:', error);
        } else {
          console.log('✅ Build succeeded');
        }
      }
    }
  });
  console.log('👀 Watching for changes...');
} else {
  // Production build
  try {
    await build(buildOptions);
    console.log('✅ Build completed successfully!');
  } catch (err) {
    console.error('❌ Build failed:', err);
    process.exit(1);
  }
}