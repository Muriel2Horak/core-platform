/**
 * Casing utilities for naming lint validation
 */

/**
 * Check if string is PascalCase (e.g., User, UserDirectory)
 */
export function isPascalCase(str) {
  if (!str || typeof str !== 'string') return false;
  // Must start with uppercase, can contain uppercase/lowercase letters (no numbers/special chars)
  return /^[A-Z][a-zA-Z]*$/.test(str);
}

/**
 * Check if string is camelCase (e.g., firstName, userId)
 */
export function isCamelCase(str) {
  if (!str || typeof str !== 'string') return false;
  // Must start with lowercase, can contain uppercase/lowercase letters
  return /^[a-z][a-zA-Z0-9]*$/.test(str);
}

/**
 * Check if string is snake_case (e.g., first_name, user_id)
 */
export function isSnakeCase(str) {
  if (!str || typeof str !== 'string') return false;
  // Lowercase letters, numbers, underscores only
  return /^[a-z][a-z0-9_]*$/.test(str);
}

/**
 * Check if string is kebab-case (e.g., user-management, order-created)
 */
export function isKebabCase(str) {
  if (!str || typeof str !== 'string') return false;
  // Lowercase letters, numbers, hyphens only
  return /^[a-z][a-z0-9-]*$/.test(str);
}

/**
 * Check if string is UPPER_SNAKE_CASE (e.g., MAX_PAGE_SIZE)
 */
export function isUpperSnakeCase(str) {
  if (!str || typeof str !== 'string') return false;
  return /^[A-Z][A-Z0-9_]*$/.test(str);
}

/**
 * Check if string is plural (basic heuristic)
 */
export function isPlural(str) {
  if (!str || typeof str !== 'string') return false;
  return str.endsWith('s') || str.endsWith('ies');
}

/**
 * Convert PascalCase to snake_case
 */
export function pascalToSnake(str) {
  return str
    .replace(/([A-Z])/g, '_$1')
    .toLowerCase()
    .replace(/^_/, '');
}

/**
 * Convert PascalCase to kebab-case
 */
export function pascalToKebab(str) {
  return str
    .replace(/([A-Z])/g, '-$1')
    .toLowerCase()
    .replace(/^-/, '');
}

/**
 * Pluralize a word (basic rules)
 */
export function pluralize(str) {
  if (!str) return str;
  
  // Already plural
  if (isPlural(str)) return str;
  
  // Ends with 'y' → 'ies' (Company → Companies)
  if (str.endsWith('y') && !/[aeiou]y$/i.test(str)) {
    return str.slice(0, -1) + 'ies';
  }
  
  // Ends with 's', 'x', 'z', 'ch', 'sh' → add 'es'
  if (/[sxz]$|ch$|sh$/i.test(str)) {
    return str + 'es';
  }
  
  // Default: add 's'
  return str + 's';
}

/**
 * Singularize a word (basic rules)
 */
export function singularize(str) {
  if (!str) return str;
  
  // 'ies' → 'y' (companies → company)
  if (str.endsWith('ies')) {
    return str.slice(0, -3) + 'y';
  }
  
  // 'es' → '' for words ending in s/x/z/ch/sh
  if (str.endsWith('es') && /s|x|z|ch|sh$/i.test(str.slice(0, -2))) {
    return str.slice(0, -2);
  }
  
  // 's' → '' (users → user)
  if (str.endsWith('s')) {
    return str.slice(0, -1);
  }
  
  return str;
}
