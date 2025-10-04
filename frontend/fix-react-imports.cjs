const fs = require('fs');
const path = require('path');

// React hooks, které mohou být potřeba
const REACT_HOOKS = [
  'useState', 'useEffect', 'useContext', 'useReducer', 'useCallback', 
  'useMemo', 'useRef', 'useImperativeHandle', 'useLayoutEffect', 'useDebugValue'
];

function findJSFiles(dir) {
  const files = [];
  
  function traverse(currentDir) {
    const items = fs.readdirSync(currentDir);
    
    for (const item of items) {
      const fullPath = path.join(currentDir, item);
      const stat = fs.statSync(fullPath);
      
      if (stat.isDirectory() && !item.startsWith('.') && item !== 'node_modules') {
        traverse(fullPath);
      } else if (stat.isFile() && (item.endsWith('.jsx') || item.endsWith('.js'))) {
        files.push(fullPath);
      }
    }
  }
  
  traverse(dir);
  return files;
}

function fixReactImports(filePath) {
  const content = fs.readFileSync(filePath, 'utf8');
  let lines = content.split('\n');
  let hasChanges = false;

  // Najdi všechny použité React hooks v kódu
  const usedHooks = new Set();
  const codeWithoutImports = lines.slice(10).join('\n'); // Skip import section
  
  REACT_HOOKS.forEach(hook => {
    if (codeWithoutImports.includes(hook + '(')) {
      usedHooks.add(hook);
    }
  });

  // Najdi první import React řádek
  let reactImportIndex = -1;
  let hasReactImport = false;
  
  for (let i = 0; i < lines.length; i++) {
    const line = lines[i];
    if (line.trim().startsWith("import React") || line.trim().startsWith("import { ") && line.includes("} from 'react'")) {
      reactImportIndex = i;
      hasReactImport = true;
      break;
    }
  }

  // Pokud je potřeba React import a není tam
  const needsReact = content.includes('<') || content.includes('useState') || content.includes('useEffect');
  
  if (needsReact && !hasReactImport) {
    // Přidej React import na začátek
    if (usedHooks.size > 0) {
      lines.unshift(`import React, { ${Array.from(usedHooks).join(', ')} } from 'react';`);
    } else {
      lines.unshift(`import React from 'react';`);
    }
    hasChanges = true;
  } else if (hasReactImport && usedHooks.size > 0) {
    // Aktualizuj existující React import
    const hooksArray = Array.from(usedHooks);
    const newImport = `import React, { ${hooksArray.join(', ')} } from 'react';`;
    
    if (lines[reactImportIndex] !== newImport) {
      lines[reactImportIndex] = newImport;
      hasChanges = true;
    }
  }

  // Zkontroluj PropTypes import
  const needsPropTypes = content.includes('.propTypes') || content.includes('PropTypes.');
  const hasPropTypes = content.includes("import PropTypes from 'prop-types'");
  
  if (needsPropTypes && !hasPropTypes) {
    // Najdi místo pro PropTypes import (za React importem)
    let insertIndex = reactImportIndex >= 0 ? reactImportIndex + 1 : 1;
    lines.splice(insertIndex, 0, "import PropTypes from 'prop-types';");
    hasChanges = true;
  }

  if (hasChanges) {
    fs.writeFileSync(filePath, lines.join('\n'));
    console.log(`✅ Opraveno: ${filePath}`);
    return true;
  }
  
  return false;
}

// Spustí opravu na všech .js/.jsx souborech
const srcDir = './src';
const files = findJSFiles(srcDir);
let fixedCount = 0;

console.log(`🔧 Opravuji React importy v ${files.length} souborech...`);

files.forEach(file => {
  try {
    if (fixReactImports(file)) {
      fixedCount++;
    }
  } catch (error) {
    console.error(`❌ Chyba v ${file}: ${error.message}`);
  }
});

console.log(`\n🎉 Opraveno ${fixedCount} souborů!`);
