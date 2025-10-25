import https from 'https';
import fs from 'fs';

// Read cookies from Playwright storage state
try {
  const state = JSON.parse(fs.readFileSync('.auth/test_admin.json', 'utf8'));
  const cookies = state.cookies || [];
  const atCookie = cookies.find(c => c.name === 'at');
  
  if (!atCookie) {
    console.log('❌ No AT cookie found in storage state');
    process.exit(1);
  }
  
  const cookieHeader = `at=${atCookie.value}`;
  console.log('🍪 Using AT cookie (first 50 chars):', atCookie.value.substring(0, 50) + '...');
  
  const options = {
    hostname: 'admin.core-platform.local',
    port: 443,
    path: '/internal/auth/grafana',
    method: 'GET',
    headers: {
      'Cookie': cookieHeader
    },
    rejectUnauthorized: false
  };
  
  console.log('🔍 Testing endpoint:', `https://${options.hostname}${options.path}`);
  
  const req = https.request(options, (res) => {
    console.log('📥 Response status:', res.statusCode);
    console.log('📋 Response headers:');
    Object.keys(res.headers).forEach(key => {
      if (key.toLowerCase().includes('grafana') || key.toLowerCase().includes('jwt') || key.toLowerCase().includes('org')) {
        console.log(`   ${key}: ${res.headers[key]}`);
      }
    });
    
    let data = '';
    res.on('data', chunk => data += chunk);
    res.on('end', () => {
      if (data) console.log('📄 Response body:', data.substring(0, 200));
    });
  });
  
  req.on('error', (e) => {
    console.error('❌ Request error:', e.message);
  });
  
  req.end();
} catch (e) {
  console.error('❌ Error:', e.message);
}
