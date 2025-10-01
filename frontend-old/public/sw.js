// 🚀 OPTIMALIZOVANÝ SERVICE WORKER pro CORE Platform
const CACHE_NAME = 'core-platform-v1.0';
const RUNTIME_CACHE = 'core-platform-runtime';

// Kritické zdroje pro okamžité cachování
const CRITICAL_RESOURCES = [
  '/',
  '/index.html',
  '/manifest.json',
  // Bude automaticky naplněno během build procesu
];

// Cache strategie pro různé typy souborů
const CACHE_STRATEGIES = {
  // Static assets - cache first s dlouhým TTL
  static: /\.(js|css|png|jpg|jpeg|gif|ico|svg|woff|woff2|ttf|eot)$/,
  // API calls - network first s fallback
  api: /^\/api\//,
  // HTML pages - stale while revalidate
  pages: /\.html$|^\/$/
};

// Install event - cache kritické zdroje
self.addEventListener('install', (event) => {
  console.log('🔧 Service Worker: Installing...');
  
  event.waitUntil(
    Promise.all([
      // Cache kritické zdroje
      caches.open(CACHE_NAME).then((cache) => {
        console.log('📦 Service Worker: Caching critical resources');
        return cache.addAll(CRITICAL_RESOURCES);
      }),
      // Příprava runtime cache
      caches.open(RUNTIME_CACHE)
    ]).catch((error) => {
      console.error('❌ Service Worker: Install failed:', error);
    })
  );
  
  // Aktivuj nový service worker okamžitě
  self.skipWaiting();
});

// Activate event - cleanup old caches
self.addEventListener('activate', (event) => {
  console.log('✅ Service Worker: Activating...');
  
  event.waitUntil(
    Promise.all([
      // Cleanup old caches
      caches.keys().then((cacheNames) => {
        return Promise.all(
          cacheNames
            .filter((cacheName) => 
              cacheName !== CACHE_NAME && 
              cacheName !== RUNTIME_CACHE
            )
            .map((cacheName) => {
              console.log('🗑️ Service Worker: Deleting old cache:', cacheName);
              return caches.delete(cacheName);
            })
        );
      }),
      // Převezmi kontrolu nad všemi stránkami
      self.clients.claim()
    ])
  );
});

// Fetch event - inteligentní cache strategie
self.addEventListener('fetch', (event) => {
  const { request } = event;
  const url = new URL(request.url);
  
  // Skip non-GET requests
  if (request.method !== 'GET') return;
  
  // Skip cross-origin requests (kromě fontů a CDN)
  if (url.origin !== location.origin && !isCDNResource(url)) return;
  
  // Skip Keycloak auth requests - musí být vždy fresh
  if (isAuthRequest(url)) return;
  
  // Aplikuj správnou cache strategii
  if (CACHE_STRATEGIES.static.test(url.pathname)) {
    event.respondWith(cacheFirstStrategy(request));
  } else if (CACHE_STRATEGIES.api.test(url.pathname)) {
    event.respondWith(networkFirstStrategy(request));
  } else if (CACHE_STRATEGIES.pages.test(url.pathname) || url.pathname === '/') {
    event.respondWith(staleWhileRevalidateStrategy(request));
  }
});

// 🚀 CACHE FIRST - pro static assets
async function cacheFirstStrategy(request) {
  try {
    const cachedResponse = await caches.match(request);
    if (cachedResponse) {
      return cachedResponse;
    }
    
    const networkResponse = await fetch(request);
    if (networkResponse.status === 200) {
      const cache = await caches.open(CACHE_NAME);
      cache.put(request, networkResponse.clone());
    }
    
    return networkResponse;
  } catch (error) {
    console.error('❌ Cache first strategy failed:', error);
    return new Response('Offline', { status: 503 });
  }
}

// 🚀 NETWORK FIRST - pro API calls
async function networkFirstStrategy(request) {
  try {
    const networkResponse = await fetch(request);
    
    if (networkResponse.status === 200) {
      const cache = await caches.open(RUNTIME_CACHE);
      cache.put(request, networkResponse.clone());
    }
    
    return networkResponse;
  } catch (error) {
    console.warn('🌐 Network failed, trying cache:', error);
    
    const cachedResponse = await caches.match(request);
    if (cachedResponse) {
      return cachedResponse;
    }
    
    return new Response(
      JSON.stringify({ error: 'Offline', cached: false }), 
      { 
        headers: { 'Content-Type': 'application/json' },
        status: 503 
      }
    );
  }
}

// 🚀 STALE WHILE REVALIDATE - pro HTML stránky
async function staleWhileRevalidateStrategy(request) {
  const cache = await caches.open(RUNTIME_CACHE);
  const cachedResponse = await cache.match(request);
  
  // Fetch v pozadí pro update
  const fetchPromise = fetch(request).then((networkResponse) => {
    if (networkResponse.status === 200) {
      cache.put(request, networkResponse.clone());
    }
    return networkResponse;
  }).catch(() => cachedResponse);
  
  // Vrať cached verzi okamžitě, pokud existuje
  return cachedResponse || await fetchPromise;
}

// Helper functions
function isCDNResource(url) {
  const cdnDomains = [
    'fonts.googleapis.com',
    'fonts.gstatic.com',
    'cdn.jsdelivr.net',
    'unpkg.com'
  ];
  return cdnDomains.some(domain => url.hostname.includes(domain));
}

function isAuthRequest(url) {
  return url.pathname.includes('/auth/') || 
         url.pathname.includes('/realms/') ||
         url.pathname.includes('/protocol/openid-connect/');
}

// 🚀 Background sync pro offline actions
self.addEventListener('sync', (event) => {
  if (event.tag === 'background-sync') {
    event.waitUntil(doBackgroundSync());
  }
});

async function doBackgroundSync() {
  console.log('🔄 Service Worker: Background sync triggered');
  // Implementace offline action synchronizace
}

// 🚀 Push notifications support
self.addEventListener('push', (event) => {
  if (event.data) {
    const data = event.data.json();
    event.waitUntil(
      self.registration.showNotification(data.title, {
        body: data.body,
        icon: '/icon-192.png',
        badge: '/badge-72.png'
      })
    );
  }
});

// 🚀 Performance monitoring
self.addEventListener('message', (event) => {
  if (event.data && event.data.type === 'PERFORMANCE_METRICS') {
    console.log('📊 Performance metrics:', event.data.metrics);
    // Odeslání metrik na analytics endpoint
  }
});