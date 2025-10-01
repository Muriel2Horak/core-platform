import React, { Suspense } from 'react';
import ReactDOM from 'react-dom/client';
import App from './App';
import './App.css';
import { CacheProvider } from '@emotion/react';
import createCache from '@emotion/cache';
import { TenantProvider } from './context/TenantContext';

// 🎨 Vytvoření Emotion cache pro Material-UI s insertion point
const muiCache = createCache({
  key: 'mui',
  insertionPoint: document.querySelector('meta[name="emotion-insertion-point"]'),
});

// 🚧 DOČASNĚ VYPNUTO - Service Worker diagnostika React error #130
// Unregister všechny existující service workery pro čistý stav
if ('serviceWorker' in navigator) {
  navigator.serviceWorker.getRegistrations().then(function(registrations) {
    for(let registration of registrations) {
      registration.unregister().then((boolean) => {
        console.log(boolean ? '🗑️ Service Worker unregistered' : 'Service Worker not found', registration.scope);
      });
    }
  }).catch(function(err) {
    console.log('Service Worker registration failed: ', err);
  });

  // Vyčistit všechny cache
  if ('caches' in window) {
    caches.keys().then(function(names) {
      for (let name of names) {
        caches.delete(name).then(() => {
          console.log('🗑️ Cache deleted:', name);
        });
      }
    });
  }
  // Znovu načíst stránku po odregistraci, aby se změny projevily
  // window.location.reload();
}

// PŮVODNÍ SERVICE WORKER REGISTRACE - DOČASNĚ VYPNUTO
/*
if ('serviceWorker' in navigator && process.env.NODE_ENV === 'production') {
  window.addEventListener('load', () => {
    fetch('/sw.js', { method: 'HEAD' })
      .then(() => {
        navigator.serviceWorker.register('/sw.js')
          .then((registration) => {
            console.log('✅ Service Worker registered:', registration.scope);
          })
          .catch((error) => {
            console.log('❌ Service Worker registration failed:', error);
          });
      })
      .catch(() => {
        console.log('📝 Service Worker file not found, skipping registration');
      });
  });
}
*/

ReactDOM.createRoot(document.getElementById('root')).render(
  <CacheProvider value={muiCache}>
    <Suspense fallback={<div>Loading...</div>}>
      <TenantProvider>
        <App />
      </TenantProvider>
    </Suspense>
  </CacheProvider>,
)
