#!/usr/bin/env node

/**
 * 🔍 Verify React Single Version
 * 
 * Checks that only one version of react and react-dom is installed.
 * Multiple versions can cause hooks errors and build issues.
 */

import { execSync } from 'child_process';

console.log('🔍 Verifying React single version...\n');

try {
  // Check React
  console.log('Checking react versions:');
  const reactOutput = execSync('npm ls react', { encoding: 'utf8' });
  const reactVersions = (reactOutput.match(/react@[\d.]+/g) || []);
  const uniqueReactVersions = [...new Set(reactVersions)];
  
  console.log('  Found:', reactVersions.join(', '));
  
  if (uniqueReactVersions.length > 1) {
    console.error('\n❌ Multiple React versions detected!');
    console.error('   Versions:', uniqueReactVersions.join(', '));
    console.error('\n   This can cause hooks errors!');
    console.error('   Run: npm dedupe');
    process.exit(1);
  }
  
  // Check React DOM
  console.log('\nChecking react-dom versions:');
  const reactDomOutput = execSync('npm ls react-dom', { encoding: 'utf8' });
  const reactDomVersions = (reactDomOutput.match(/react-dom@[\d.]+/g) || []);
  const uniqueReactDomVersions = [...new Set(reactDomVersions)];
  
  console.log('  Found:', reactDomVersions.join(', '));
  
  if (uniqueReactDomVersions.length > 1) {
    console.error('\n❌ Multiple React DOM versions detected!');
    console.error('   Versions:', uniqueReactDomVersions.join(', '));
    console.error('\n   This can cause hooks errors!');
    console.error('   Run: npm dedupe');
    process.exit(1);
  }
  
  console.log('\n✅ React version check passed!');
  console.log('   React:', uniqueReactVersions[0] || 'not found');
  console.log('   React DOM:', uniqueReactDomVersions[0] || 'not found');
  
  process.exit(0);
  
} catch (error) {
  console.error('\n⚠️ Error checking React versions:', error.message);
  
  // Don't fail if npm ls fails for other reasons
  if (error.message.includes('missing') || error.message.includes('extraneous')) {
    console.log('\n⚠️ Dependency tree has issues, but continuing...');
    process.exit(0);
  }
  
  process.exit(1);
}
