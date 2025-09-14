(function(){
  const state = { el: null, user: null, roles: [] };

  async function api(path, options={}){
    const res = await fetch(path, { credentials: 'include', headers: { 'Content-Type': 'application/json' }, ...options });
    const ct = res.headers.get('content-type') || '';
    if (!res.ok) {
      const body = ct.includes('application/json') ? await res.json().catch(()=>({})) : await res.text().catch(()=> '');
      const msg = typeof body === 'object' && body && (body.detail || body.error_description || body.error) ? (body.detail || body.error_description || body.error) : (typeof body === 'string' ? body : `HTTP ${res.status}`);
      throw new Error(msg);
    }
    return ct.includes('application/json') ? res.json().catch(()=>({})) : res.text().catch(()=>'');
  }

  function qs(el, sel){ return el.querySelector(sel); }
  function show(el, on){ if (el) el.style.display = on ? '' : 'none'; }
  function setText(el, t){ if (el) el.textContent = t; }

  function render(el){
    el.innerHTML = [
      '<div class="authw">',
      '  <div class="row"><strong id="authw-status">Kontroluji…</strong></div>',
      '  <div class="row" id="authw-user" style="display:none"></div>',
      '  <div class="row" id="authw-login-form" style="display:none; gap:6px; align-items:center">',
      '    <input id="authw-username" placeholder="Uživatel" />',
      '    <input id="authw-password" type="password" placeholder="Heslo" />',
      '    <button id="authw-login">Přihlásit</button>',
      '  </div>',
      '  <div class="row" style="gap:6px;">',
      '    <button id="authw-logout" style="display:none">Odhlásit</button>',
      '    <button id="authw-account" style="display:none">Můj účet</button>',
      '  </div>',
      '  <div class="row" id="authw-error" style="color:#b91c1c"></div>',
      '</div>'
    ].join('');
  }

  function updateUI(el){
    const statusEl = qs(el, '#authw-status');
    const userEl = qs(el, '#authw-user');
    const formEl = qs(el, '#authw-login-form');
    const logoutBtn = qs(el, '#authw-logout');
    const accountBtn = qs(el, '#authw-account');

    if (state.user) {
      setText(statusEl, 'Přihlášen');
      userEl.innerHTML = [
        `<div><strong>Uživatel:</strong> ${state.user.preferred_username || '-'}</div>`,
        `<div><strong>Jméno:</strong> ${state.user.given_name || '-'}</div>`,
        `<div><strong>Příjmení:</strong> ${state.user.family_name || '-'}</div>`,
        `<div><strong>Email:</strong> ${state.user.email || '-'}</div>`,
        `<div><strong>Role:</strong> ${(state.roles||[]).join(', ') || 'bez rolí'}</div>`
      ].join('');
      show(userEl, true);
      show(formEl, false);
      show(logoutBtn, true);
      show(accountBtn, true);
    } else {
      setText(statusEl, 'Nepřihlášen');
      show(userEl, false);
      show(formEl, true);
      show(logoutBtn, false);
      show(accountBtn, false);
    }
  }

  async function loadMe(){
    try {
      const me = await api('/api/auth/me');
      console.log('AuthWidget: user loaded', me);
      state.user = me;
      state.roles = me.roles || [];
    } catch (e) {
      console.error('AuthWidget: failed to load user', e);
      state.user = null; state.roles = [];
    }
  }

  function attach(targetSelector){
    const el = document.querySelector(targetSelector);
    if (!el) throw new Error(`AuthWidget: target '${targetSelector}' not found`);
    state.el = el;
    render(el);

    const loginBtn = qs(el, '#authw-login');
    const logoutBtn = qs(el, '#authw-logout');
    const accountBtn = qs(el, '#authw-account');
    const errEl = qs(el, '#authw-error');

    if (loginBtn) loginBtn.onclick = async () => {
      errEl.textContent = '';
      const username = (qs(el, '#authw-username')||{}).value || '';
      const password = (qs(el, '#authw-password')||{}).value || '';
      try {
        await api('/api/auth/login', { method: 'POST', body: JSON.stringify({ username, password }) });
        await loadMe();
        updateUI(el);
      } catch (e){
        errEl.textContent = e && e.message ? String(e.message) : 'Přihlášení selhalo';
      }
    };

    if (logoutBtn) logoutBtn.onclick = async () => {
      try { await api('/api/auth/logout', { method: 'POST' }); } catch(_){}
      state.user = null; state.roles = [];
      updateUI(el);
    };

    if (accountBtn) accountBtn.onclick = () => {
      window.location.href = '/account';
    };

    loadMe().finally(() => updateUI(el));
  }

  window.AuthWidget = { attach };
})();
