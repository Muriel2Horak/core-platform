// File Upload API Service
import authService from './auth';
import logger from './logger';

class FileUploadService {
  constructor() {
    this.baseUrl = '';
  }

  /**
   * Upload profilové fotky pro aktuálního uživatele
   */
  async uploadProfilePicture(file) {
    try {
      logger.apiCall('POST', '/files/profile-picture', 'start', 0, { operation: 'upload_profile_picture' });
      const startTime = Date.now();

      // Validace souboru na frontend straně
      this.validateImageFile(file);

      // Vytvoř FormData pro multipart upload
      const formData = new FormData();
      formData.append('file', file);

      const response = await authService.apiCall('/files/profile-picture', {
        method: 'POST',
        body: formData,
        // Neposíláme Content-Type header - nech browser nastavit boundary pro multipart
        headers: {} 
      });

      if (!response) throw new Error('Failed to upload profile picture');

      const data = await response.json();
      const duration = Date.now() - startTime;

      logger.apiCall('POST', '/files/profile-picture', response.status, duration, { 
        operation: 'upload_profile_picture',
        success: true,
        fileSize: file.size,
        fileName: file.name
      });

      logger.userAction('PROFILE_PICTURE_UPLOADED', { 
        fileName: file.name,
        fileSize: file.size 
      });

      return data;

    } catch (error) {
      logger.error('UPLOAD_PROFILE_PICTURE_ERROR', error.message, { 
        error: error.stack,
        fileName: file?.name,
        fileSize: file?.size 
      });
      throw error;
    }
  }

  /**
   * Smazání profilové fotky pro aktuálního uživatele
   */
  async deleteProfilePicture() {
    try {
      logger.apiCall('DELETE', '/files/profile-picture', 'start', 0, { operation: 'delete_profile_picture' });
      const startTime = Date.now();

      const response = await authService.apiCall('/files/profile-picture', {
        method: 'DELETE'
      });

      if (!response) throw new Error('Failed to delete profile picture');

      const data = await response.json();
      const duration = Date.now() - startTime;

      logger.apiCall('DELETE', '/files/profile-picture', response.status, duration, { 
        operation: 'delete_profile_picture',
        success: true
      });

      logger.userAction('PROFILE_PICTURE_DELETED', {});

      return data;

    } catch (error) {
      logger.error('DELETE_PROFILE_PICTURE_ERROR', error.message, { error: error.stack });
      throw error;
    }
  }

  /**
   * Upload profilové fotky pro konkrétního uživatele (admin only)
   */
  async uploadUserProfilePicture(userId, file) {
    try {
      logger.apiCall('POST', `/files/profile-picture/${userId}`, 'start', 0, { operation: 'upload_user_profile_picture' });
      const startTime = Date.now();

      // Validace souboru na frontend straně
      this.validateImageFile(file);

      // Vytvoř FormData pro multipart upload
      const formData = new FormData();
      formData.append('file', file);

      const response = await authService.apiCall(`/files/profile-picture/${userId}`, {
        method: 'POST',
        body: formData,
        headers: {} 
      });

      if (!response) throw new Error('Failed to upload user profile picture');

      const data = await response.json();
      const duration = Date.now() - startTime;

      logger.apiCall('POST', `/files/profile-picture/${userId}`, response.status, duration, { 
        operation: 'upload_user_profile_picture',
        success: true,
        fileSize: file.size,
        fileName: file.name,
        userId
      });

      logger.userAction('USER_PROFILE_PICTURE_UPLOADED', { 
        userId,
        fileName: file.name,
        fileSize: file.size 
      });

      return data;

    } catch (error) {
      logger.error('UPLOAD_USER_PROFILE_PICTURE_ERROR', error.message, { 
        error: error.stack,
        userId,
        fileName: file?.name,
        fileSize: file?.size 
      });
      throw error;
    }
  }

  /**
   * Smazání profilové fotky pro konkrétního uživatele (admin only)
   */
  async deleteUserProfilePicture(userId) {
    try {
      logger.apiCall('DELETE', `/files/profile-picture/${userId}`, 'start', 0, { operation: 'delete_user_profile_picture' });
      const startTime = Date.now();

      const response = await authService.apiCall(`/files/profile-picture/${userId}`, {
        method: 'DELETE'
      });

      if (!response) throw new Error('Failed to delete user profile picture');

      const data = await response.json();
      const duration = Date.now() - startTime;

      logger.apiCall('DELETE', `/files/profile-picture/${userId}`, response.status, duration, { 
        operation: 'delete_user_profile_picture',
        success: true,
        userId
      });

      logger.userAction('USER_PROFILE_PICTURE_DELETED', { userId });

      return data;

    } catch (error) {
      logger.error('DELETE_USER_PROFILE_PICTURE_ERROR', error.message, { error: error.stack, userId });
      throw error;
    }
  }

  /**
   * Validace image souboru na frontend straně
   */
  validateImageFile(file) {
    if (!file) {
      throw new Error('Žádný soubor nebyl vybrán');
    }

    // Kontrola velikosti (5MB limit)
    const MAX_SIZE = 5 * 1024 * 1024; // 5MB
    if (file.size > MAX_SIZE) {
      throw new Error(`Soubor je příliš velký. Maximální velikost je ${MAX_SIZE / 1024 / 1024}MB`);
    }

    // Kontrola typu souboru
    const allowedTypes = ['image/jpeg', 'image/jpg', 'image/png', 'image/gif', 'image/webp'];
    if (!allowedTypes.includes(file.type)) {
      throw new Error(`Nepodporovaný typ souboru. Povolené typy: ${allowedTypes.join(', ')}`);
    }

    // Dodatečná kontrola přípony
    const allowedExtensions = ['.jpg', '.jpeg', '.png', '.gif', '.webp'];
    const fileName = file.name.toLowerCase();
    const hasValidExtension = allowedExtensions.some(ext => fileName.endsWith(ext));
    
    if (!hasValidExtension) {
      throw new Error(`Nepodporovaná přípona souboru. Povolené přípony: ${allowedExtensions.join(', ')}`);
    }
  }

  /**
   * Získání preview URL pro soubor (Object URL)
   */
  createFilePreview(file) {
    try {
      this.validateImageFile(file);
      return URL.createObjectURL(file);
    } catch (error) {
      // Přidáme specifické logování pro preview chyby
      logger.error('CREATE_FILE_PREVIEW_ERROR', error.message, { 
        error: error.stack,
        fileName: file?.name,
        fileSize: file?.size,
        fileType: file?.type
      });
      
      // Vracíme null místo vyhození chyby pro lepší UX
      return null;
    }
  }

  /**
   * Vyčištění preview URL
   */
  revokeFilePreview(url) {
    if (url && url.startsWith('blob:')) {
      URL.revokeObjectURL(url);
    }
  }

  /**
   * Rozpoznání typu a velikosti souboru pro UI
   */
  getFileInfo(file) {
    if (!file) return null;

    return {
      name: file.name,
      size: file.size,
      type: file.type,
      sizeFormatted: this.formatFileSize(file.size),
      isImage: file.type.startsWith('image/'),
      lastModified: file.lastModified ? new Date(file.lastModified) : null
    };
  }

  /**
   * Formátování velikosti souboru
   */
  formatFileSize(bytes) {
    if (bytes === 0) return '0 B';
    
    const k = 1024;
    const sizes = ['B', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
  }
}

// Export singleton instance
export const fileUploadService = new FileUploadService();
export default fileUploadService;