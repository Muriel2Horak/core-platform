// User Directory API Service - nová funkcionalita pre všetkých užívateľov
import authService from './auth';
import logger from './logger';

class UsersDirectoryService {
  constructor() {
    this.baseUrl = '/users-directory'; // OPRAVENO: Bez /api prefix - authService ho automaticky přidává
  }

  // ================== Main Directory API ==================

  /**
   * Vyhľadávanie v User Directory s paginaciou a filtrami
   * @param {Object} options - { q, tenantId, source, page, size, sort }
   */
  async searchDirectory(options = {}) {
    try {
      const params = new URLSearchParams();

      // Query parameters
      if (options.q) params.append('q', options.q);
      if (options.tenantId) params.append('tenantId', options.tenantId);
      if (options.source) params.append('source', options.source);
      if (options.page !== undefined) params.append('page', options.page);
      if (options.size !== undefined) params.append('size', options.size);
      if (options.sort) params.append('sort', options.sort);

      // OPRAVENO: Bez /directory na konci - backend má endpoint na /api/users-directory
      const url = `${this.baseUrl}${params.toString() ? '?' + params.toString() : ''}`;
      logger.apiCall('GET', url, 'start', 0, { operation: 'search_directory', ...options });
      const startTime = Date.now();

      const response = await authService.apiCall(url);
      if (!response) throw new Error('Failed to search directory');

      const data = await response.json();
      const duration = Date.now() - startTime;

      logger.apiCall('GET', url, response.status, duration, {
        operation: 'search_directory',
        success: true,
        resultCount: data.content?.length || 0,
        totalElements: data.totalElements
      });

      return data;
    } catch (error) {
      logger.error('SEARCH_DIRECTORY_ERROR', error.message, { error: error.stack, options });
      throw error;
    }
  }

  /**
   * Získať detail užívateľa z Directory
   */
  async getUserDetail(userId) {
    try {
      logger.apiCall('GET', `${this.baseUrl}/${userId}`, 'start', 0, { operation: 'get_user_detail' });
      const startTime = Date.now();

      const response = await authService.apiCall(`${this.baseUrl}/${userId}`);
      if (!response) throw new Error('Failed to get user detail');

      const data = await response.json();
      const duration = Date.now() - startTime;

      logger.apiCall('GET', `${this.baseUrl}/${userId}`, response.status, duration, {
        operation: 'get_user_detail',
        success: true
      });

      return data;
    } catch (error) {
      logger.error('GET_USER_DETAIL_ERROR', error.message, { error: error.stack, userId });
      throw error;
    }
  }

  /**
   * Aktualizovať užívateľa v Directory
   */
  async updateUser(userId, updates) {
    try {
      logger.apiCall('PATCH', `${this.baseUrl}/${userId}`, 'start', 0, { operation: 'update_directory_user' });
      const startTime = Date.now();

      const response = await authService.apiCall(`${this.baseUrl}/${userId}`, {
        method: 'PATCH',
        body: JSON.stringify(updates)
      });

      if (!response) throw new Error('Failed to update user');

      const data = await response.json();
      const duration = Date.now() - startTime;

      logger.apiCall('PATCH', `${this.baseUrl}/${userId}`, response.status, duration, {
        operation: 'update_directory_user',
        success: true
      });

      logger.userAction('DIRECTORY_USER_UPDATED', {
        userId,
        fields: Object.keys(updates)
      });

      return data;
    } catch (error) {
      logger.error('UPDATE_DIRECTORY_USER_ERROR', error.message, { error: error.stack, userId, updates });
      throw error;
    }
  }

  /**
   * Zmazať užívateľa z Directory (len pre adminov)
   */
  async deleteUser(userId) {
    try {
      logger.apiCall('DELETE', `${this.baseUrl}/${userId}`, 'start', 0, { operation: 'delete_directory_user' });
      const startTime = Date.now();

      const response = await authService.apiCall(`${this.baseUrl}/${userId}`, {
        method: 'DELETE'
      });

      if (!response) throw new Error('Failed to delete user');

      const duration = Date.now() - startTime;

      logger.apiCall('DELETE', `${this.baseUrl}/${userId}`, response.status, duration, {
        operation: 'delete_directory_user',
        success: true
      });

      logger.userAction('DIRECTORY_USER_DELETED', { userId });

      return true;
    } catch (error) {
      logger.error('DELETE_DIRECTORY_USER_ERROR', error.message, { error: error.stack, userId });
      throw error;
    }
  }

  // ================== Tenants API (for core-admin filter) ==================

  /**
   * Získať zoznam všetkých tenantov (len pre core-admin)
   */
  async getAllTenants() {
    try {
      logger.apiCall('GET', '/tenants', 'start', 0, { operation: 'get_all_tenants' });
      const startTime = Date.now();

      const response = await authService.apiCall('/tenants');
      if (!response) throw new Error('Failed to get tenants');

      const data = await response.json();
      const duration = Date.now() - startTime;

      logger.apiCall('GET', '/tenants', response.status, duration, {
        operation: 'get_all_tenants',
        success: true,
        resultCount: data.length
      });

      return data;
    } catch (error) {
      logger.error('GET_ALL_TENANTS_ERROR', error.message, { error: error.stack });
      throw error;
    }
  }

  // ================== Helper Methods ==================

  /**
   * Permission helper - môže užívateľ editovať iného užívateľa?
   */
  canEditUser(user, currentUser) {
    if (!user || !currentUser) return false;

    // Je to môj profil?
    const isMe = user.keycloakUserId === currentUser.sub || user.username === currentUser.preferred_username;
    if (isMe) return true;

    // Som core admin?
    const isCoreAdmin = currentUser.roles?.includes('CORE_ROLE_ADMIN');
    if (isCoreAdmin) return true;

    // Som tenant admin v rovnakom tenante?
    const isTenantAdmin = currentUser.roles?.includes('CORE_ROLE_TENANT_ADMIN') ||
      currentUser.roles?.includes('CORE_ROLE_USER_MANAGER');
    const sameTenant = user.tenantId === currentUser.tenant_id;

    return isTenantAdmin && sameTenant;
  }

  /**
   * Permission helper - môže užívateľ zmazať iného užívateľa?
   */
  canDeleteUser(user, currentUser) {
    if (!user || !currentUser) return false;

    // Core admin môže zmazať kohokoľvek
    const isCoreAdmin = currentUser.roles?.includes('CORE_ROLE_ADMIN');
    if (isCoreAdmin) return true;

    // Tenant admin môže zmazať len v svojom tenante
    const isTenantAdmin = currentUser.roles?.includes('CORE_ROLE_TENANT_ADMIN') ||
      currentUser.roles?.includes('CORE_ROLE_USER_MANAGER');
    const sameTenant = user.tenantId === currentUser.tenant_id;

    return isTenantAdmin && sameTenant;
  }

  /**
   * Permission helper - môže užívateľ vidieť všetky tenenty?
   */
  canViewAllTenants(currentUser) {
    return currentUser?.roles?.includes('CORE_ROLE_ADMIN');
  }

  /**
   * Helper - je užívateľ read-only?
   */
  isReadOnly(user, currentUser) {
    return !this.canEditUser(user, currentUser);
  }

  /**
   * Helper - získať directory source badge text
   */
  getDirectorySourceBadge(user) {
    return user.isFederated || user.directorySource === 'AD' ? 'AD' : 'Local';
  }

  /**
   * Helper - získať display name užívateľa
   */
  getDisplayName(user) {
    if (user.firstName && user.lastName) {
      return `${user.firstName} ${user.lastName}`;
    }
    if (user.displayName) return user.displayName;
    if (user.username) return user.username;
    return 'Unknown User';
  }
}

// Export only default instance for consistency
const usersDirectoryService = new UsersDirectoryService();
export default usersDirectoryService;