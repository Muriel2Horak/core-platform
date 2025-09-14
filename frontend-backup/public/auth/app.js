(function(){
  const cfg = window.KEYCLOAK_CFG;
  const kc = new Keycloak(cfg);

  const status = document.getElementById('status');
  const loginBtn = document.getElementById('loginBtn');
  const logoutBtn = document.getElementById('logoutBtn');
  const accountBtn = document.getElementById('accountBtn');
  const changePwdBtn = document.getElementById('changePwdBtn');
  const continueBtn = document.getElementById('continueBtn');
  const userBox = document.getElementById('user');
  const errBox = document.getElementById('error');

  const qsParams = new URLSearchParams(window.location.search);
  const continueTarget = qsParams.get('continue') || qsParams.get('continueUrl') || window.APP_URL || '/';
  const autoRedirect = qsParams.get('auto') === '1' || qsParams.get('redirect') === '1';

  const show = (el, on) => {
    if (!el) return;
    if (on) {
      el.style.display = el.tagName === 'A' ? 'inline-block' : '';
    } else {
      el.style.display = 'none';
    }
  };
  const setStatus = (t) => { status.textContent = t; };

  // URL pro přímou změnu hesla (vyžádá login a provede required action)
  function buildUpdatePasswordUrl(){
    const base = `${cfg.url}/realms/${cfg.realm}/protocol/openid-connect/auth`;
    const params = new URLSearchParams({
      client_id: cfg.clientId || 'web',
      redirect_uri: window.location.origin + '/auth/',
      response_type: 'code',
      scope: 'openid',
      kc_action: 'UPDATE_PASSWORD'
    });
    return `${base}?${params.toString()}`;
  }

  function renderUser(info){
    const roles = (kc.tokenParsed && kc.tokenParsed.realm_access && kc.tokenParsed.realm_access.roles) || [];
    const data = {
      username: info.preferred_username,
      name: info.name,
      email: info.email,
      roles,
      exp: kc.tokenParsed && kc.tokenParsed.exp
    };
    userBox.textContent = [
      `Jméno: ${data.name || data.username || '-'}`,
      `Email: ${data.email || '-'}`,
      `Role: ${(data.roles || []).join(', ') || 'bez rolí'}`
    ].join('\n');
  }

  function afterAuth(){
    show(loginBtn, false);
    show(logoutBtn, true);
    show(accountBtn, true);
    show(changePwdBtn, true);
    if (continueBtn) {
      continueBtn.href = continueTarget;
      show(continueBtn, true);
    }
    setStatus('Přihlášen');
    kc.loadUserInfo().then(info => { show(userBox, true); renderUser(info); });
    sessionStorage.setItem('kc_token', kc.token || '');
    sessionStorage.setItem('kc_refreshToken', kc.refreshToken || '');

    if (autoRedirect) {
      try { window.location.href = continueTarget; } catch (_) {}
    }
  }

  function afterLogout(){
    show(loginBtn, true);
    show(logoutBtn, false);
    show(accountBtn, false);
    show(changePwdBtn, false);
    if (continueBtn) show(continueBtn, false);
    show(userBox, false);
    setStatus('Odhlášen');
    sessionStorage.removeItem('kc_token');
    sessionStorage.removeItem('kc_refreshToken');
  }

  kc.init({ onLoad: 'check-sso', pkceMethod: 'S256', checkLoginIframe: false, silentCheckSsoRedirectUri: window.location.origin + '/auth/silent-check-sso.html' })
    .then(auth => { if (auth) afterAuth(); else setStatus('Nepřihlášen'); })
    .catch(err => { setStatus('Chyba inicializace'); errBox.textContent = String(err); show(errBox, true); });

  kc.onTokenExpired = () => { kc.updateToken(30)
    .then(() => {
      sessionStorage.setItem('kc_token', kc.token || '');
      sessionStorage.setItem('kc_refreshToken', kc.refreshToken || '');
    })
    .catch(() => kc.login()); };
  loginBtn.addEventListener('click', () => kc.login({ redirectUri: window.location.origin + '/auth/' }));
  logoutBtn.addEventListener('click', () => kc.logout({ redirectUri: window.location.origin + '/auth/' }).then(afterLogout));
  accountBtn.addEventListener('click', () => kc.accountManagement());
  changePwdBtn.addEventListener('click', () => { window.location.href = buildUpdatePasswordUrl(); });

  const loginForm = document.getElementById('loginForm');
  if (loginForm) {
    loginForm.addEventListener('submit', function(event) {
      event.preventDefault();
      const username = this.elements.username.value;
      const password = this.elements.password.value;

      fetch("/api/auth/login", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ username, password })
      })
      .then(response => response.json())
      .then(data => {
          if (data.error) {
              loginError(data.error);
          } else {
              loginSuccess(data);
          }
      })
      .catch(err => {
          loginError("Došlo k chybě při komunikaci se serverem.");
      });
    });
  }

  function loginSuccess(data) {
    // Přesměrování na hlavní stránku po úspěšném přihlášení
    window.location.href = "/";
  }

  function loginError(error) {
    const errorElement = document.getElementById('error');
    errorElement.textContent = `Chyba přihlášení: ${error}`;
    errorElement.style.display = 'block';
  }

})();
