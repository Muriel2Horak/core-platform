const fs = require('fs');
const path = require('path');

function findAllFiles(dir) {
  const files = [];
  
  function traverse(currentDir) {
    const items = fs.readdirSync(currentDir);
    
    for (const item of items) {
      const fullPath = path.join(currentDir, item);
      const stat = fs.statSync(fullPath);
      
      if (stat.isDirectory() && !item.startsWith('.') && item !== 'node_modules') {
        traverse(fullPath);
      } else if (stat.isFile() && (item.endsWith('.jsx') || item.endsWith('.js') || item.endsWith('.ts') || item.endsWith('.tsx'))) {
        files.push(fullPath);
      }
    }
  }
  
  traverse(dir);
  return files;
}

function fixAllESLintIssues(filePath) {
  const content = fs.readFileSync(filePath, 'utf8');
  let lines = content.split('\n');
  let hasChanges = false;

  // 1. Odstranění nepoužívaných React importů
  if (content.includes("import React from 'react'") && !content.includes('React.') && !content.includes('<')) {
    lines = lines.filter(line => !line.trim().startsWith("import React from 'react'"));
    hasChanges = true;
  }

  // 2. Odstranění nepoužívaných PropTypes importů
  if (content.includes("import PropTypes from 'prop-types'") && !content.includes('PropTypes.') && !content.includes('.propTypes')) {
    lines = lines.filter(line => !line.trim().startsWith("import PropTypes from 'prop-types'"));
    hasChanges = true;
  }

  // 3. Přidání chybějícího UserPropType importu
  if (content.includes('UserPropType') && !content.includes("import { UserPropType }")) {
    // Najdi první import řádek
    let insertIndex = 0;
    for (let i = 0; i < lines.length; i++) {
      if (lines[i].trim().startsWith('import')) {
        insertIndex = i + 1;
      } else if (lines[i].trim().startsWith('import') && insertIndex === 0) {
        insertIndex = i;
        break;
      }
    }
    lines.splice(insertIndex, 0, "import { UserPropType } from '../shared/propTypes.js';");
    hasChanges = true;
  }

  // 4. Odstranění všech nepoužívaných MUI importů
  const muiComponents = [
    'Box', 'Card', 'CardContent', 'Typography', 'Grid', 'Avatar', 'Chip', 'Alert',
    'Button', 'TextField', 'IconButton', 'Tooltip', 'Skeleton', 'CircularProgress', 'Paper',
    'AppBar', 'Drawer', 'Toolbar', 'Menu', 'MenuItem', 'Divider', 'Badge', 'ListSubheader',
    'Table', 'TableBody', 'TableCell', 'TableContainer', 'TableHead', 'TableRow',
    'Dialog', 'DialogTitle', 'DialogContent', 'DialogActions', 'FormControl', 'InputLabel', 
    'Select', 'FormControlLabel', 'Switch', 'Slide', 'Fade', 'Pagination', 'InputAdornment',
    'Tabs', 'Tab'
  ];

  const muiIcons = [
    'PersonIcon', 'BusinessIcon', 'SecurityIcon', 'DashboardIcon', 'MenuIcon', 'LogoutIcon',
    'AccountCircleIcon', 'SwitchTenantIcon', 'DomainIcon', 'ExpandMoreIcon', 'CheckCircleIcon',
    'AddIcon', 'EditIcon', 'DeleteIcon', 'RefreshIcon', 'MoreVertIcon', 'VisibilityIcon',
    'CheckIcon', 'SearchIcon', 'PeopleIcon', 'CloudIcon', 'ServerIcon', 'FilterIcon',
    'ViewColumnIcon', 'DownloadIcon', 'EmailIcon', 'PhoneIcon', 'LocationIcon', 'ManagerIcon',
    'DeputyIcon', 'CostCenterIcon', 'CloseIcon', 'KeyIcon', 'CameraIcon', 'WorkIcon',
    'CheckCircle', 'Warning', 'Error', 'Info', 'Person', 'Email', 'Phone', 'LocationOn'
  ];

  const sharedComponents = [
    'SidebarNav', 'PageContainer', 'PageHeader', 'ContentCard', 'FormField', 'PrimaryButton',
    'SecondaryButton', 'DestructiveButton', 'LoadingSkeleton', 'LoadingSpinner', 'SuccessAlert',
    'WarningAlert', 'ErrorAlert', 'InfoAlert', 'ResponsiveGrid', 'EmptyState', 'AccessibleButton',
    'AppButton', 'Loader', 'WorkSection'
  ];

  const dndComponents = ['DndContext', 'DragOverlay', 'SortableContext'];

  const allUnusedComponents = [...muiComponents, ...muiIcons, ...sharedComponents, ...dndComponents];

  // Odstranění nepoužívaných importů pro každý komponent
  allUnusedComponents.forEach(component => {
    if (content.includes(component) && !isComponentUsed(content, component)) {
      lines = lines.map(line => {
        if (line.includes(`import`) && line.includes(component)) {
          // Odstranění komponenty z named imports
          return line.replace(new RegExp(`,?\\s*${component}(?:\\s+as\\s+\\w+)?(?=\\s*[,}])`), '')
                    .replace(new RegExp(`{\\s*${component}(?:\\s+as\\s+\\w+)?\\s*,?\\s*}`), '{}')
                    .replace(new RegExp(`{\\s*,?\\s*${component}(?:\\s+as\\s+\\w+)?\\s*}`), '{}');
        }
        return line;
      });
      hasChanges = true;
    }
  });

  // Odstranění prázdných import řádků
  lines = lines.filter(line => {
    const trimmed = line.trim();
    return !(trimmed === 'import {};' || trimmed === 'import { } from' || trimmed.match(/^import\s*{\s*}\s*from/));
  });

  // 5. Odstranění nepoužívaných proměnných v kódu
  const unusedVars = ['AppContent', 'GlobalStyles', 'ThemeProvider', 'CssBaseline', 'AuthProvider', 
                      'Dashboard', 'Users', 'UserDirectory', 'Profile', 'Tenants', 'TenantManagement',
                      'DataTablePage', 'KanbanPage', 'Router', 'Routes', 'Route', 'Navigate', 'App'];

  unusedVars.forEach(varName => {
    if (content.includes(varName) && !isVariableUsed(content, varName)) {
      lines = lines.filter(line => !line.includes(`${varName} =`) && !line.includes(`import ${varName}`));
      hasChanges = true;
    }
  });

  if (hasChanges) {
    // Vyčištění prázdných řádků na začátku
    while (lines.length > 0 && lines[0].trim() === '') {
      lines.shift();
    }
    
    fs.writeFileSync(filePath, lines.join('\n'));
    console.log(`✅ Opraveno: ${filePath}`);
    return true;
  }
  
  return false;
}

function isComponentUsed(content, component) {
  // Zkontroluj použití v JSX
  if (content.includes(`<${component}`)) return true;
  // Zkontroluj použití jako funkce
  if (content.includes(`${component}(`)) return true;
  // Zkontroluj použití v PropTypes
  if (content.includes(`${component}.`)) return true;
  return false;
}

function isVariableUsed(content, varName) {
  const lines = content.split('\n');
  let usageCount = 0;
  
  lines.forEach(line => {
    if (line.includes(varName)) {
      usageCount++;
    }
  });
  
  // Pokud je proměnná použita více než jednou (definice + použití), je aktivní
  return usageCount > 1;
}

// Speciální opravy pro konkrétní soubory
function fixSpecificFiles() {
  // Oprava logger.js - odstranění React importu
  const loggerPath = './src/services/logger.js';
  if (fs.existsSync(loggerPath)) {
    let content = fs.readFileSync(loggerPath, 'utf8');
    if (content.includes("import React from 'react'")) {
      content = content.replace("import React from 'react';\n", '');
      fs.writeFileSync(loggerPath, content);
      console.log(`✅ Opraveno: ${loggerPath}`);
    }
  }

  // Oprava theme.js - odstranění React importu
  const themePath = './src/styles/theme.js';
  if (fs.existsSync(themePath)) {
    let content = fs.readFileSync(themePath, 'utf8');
    if (content.includes("import React from 'react'")) {
      content = content.replace("import React from 'react';\n", '');
      fs.writeFileSync(themePath, content);
      console.log(`✅ Opraveno: ${themePath}`);
    }
  }
}

// Spustí opravu na všech souborech
const srcDir = './src';
const files = findAllFiles(srcDir);
let fixedCount = 0;

console.log(`🔧 Finální oprava všech ESLint problémů v ${files.length} souborech...`);

files.forEach(file => {
  try {
    if (fixAllESLintIssues(file)) {
      fixedCount++;
    }
  } catch (error) {
    console.error(`❌ Chyba v ${file}: ${error.message}`);
  }
});

// Speciální opravy
fixSpecificFiles();

console.log(`\n🎉 Finálně opraveno ${fixedCount} souborů pro dokonalý ESLint!`);
