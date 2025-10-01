// User Management API Service
import authService from './auth';
import logger from './logger';

class UserManagementService {
  constructor() {
    this.baseUrl = '';
  }

  // ================== Profile Management ==================
  
  async getMyProfile() {
    try {
      logger.apiCall('GET', '/me', 'start', 0, { operation: 'get_my_profile' });
      const startTime = Date.now();
      
      const response = await authService.apiCall('/me');
      if (!response) throw new Error('Failed to get profile');
      
      const data = await response.json();
      const duration = Date.now() - startTime;
      
      logger.apiCall('GET', '/me', response.status, duration, { 
        operation: 'get_my_profile',
        success: true
      });
      
      return data;
    } catch (error) {
      logger.error('GET_MY_PROFILE_ERROR', error.message, { error: error.stack });
      throw error;
    }
  }

  async updateMyProfile(profileData) {
    try {
      logger.apiCall('PUT', '/me', 'start', 0, { operation: 'update_my_profile' });
      const startTime = Date.now();
      
      const response = await authService.apiCall('/me', {
        method: 'PUT',
        body: JSON.stringify(profileData)
      });
      
      if (!response) throw new Error('Failed to update profile');
      
      const data = await response.json();
      const duration = Date.now() - startTime;
      
      logger.apiCall('PUT', '/me', response.status, duration, { 
        operation: 'update_my_profile',
        success: true
      });
      
      logger.userAction('PROFILE_UPDATED', { fields: Object.keys(profileData) });
      
      return data;
    } catch (error) {
      logger.error('UPDATE_MY_PROFILE_ERROR', error.message, { error: error.stack });
      throw error;
    }
  }

  async changeMyPassword(passwordData) {
    try {
      logger.apiCall('PUT', '/me/password', 'start', 0, { operation: 'change_my_password' });
      const startTime = Date.now();
      
      const response = await authService.apiCall('/me/password', {
        method: 'PUT',
        body: JSON.stringify(passwordData)
      });
      
      if (!response) throw new Error('Failed to change password');
      
      const duration = Date.now() - startTime;
      
      logger.apiCall('PUT', '/me/password', response.status, duration, { 
        operation: 'change_my_password',
        success: true
      });
      
      logger.userAction('PASSWORD_CHANGED', { method: 'self_service' });
      
      return true;
    } catch (error) {
      logger.error('CHANGE_MY_PASSWORD_ERROR', error.message, { error: error.stack });
      throw error;
    }
  }

  // ================== User Administration ==================

  async searchUsers(filters = {}) {
    try {
      const params = new URLSearchParams();
      Object.entries(filters).forEach(([key, value]) => {
        if (value !== null && value !== undefined && value !== '') {
          params.append(key, value);
        }
      });

      const url = `/users${params.toString() ? '?' + params.toString() : ''}`;
      logger.apiCall('GET', url, 'start', 0, { operation: 'search_users', filters });
      const startTime = Date.now();
      
      const response = await authService.apiCall(url);
      if (!response) throw new Error('Failed to search users');
      
      const data = await response.json();
      const duration = Date.now() - startTime;
      
      logger.apiCall('GET', url, response.status, duration, { 
        operation: 'search_users',
        success: true,
        resultCount: data.length
      });
      
      return data;
    } catch (error) {
      logger.error('SEARCH_USERS_ERROR', error.message, { error: error.stack });
      throw error;
    }
  }

  async getUserById(userId) {
    try {
      logger.apiCall('GET', `/users/${userId}`, 'start', 0, { operation: 'get_user_by_id' });
      const startTime = Date.now();
      
      const response = await authService.apiCall(`/users/${userId}`);
      if (!response) throw new Error('Failed to get user');
      
      const data = await response.json();
      const duration = Date.now() - startTime;
      
      logger.apiCall('GET', `/users/${userId}`, response.status, duration, { 
        operation: 'get_user_by_id',
        success: true
      });
      
      return data;
    } catch (error) {
      logger.error('GET_USER_BY_ID_ERROR', error.message, { error: error.stack, userId });
      throw error;
    }
  }

  async createUser(userData) {
    try {
      logger.apiCall('POST', '/users', 'start', 0, { operation: 'create_user' });
      const startTime = Date.now();
      
      const response = await authService.apiCall('/users', {
        method: 'POST',
        body: JSON.stringify(userData)
      });
      
      if (!response) throw new Error('Failed to create user');
      
      const data = await response.json();
      const duration = Date.now() - startTime;
      
      logger.apiCall('POST', '/users', response.status, duration, { 
        operation: 'create_user',
        success: true
      });
      
      logger.userAction('USER_CREATED', { 
        username: userData.username,
        email: userData.email 
      });
      
      return data;
    } catch (error) {
      logger.error('CREATE_USER_ERROR', error.message, { error: error.stack });
      throw error;
    }
  }

  async updateUser(userId, userData) {
    try {
      logger.apiCall('PUT', `/users/${userId}`, 'start', 0, { operation: 'update_user' });
      const startTime = Date.now();
      
      const response = await authService.apiCall(`/users/${userId}`, {
        method: 'PUT',
        body: JSON.stringify(userData)
      });
      
      if (!response) throw new Error('Failed to update user');
      
      const data = await response.json();
      const duration = Date.now() - startTime;
      
      logger.apiCall('PUT', `/users/${userId}`, response.status, duration, { 
        operation: 'update_user',
        success: true
      });
      
      logger.userAction('USER_UPDATED', { 
        userId,
        fields: Object.keys(userData)
      });
      
      return data;
    } catch (error) {
      logger.error('UPDATE_USER_ERROR', error.message, { error: error.stack, userId });
      throw error;
    }
  }

  async resetUserPassword(userId, passwordData) {
    try {
      logger.apiCall('PUT', `/users/${userId}/password`, 'start', 0, { operation: 'reset_user_password' });
      const startTime = Date.now();
      
      const response = await authService.apiCall(`/users/${userId}/password`, {
        method: 'PUT',
        body: JSON.stringify(passwordData)
      });
      
      if (!response) throw new Error('Failed to reset password');
      
      const duration = Date.now() - startTime;
      
      logger.apiCall('PUT', `/users/${userId}/password`, response.status, duration, { 
        operation: 'reset_user_password',
        success: true
      });
      
      logger.userAction('USER_PASSWORD_RESET', { userId, method: 'admin_reset' });
      
      return true;
    } catch (error) {
      logger.error('RESET_USER_PASSWORD_ERROR', error.message, { error: error.stack, userId });
      throw error;
    }
  }

  async deleteUser(userId) {
    try {
      logger.apiCall('DELETE', `/users/${userId}`, 'start', 0, { operation: 'delete_user' });
      const startTime = Date.now();
      
      const response = await authService.apiCall(`/users/${userId}`, {
        method: 'DELETE'
      });
      
      if (!response) throw new Error('Failed to delete user');
      
      const duration = Date.now() - startTime;
      
      logger.apiCall('DELETE', `/users/${userId}`, response.status, duration, { 
        operation: 'delete_user',
        success: true
      });
      
      logger.userAction('USER_DELETED', { userId });
      
      return true;
    } catch (error) {
      logger.error('DELETE_USER_ERROR', error.message, { error: error.stack, userId });
      throw error;
    }
  }

  // ================== Role Management ==================

  async getAllRoles() {
    try {
      logger.apiCall('GET', '/roles', 'start', 0, { operation: 'get_all_roles' });
      const startTime = Date.now();
      
      const response = await authService.apiCall('/roles');
      if (!response) throw new Error('Failed to get roles');
      
      const data = await response.json();
      const duration = Date.now() - startTime;
      
      logger.apiCall('GET', '/roles', response.status, duration, { 
        operation: 'get_all_roles',
        success: true,
        resultCount: data.length
      });
      
      return data;
    } catch (error) {
      logger.error('GET_ALL_ROLES_ERROR', error.message, { error: error.stack });
      throw error;
    }
  }

  async createRole(roleData) {
    try {
      logger.apiCall('POST', '/roles', 'start', 0, { operation: 'create_role' });
      const startTime = Date.now();
      
      const response = await authService.apiCall('/roles', {
        method: 'POST',
        body: JSON.stringify(roleData)
      });
      
      if (!response) throw new Error('Failed to create role');
      
      const data = await response.json();
      const duration = Date.now() - startTime;
      
      logger.apiCall('POST', '/roles', response.status, duration, { 
        operation: 'create_role',
        success: true
      });
      
      logger.userAction('ROLE_CREATED', { roleName: roleData.name });
      
      return data;
    } catch (error) {
      logger.error('CREATE_ROLE_ERROR', error.message, { error: error.stack });
      throw error;
    }
  }

  async getUserRoles(userId) {
    try {
      logger.apiCall('GET', `/users/${userId}/roles`, 'start', 0, { operation: 'get_user_roles' });
      const startTime = Date.now();
      
      const response = await authService.apiCall(`/users/${userId}/roles`);
      if (!response) throw new Error('Failed to get user roles');
      
      const data = await response.json();
      const duration = Date.now() - startTime;
      
      logger.apiCall('GET', `/users/${userId}/roles`, response.status, duration, { 
        operation: 'get_user_roles',
        success: true
      });
      
      return data;
    } catch (error) {
      logger.error('GET_USER_ROLES_ERROR', error.message, { error: error.stack, userId });
      throw error;
    }
  }

  async assignRoleToUser(userId, roleName) {
    try {
      logger.apiCall('POST', `/users/${userId}/roles`, 'start', 0, { operation: 'assign_role_to_user' });
      const startTime = Date.now();
      
      const response = await authService.apiCall(`/users/${userId}/roles`, {
        method: 'POST',
        body: JSON.stringify({ roleName })
      });
      
      if (!response) throw new Error('Failed to assign role');
      
      const duration = Date.now() - startTime;
      
      logger.apiCall('POST', `/users/${userId}/roles`, response.status, duration, { 
        operation: 'assign_role_to_user',
        success: true
      });
      
      logger.userAction('ROLE_ASSIGNED', { userId, roleName });
      
      return true;
    } catch (error) {
      logger.error('ASSIGN_ROLE_ERROR', error.message, { error: error.stack, userId, roleName });
      throw error;
    }
  }

  async removeRoleFromUser(userId, roleName) {
    try {
      logger.apiCall('DELETE', `/users/${userId}/roles/${roleName}`, 'start', 0, { operation: 'remove_role_from_user' });
      const startTime = Date.now();
      
      const response = await authService.apiCall(`/users/${userId}/roles/${roleName}`, {
        method: 'DELETE'
      });
      
      if (!response) throw new Error('Failed to remove role');
      
      const duration = Date.now() - startTime;
      
      logger.apiCall('DELETE', `/users/${userId}/roles/${roleName}`, response.status, duration, { 
        operation: 'remove_role_from_user',
        success: true
      });
      
      logger.userAction('ROLE_REMOVED', { userId, roleName });
      
      return true;
    } catch (error) {
      logger.error('REMOVE_ROLE_ERROR', error.message, { error: error.stack, userId, roleName });
      throw error;
    }
  }
}

// Export only default instance for consistency
const userManagementService = new UserManagementService();
export default userManagementService;