import axios from 'axios';
import logger from './logger.js';

class ApiService {
  constructor() {
    this.baseURL = window.location.origin;
    this.token = null;
    this.user = null;
    
    // Setup axios defaults
    axios.defaults.baseURL = this.baseURL;
    axios.defaults.headers.common['Content-Type'] = 'application/json';
    
    // Setup request interceptor
    axios.interceptors.request.use(
      (config) => {
        if (this.token) {
          config.headers.Authorization = `Bearer ${this.token}`;
        }
        logger.debug('🌐 API Request', {
          method: config.method?.toUpperCase(),
          url: config.url,
          hasAuth: !!this.token,
          tokenPrefix: this.token ? this.token.substring(0, 20) + '...' : null
        });
        return config;
      },
      (error) => {
        logger.error('❌ API Request Error', { error: error.message });
        return Promise.reject(error);
      }
    );

    // Setup response interceptor
    axios.interceptors.response.use(
      (response) => {
        logger.debug('✅ API Response', {
          status: response.status,
          url: response.config.url,
          hasData: !!response.data
        });
        return response;
      },
      (error) => {
        const status = error.response?.status;
        const message = error.response?.data?.message || error.message;
        
        logger.error('❌ API Error', {
          status,
          message,
          url: error.config?.url,
          responseData: error.response?.data
        });

        if (status === 401) {
          logger.warn('🚫 Unauthorized - clearing tokens');
          this.handleUnauthorized();
        }
        
        return Promise.reject(error);
      }
    );
  }

  setToken(token) {
    const oldToken = this.token;
    this.token = token;
    
    if (token) {
      localStorage.setItem('auth_token', token);
      logger.info('🔐 Token stored', { 
        tokenPrefix: token.substring(0, 20) + '...',
        hasOldToken: !!oldToken 
      });
    } else {
      localStorage.removeItem('auth_token');
      logger.info('🗑️ Token cleared');
    }
  }

  getStoredToken() {
    const token = localStorage.getItem('auth_token');
    logger.debug('📖 Retrieved stored token', { 
      hasToken: !!token,
      tokenPrefix: token ? token.substring(0, 20) + '...' : null
    });
    return token;
  }

  handleUnauthorized() {
    logger.warn('🚨 Handling unauthorized access');
    this.token = null;
    this.user = null;
    localStorage.removeItem('auth_token');
    // Redirect to Keycloak login will be handled by the App component
    window.location.reload();
  }

  // Auth endpoints
  async getUserInfo() {
    logger.info('👤 Getting user info...');
    try {
      const response = await axios.get('/api/auth/userinfo');
      this.user = response.data;
      logger.info('✅ User info retrieved', { 
        username: response.data?.username,
        tenant: response.data?.tenant,
        roles: response.data?.roles?.length || 0
      });
      return response.data;
    } catch (error) {
      logger.error('❌ Failed to get user info', { 
        status: error.response?.status,
        message: error.response?.data?.message || error.message 
      });
      throw error;
    }
  }

  async createSession(token) {
    logger.info('🔄 Creating session...', { 
      tokenPrefix: token ? token.substring(0, 20) + '...' : 'NO_TOKEN',
      hasToken: !!token
    });
    
    if (!token) {
      logger.error('❌ Cannot create session - no token provided');
      throw new Error('No token provided for session creation');
    }

    try {
      // Set token first so it's included in request
      this.setToken(token);
      
      // Create session with token in Authorization header
      const response = await axios.post('/api/auth/session', {}, {
        headers: {
          'Authorization': `Bearer ${token}`
        }
      });
      
      logger.info('✅ Session created successfully', { 
        username: response.data?.username,
        tenant: response.data?.tenant 
      });
      
      return response.data;
    } catch (error) {
      logger.error('❌ Failed to create session', { 
        status: error.response?.status,
        message: error.response?.data?.message || error.message,
        responseData: error.response?.data
      });
      // Clear token on session creation failure
      this.setToken(null);
      throw error;
    }
  }

  async logout() {
    logger.info('🚪 Logging out...');
    try {
      await axios.post('/api/auth/logout');
      logger.info('✅ Logout API call successful');
    } catch (error) {
      logger.warn('⚠️ Logout API call failed', { error: error.message });
    } finally {
      this.setToken(null);
      this.user = null;
      logger.info('🧹 Tokens and user data cleared');
    }
  }

  // User endpoints
  async getMe() {
    const response = await axios.get('/api/me');
    return response.data;
  }

  async updateMe(profileData) {
    const response = await axios.put('/api/me', profileData);
    return response.data;
  }

  async changeMyPassword(passwordData) {
    await axios.put('/api/me/password', passwordData);
  }

  // 🆕 CDC: Check for user data changes
  async checkUserChanges(since = null) {
    const params = since ? `?since=${since}` : '';
    const response = await axios.get(`/api/me/changes${params}`);
    return response.data;
  }

  // 🆕 USER DIRECTORY ENDPOINTS - pro běžné uživatele (čtení User Directory)
  async getUsersDirectory(params = {}) {
    const queryParams = new URLSearchParams();
    
    // Add search parameters
    if (params.q) queryParams.append('q', params.q);
    if (params.tenantKey) queryParams.append('tenantKey', params.tenantKey);
    if (params.source) queryParams.append('source', params.source);
    if (params.page !== undefined) queryParams.append('page', params.page);
    if (params.size !== undefined) queryParams.append('size', params.size);
    if (params.sort) queryParams.append('sort', params.sort);
    
    const url = `/api/users-directory${queryParams.toString() ? '?' + queryParams.toString() : ''}`;
    const response = await axios.get(url);
    return response.data;
  }

  async getUserDirectoryDetail(id) {
    const response = await axios.get(`/api/users-directory/${id}`);
    return response.data;
  }

  async updateUserDirectory(id, updates) {
    const response = await axios.patch(`/api/users-directory/${id}`, updates);
    return response.data;
  }

  // USER MANAGEMENT ENDPOINTS - pro admin uživatele (správa uživatelů přes Keycloak)
  async getUsers() {
    const response = await axios.get('/api/users');
    return response.data;
  }

  async createUser(userData) {
    const response = await axios.post('/api/users', userData);
    return response.data;
  }

  async updateUser(id, userData) {
    const response = await axios.put(`/api/users/${id}`, userData);
    return response.data;
  }

  async deleteUser(id) {
    await axios.delete(`/api/users/${id}`);
  }

  async getUserRoles(userId) {
    const response = await axios.get(`/api/users/${userId}/roles`);
    return response.data;
  }

  async assignRoleToUser(userId, roleData) {
    const response = await axios.post(`/api/users/${userId}/roles`, roleData);
    return response.data;
  }

  async removeRoleFromUser(userId, roleName) {
    await axios.delete(`/api/users/${userId}/roles/${roleName}`);
  }
  
  // User group endpoints
  async getUserGroups(userId) {
    const response = await axios.get(`/api/users/${userId}/groups`);
    return response.data;
  }

  async assignGroupToUser(userId, groupData) {
    const response = await axios.post(`/api/users/${userId}/groups`, groupData);
    return response.data;
  }

  async removeGroupFromUser(userId, groupName) {
    await axios.delete(`/api/users/${userId}/groups/${groupName}`);
  }

  async resetUserPassword(userId, passwordData) {
    const response = await axios.put(`/api/users/${userId}/password`, passwordData);
    return response.data;
  }

  // Role endpoints
  async getRoles() {
    const response = await axios.get('/api/roles');
    return response.data;
  }

  async getRolesByTenant(tenantKey) {
    const response = await axios.get(`/api/admin/roles/tenant/${tenantKey}`);
    return response.data;
  }

  async createRole(roleData) {
    const response = await axios.post('/api/roles', roleData);
    return response.data;
  }

  async updateRole(roleName, roleData) {
    const response = await axios.put(`/api/roles/${roleName}`, roleData);
    return response.data;
  }

  async deleteRole(roleName) {
    await axios.delete(`/api/roles/${roleName}`);
  }

  async getRoleComposites(roleName) {
    const response = await axios.get(`/api/roles/${roleName}/composites`);
    return response.data;
  }

  async addCompositeRole(parentRoleName, compositeData) {
    const response = await axios.post(`/api/roles/${parentRoleName}/composites`, compositeData);
    return response.data;
  }

  async removeCompositeRole(parentRoleName, childRoleName) {
    await axios.delete(`/api/roles/${parentRoleName}/composites/${childRoleName}`);
  }

  async getRoleUsers(roleName) {
    const response = await axios.get(`/api/roles/${roleName}/users`);
    return response.data;
  }
  
  // Group endpoints
  async getGroups() {
    const response = await axios.get('/api/groups');
    return response.data;
  }

  async createGroup(groupData) {
    const response = await axios.post('/api/groups', groupData);
    return response.data;
  }

  async updateGroup(groupName, groupData) {
    const response = await axios.put(`/api/groups/${groupName}`, groupData);
    return response.data;
  }

  async deleteGroup(groupName) {
    await axios.delete(`/api/groups/${groupName}`);
  }

  async getGroupMembers(groupName) {
    const response = await axios.get(`/api/groups/${groupName}/members`);
    return response.data;
  }

  // Tenant endpoints (admin only)
  async getTenants() {
    const response = await axios.get('/api/admin/tenants');
    // Backend vrací formát: { success: true, tenants: [...], total: N }
    return response.data.tenants || response.data;
  }

  // 🆕 TENANT INFO ENDPOINTS
  // ❌ REMOVED: getCurrentTenant() - už není potřeba, tenant je v user objektu

  async getAllTenants() {
    const response = await axios.get('/api/admin/tenants');
    return response.data.tenants || response.data;
  }

  async createTenant(tenantData) {
    const response = await axios.post('/api/admin/tenants', tenantData);
    return response.data;
  }

  async updateTenant(id, tenantData) {
    const response = await axios.put(`/api/admin/tenants/${id}`, tenantData);
    return response.data;
  }

  async deleteTenant(id) {
    await axios.delete(`/api/admin/tenants/${id}`);
  }

  async getTenantStats(tenantKey) {
    const response = await axios.get(`/api/admin/tenants/${tenantKey}/stats`);
    return response.data;
  }

  async getTenantUsers(tenantKey) {
    const response = await axios.get(`/api/admin/tenants/${tenantKey}/users`);
    return response.data;
  }
}

// Create singleton instance
const apiService = new ApiService();

export default apiService;