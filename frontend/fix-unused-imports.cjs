const fs = require('fs');
const path = require('path');

// Najde všechny .js, .jsx, .ts, .tsx soubory
function findFiles(dir, extensions = ['.js', '.jsx', '.ts', '.tsx']) {
  const files = [];
  
  function traverse(currentDir) {
    const items = fs.readdirSync(currentDir);
    
    for (const item of items) {
      const fullPath = path.join(currentDir, item);
      const stat = fs.statSync(fullPath);
      
      if (stat.isDirectory() && !item.startsWith('.') && item !== 'node_modules') {
        traverse(fullPath);
      } else if (stat.isFile() && extensions.some(ext => item.endsWith(ext))) {
        files.push(fullPath);
      }
    }
  }
  
  traverse(dir);
  return files;
}

// Odstraní nepoužívané importy ze souboru
function removeUnusedImports(filePath) {
  const content = fs.readFileSync(filePath, 'utf8');
  const lines = content.split('\n');
  const usedImports = new Set();
  const importLines = [];
  let hasChanges = false;
  
  // Najde všechny importy a jejich použití
  lines.forEach((line, index) => {
    if (line.trim().startsWith('import ') && !line.includes(' from ')) {
      // Skip: import './style.css' or import 'module'
      return;
    }
    
    if (line.trim().startsWith('import ')) {
      importLines.push({ line, index });
      
      // Extrahuje názvy importů
      const match = line.match(/import\s+(?:{([^}]+)}|(\w+))/);
      if (match) {
        if (match[1]) { // Named imports: { A, B, C }
          const namedImports = match[1].split(',').map(s => s.trim().split(' as ')[0]);
          namedImports.forEach(imp => {
            if (imp && !imp.includes('*')) {
              // Kontrola použití v kódu
              const isUsed = lines.some((l, i) => 
                i !== index && 
                (l.includes(imp + '(') || l.includes(imp + '.') || l.includes('<' + imp) || 
                 l.includes(' ' + imp + ' ') || l.includes('{' + imp + '}') ||
                 l.includes('=' + imp) || l.includes('(' + imp + ')'))
              );
              if (isUsed) usedImports.add(imp);
            }
          });
        } else if (match[2]) { // Default import
          const defaultImport = match[2];
          const isUsed = lines.some((l, i) => 
            i !== index && 
            (l.includes(defaultImport + '(') || l.includes(defaultImport + '.') || 
             l.includes('<' + defaultImport) || l.includes(' ' + defaultImport + ' '))
          );
          if (isUsed) usedImports.add(defaultImport);
        }
      }
    }
  });
  
  // Vytvoří nový obsah bez nepoužívaných importů
  const newLines = lines.map((line, index) => {
    const importLine = importLines.find(il => il.index === index);
    if (!importLine) return line;
    
    // Kontrola, jestli je import používán
    const match = line.match(/import\s+(?:{([^}]+)}|(\w+))/);
    if (match) {
      if (match[1]) { // Named imports
        const namedImports = match[1].split(',').map(s => s.trim());
        const usedNamedImports = namedImports.filter(imp => {
          const cleanImp = imp.split(' as ')[0].trim();
          return usedImports.has(cleanImp);
        });
        
        if (usedNamedImports.length === 0) {
          hasChanges = true;
          return ''; // Odstraní celý řádek
        } else if (usedNamedImports.length !== namedImports.length) {
          hasChanges = true;
          const fromPart = line.match(/from\s+['"]([^'"]+)['"]/);
          return `import { ${usedNamedImports.join(', ')} } from '${fromPart[1]}';`;
        }
      } else if (match[2]) { // Default import
        if (!usedImports.has(match[2])) {
          hasChanges = true;
          return ''; // Odstraní celý řádek
        }
      }
    }
    
    return line;
  });
  
  if (hasChanges) {
    // Odstraní prázdné řádky na začátku
    const cleanLines = newLines.filter((line, index) => {
      if (line.trim() === '' && index < 20) { // První 20 řádků
        const nextNonEmpty = newLines.slice(index + 1).find(l => l.trim() !== '');
        return nextNonEmpty && !nextNonEmpty.startsWith('import');
      }
      return true;
    });
    
    fs.writeFileSync(filePath, cleanLines.join('\n'));
    console.log(`✅ Opraveno: ${filePath}`);
    return true;
  }
  
  return false;
}

// Spustí opravu na všech souborech
const srcDir = './src';
const files = findFiles(srcDir);
let fixedCount = 0;

console.log(`🔍 Kontroluji ${files.length} souborů...`);

files.forEach(file => {
  try {
    if (removeUnusedImports(file)) {
      fixedCount++;
    }
  } catch (error) {
    console.error(`❌ Chyba v ${file}: ${error.message}`);
  }
});

console.log(`\n🎉 Opraveno ${fixedCount} souborů!`);
