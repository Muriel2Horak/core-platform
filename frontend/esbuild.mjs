import { build, context } from 'esbuild';
import { copyFileSync, mkdirSync, existsSync, readdirSync } from 'fs';
import { join } from 'path';

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
  // Development mode with watch and dev server
  const ctx = await context({
    ...buildOptions,
    banner: {
      js: '(() => new EventSource("/esbuild").addEventListener("change", () => location.reload()))();',
    },
  });
  
  await ctx.watch();
  
  // Start dev server
  const { host, port } = await ctx.serve({
    servedir: 'dist',
    port: 8080,
    host: '0.0.0.0',
  });
  
  console.log(`🚀 Dev server running at http://${host}:${port}`);
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