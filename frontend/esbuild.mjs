import { build, context } from 'esbuild';
import { copyFileSync, mkdirSync, existsSync, readdirSync } from 'fs';
import { join, resolve } from 'path';

const isDev = process.argv.includes('--watch');
const isProduction = !isDev;

console.log(`🔧 Building in ${isProduction ? 'PRODUCTION' : 'DEVELOPMENT'} mode`);

// Plugin to resolve .gen files without extension  
const grafanaSchemaResolverPlugin = {
  name: 'grafana-schema-resolver',
  setup(build) {
    build.onResolve({ filter: /\.gen$/ }, args => {
      // Resolve relative to node_modules
      const resolvedPath = resolve(join('node_modules', args.path + '.js'));
      return {
        path: resolvedPath,
        external: false
      };
    });
  }
};

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
    'import.meta.env.VITE_STREAMING_ENABLED': JSON.stringify(process.env.STREAMING_ENABLED || 'false'),
    'import.meta.env.VITE_GRAFANA_PUBLIC_URL': JSON.stringify(process.env.GRAFANA_PUBLIC_URL || 'https://grafana.core-platform.local'),
    'import.meta.env.VITE_API_BASE_URL': JSON.stringify(process.env.API_BASE_URL || '/api'),
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
    '.svg': 'dataurl',
    '.ttf': 'file',
    '.woff': 'file',
    '.woff2': 'file'
  },
  resolveExtensions: ['.tsx', '.ts', '.jsx', '.js', '.json', '.gen.js', '.gen.ts'],
  external: [],
  logLevel: 'info',
  plugins: [grafanaSchemaResolverPlugin]
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