import { build } from 'esbuild';
import { readFileSync, writeFileSync, mkdirSync, existsSync, readdirSync, statSync, copyFileSync } from 'fs';
import { resolve, dirname, join, extname } from 'path';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);

console.log(`🚀 Building frontend in PRODUCTION mode...`);

// Funkce pro kopírování souborů
function copyDir(src, dest) {
  if (!existsSync(dest)) {
    mkdirSync(dest, { recursive: true });
  }
  
  const files = readdirSync(src);
  for (const file of files) {
    const srcPath = join(src, file);
    const destPath = join(dest, file);
    
    if (statSync(srcPath).isDirectory()) {
      copyDir(srcPath, destPath);
    } else {
      copyFileSync(srcPath, destPath);
    }
  }
}

// SVG Plugin pro ESBuild
const svgrPlugin = {
  name: 'svgr',
  setup(build) {
    build.onLoad({ filter: /\.svg$/ }, async (args) => {
      const svgContent = readFileSync(args.path, 'utf8');
      
      const componentName = args.path.split('/').pop().replace('.svg', '').replace(/[^a-zA-Z0-9]/g, '');
      const reactComponent = `
import React from 'react';
const ${componentName} = (props) => (
  ${svgContent.replace('<svg', '<svg {...props}')}
);
export default ${componentName};
      `.trim();
      
      return {
        contents: reactComponent,
        loader: 'jsx'
      };
    });
  }
};

// Alias plugin
const aliasPlugin = {
  name: 'alias',
  setup(build) {
    build.onResolve({ filter: /^src\// }, (args) => ({
      path: resolve(__dirname, args.path.replace('src/', 'src/'))
    }));
  }
};

async function buildApp() {
  try {
    console.log('📖 Reading HTML template...');
    
    const htmlTemplatePath = resolve(__dirname, 'index.html');
    if (!existsSync(htmlTemplatePath)) {
      throw new Error(`HTML template not found at: ${htmlTemplatePath}`);
    }
    
    const htmlTemplate = readFileSync(htmlTemplatePath, 'utf8');
    console.log(`✅ HTML template loaded (${htmlTemplate.length} bytes)`);
    
    // Ensure dist directory exists
    const distPath = resolve(__dirname, 'dist');
    if (!existsSync(distPath)) {
      mkdirSync(distPath, { recursive: true });
      console.log('📁 Created dist directory');
    }
    
    // Najdi správný entry point
    const possibleEntries = ['src/main.jsx', 'src/main.tsx', 'src/index.jsx', 'src/index.tsx'];
    let entryPoint = null;
    
    for (const entry of possibleEntries) {
      if (existsSync(resolve(__dirname, entry))) {
        entryPoint = entry;
        break;
      }
    }
    
    if (!entryPoint) {
      throw new Error(`No entry point found. Tried: ${possibleEntries.join(', ')}`);
    }
    
    console.log(`🔨 Running ESBuild in PRODUCTION mode...`);
    console.log(`📍 Entry point: ${entryPoint}`);
    
    const result = await build({
      entryPoints: [entryPoint],
      bundle: true,
      outdir: 'dist',
      
      // Production optimalizace
      minify: true,
      sourcemap: false,
      
      // React JSX
      jsx: 'automatic',
      jsxImportSource: 'react',
      
      // OPRAVA: Zakázání code splitting pro stabilitu
      splitting: false,
      format: 'iife', // Změna z 'esm' na 'iife'
      globalName: 'App',
      
      // Plugins
      plugins: [svgrPlugin, aliasPlugin],
      
      // External dependencies - žádné pro self-contained build
      external: [],
      
      // Production definice
      define: {
        'process.env.NODE_ENV': '"production"',
        'import.meta.env.MODE': '"production"',
        'import.meta.env.DEV': 'false',
        'import.meta.env.PROD': 'true'
      },
      
      // Asset handling
      loader: {
        '.png': 'file',
        '.jpg': 'file',
        '.jpeg': 'file',
        '.gif': 'file',
        '.webp': 'file',
        '.ico': 'file',
        '.woff': 'file',
        '.woff2': 'file',
        '.ttf': 'file',
        '.eot': 'file'
      },
      
      // Production chunk naming - jednodušší naming
      entryNames: '[name]',
      assetNames: '[name]-[hash]',
      
      // Target na moderní browsery
      target: ['es2020', 'chrome80', 'firefox78', 'safari14'],
      
      metafile: true
    });
    
    console.log('✅ ESBuild completed');
    
    // Zkopíruj statické soubory z public/
    console.log('📁 Copying static files from public/...');
    const publicPath = resolve(__dirname, 'public');
    if (existsSync(publicPath)) {
      copyDir(publicPath, distPath);
      console.log('✅ Static files copied');
    }
    
    // Generuj index.html s odkazy na skutečné soubory
    let outputHtml = htmlTemplate;
    
    console.log('📁 Scanning dist directory for generated files...');
    
    if (existsSync(distPath)) {
      const files = readdirSync(distPath);
      console.log(`📦 Found files in dist:`, files);
      
      // Najdi hlavní JS soubor (obvykle main-[hash].js)
      const jsFiles = files.filter(f => f.endsWith('.js') && !f.includes('chunk') && f.includes('main'));
      const cssFiles = files.filter(f => f.endsWith('.css'));
      
      console.log(`🟨 Main JS files:`, jsFiles);
      console.log(`🟦 CSS files:`, cssFiles);
      
      // Nahraď script tag s reálným souborem
      if (jsFiles.length > 0) {
        const mainJsFile = jsFiles[0]; // vezmi první (měl by být jen jeden)
        outputHtml = outputHtml.replace(
          /<script[^>]*src\s*=\s*["'][^"']*main\.js["'][^>]*><\/script>/gi,
          `<script src="/${mainJsFile}"></script>`
        );
      }
      
      // Přidej CSS soubory
      if (cssFiles.length > 0) {
        const cssLinks = cssFiles.map(f => `<link rel="stylesheet" href="/${f}">`).join('\n    ');
        outputHtml = outputHtml.replace('</head>', `    ${cssLinks}\n  </head>`);
      }
    }
    
    console.log('📝 Writing final HTML...');
    const outputPath = resolve(__dirname, 'dist/index.html');
    
    // Fallback HTML pokud něco selhalo
    if (!outputHtml || outputHtml.trim().length === 0) {
      console.warn('⚠️ HTML template is empty, using fallback');
      outputHtml = `<!DOCTYPE html>
<html lang="cs">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Core Platform</title>
</head>
<body>
    <div id="root"></div>
    <script src="/main.js"></script>
</body>
</html>`;
    }
    
    writeFileSync(outputPath, outputHtml, 'utf8');
    
    // Ověření
    const finalSize = statSync(outputPath).size;
    console.log(`✅ HTML file written: ${outputPath} (${finalSize} bytes)`);
    
    if (finalSize === 0) {
      throw new Error('Generated HTML file is empty!');
    }
    
    // Výpis metadat o buildu
    if (result.metafile) {
      console.log('📊 Build stats:');
      const outputs = Object.keys(result.metafile.outputs);
      outputs.forEach(output => {
        const info = result.metafile.outputs[output];
        console.log(`  ${output}: ${Math.round(info.bytes / 1024)}KB`);
      });
    }
    
    console.log(`✅ PRODUCTION build completed successfully!`);
    console.log(`📍 Entry point used: ${entryPoint}`);
    
  } catch (error) {
    console.error('❌ Build failed:', error);
    process.exit(1);
  }
}

buildApp();