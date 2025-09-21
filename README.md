# 🚀 Core Platform

Modern full-stack application with React frontend, Spring Boot backend, PostgreSQL database, and Keycloak authentication - all running in Docker containers with unified deployment across development, staging, and production environments.

## 🏗️ Architecture

```
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│   Nginx Proxy   │────│  React Frontend  │────│ Spring Backend  │
│  (SSL/HTTPS)    │    │   (Vite/MUI)     │    │   (Java 21)     │
└─────────────────┘    └──────────────────┘    └─────────────────┘
         │                       │                       │
         └───────────────────────┼───────────────────────┘
                                 │
            ┌────────────────────┼────────────────────┐
            │                    │                    │
   ┌────────────────┐   ┌────────────────┐   ┌────────────────┐
   │   Keycloak     │   │  PostgreSQL    │   │   Monitoring   │
   │ (Auth/SSO)     │   │  Database      │   │ (Grafana/Loki) │
   └────────────────┘   └────────────────┘   └────────────────┘
```

## 📋 Prerequisites

### 🖥️ **System Requirements**

- **macOS** (tested on macOS 12+)
- **Docker Desktop** 4.0+ with Docker Compose
- **Git**
- **Admin privileges** (for `/etc/hosts` modification)

### 🔧 **Required Software**

1. **Docker Desktop**
   ```bash
   # Install via Homebrew
   brew install --cask docker
   
   # Or download from: https://www.docker.com/products/docker-desktop/
   ```

2. **OpenSSL** (for SSL certificate generation)
   ```bash
   # Usually pre-installed on macOS, verify with:
   openssl version
   
   # If missing, install via Homebrew:
   brew install openssl
   ```

3. **Git**
   ```bash
   # Usually pre-installed on macOS, verify with:
   git --version
   
   # If missing, install via Homebrew:
   brew install git
   ```

### 🌐 **Network Configuration**

The development environment uses local domains that need to be added to your `/etc/hosts` file:

- `core-platform.local` - Main application
- `api.core-platform.local` - API endpoints  
- `auth.core-platform.local` - Keycloak authentication
- `admin.core-platform.local` - Admin interfaces

**These domains are automatically configured by the setup script.**

## 🚀 Quick Start

### 🔧 **Development Setup**

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd core-platform
   ```

2. **Start development environment**
   ```bash
   cd docker
   ./scripts/docker/dev-start-ssl.sh
   ```

   This script will:
   - 🧹 Clean up any existing containers
   - 🌐 Add local domains to `/etc/hosts` (requires sudo password)
   - 🔐 Generate self-signed SSL certificates
   - 🐳 Start all services with HTTPS support
   - 🔍 Perform health checks

3. **Access the application**
   
   After successful startup, access:
   - 🌐 **Frontend**: https://core-platform.local
   - 🔐 **API**: https://core-platform.local/api
   - 🔑 **Keycloak**: https://core-platform.local/auth
   - 👤 **Keycloak Admin**: https://core-platform.local/admin
   - 📊 **Grafana**: http://localhost:3001
   - 🗄️ **PgAdmin**: http://localhost:5050

### 🔐 **Default Credentials**

| Service | Username | Password |
|---------|----------|----------|
| **Test User** | `test` | `Test.1234` |
| **Keycloak Admin** | `admin` | `admin` |
| **Grafana** | `admin` | `admin` |
| **PgAdmin** | `admin@local.dev` | `admin` |

## 🌍 Environments

The platform supports three environments with unified configuration:

### 🔧 **Development** (`core-platform.local`)
- Self-signed SSL certificates
- Hot reload for frontend/backend
- Debug logging enabled
- All monitoring services
- Local domain names

### 🧪 **Staging** (`staging.yourdomain.com`)
- Let's Encrypt SSL certificates  
- Production builds
- Rate limiting enabled
- Restricted admin access

### 🚀 **Production** (`yourdomain.com`)
- Let's Encrypt SSL certificates
- Full security headers
- Rate limiting and monitoring
- Database backups
- Zero-downtime deployments

## 🛠️ Management Commands

### 🔧 **Development**
```bash
# Start development environment
./scripts/docker/dev-start-ssl.sh

# View service logs
docker-compose --env-file .env.development logs -f [service]

# Stop environment
docker-compose --env-file .env.development down

# Restart specific service
docker-compose --env-file .env.development restart [service]

# Clean rebuild
docker-compose --env-file .env.development up -d --build
```

### 🧹 **Cleanup**
```bash
# Clean up all containers and networks
./scripts/docker/cleanup.sh

# Remove SSL certificates and volumes (⚠️ DESTRUCTIVE)
./scripts/docker/cleanup.sh  # Follow prompts
```

### 🧪 **Staging Deployment**
```bash
# Set required environment variables
export DOMAIN_NAME="staging.yourdomain.com"
export SSL_EMAIL="admin@yourdomain.com"
export KEYCLOAK_ADMIN_PASSWORD="secure-password"
export GRAFANA_PASSWORD="secure-password"

# Deploy to staging
./scripts/docker/staging-deploy.sh
```

### 🚀 **Production Deployment**
```bash
# Set required environment variables (use secrets management!)
export DOMAIN_NAME="yourdomain.com"
export SSL_EMAIL="admin@yourdomain.com"
export KEYCLOAK_ADMIN_PASSWORD="secure-password"
export KEYCLOAK_CLIENT_SECRET="secure-secret"
export APP_SECRET_KEY="secure-app-key"
export JWT_SIGNING_KEY="secure-jwt-key"

# Deploy to production
./scripts/docker/prod-deploy.sh
```

## 🔧 Troubleshooting

### 🌐 **SSL Certificate Issues**

If you see SSL warnings in browser:
1. Click **"Advanced"** 
2. Click **"Proceed to core-platform.local"**
3. Or add certificates to keychain:
   ```bash
   # Add certificate to macOS keychain
   sudo security add-trusted-cert -d -r trustRoot -k /Library/Keychains/System.keychain docker/ssl/cert.pem
   ```

### 🗄️ **Database Connection Issues**
```bash
# Check database status
docker-compose logs db

# Reset database (⚠️ DESTRUCTIVE)
docker-compose down -v
docker volume rm core-platform_core_db_data
```

### 🔐 **Keycloak Issues**
```bash
# Check Keycloak logs
docker-compose logs keycloak

# Reset Keycloak realm
docker-compose restart keycloak
```

### 🌐 **/etc/hosts Issues**

If local domains don't work:
```bash
# Check if domains are in hosts file
cat /etc/hosts | grep core-platform

# Manually add domains (if script failed)
sudo vim /etc/hosts
# Add these lines:
# 127.0.0.1   core-platform.local
# 127.0.0.1   api.core-platform.local  
# 127.0.0.1   auth.core-platform.local
# 127.0.0.1   admin.core-platform.local

# Flush DNS cache on macOS
sudo dscacheutil -flushcache
sudo killall -HUP mDNSResponder
```

### 🐳 **Docker Issues**
```bash
# Check Docker Desktop is running
docker version

# Clean Docker system
docker system prune -a

# Reset Docker Desktop (if needed)
# Docker Desktop → Troubleshoot → Reset to factory defaults
```

## 🔐 Authentication Flow

The platform uses **Keycloak with standard OIDC flow**:

1. User visits `https://core-platform.local`
2. Frontend detects no authentication
3. Redirects to Keycloak login page
4. User enters credentials (`test` / `Test.1234`)
5. Keycloak issues JWT token
6. Frontend stores token in memory (not localStorage)
7. All API calls include `Authorization: Bearer <token>`
8. Backend validates JWT signatures against Keycloak

### 🔑 **Keycloak Configuration**

- **Realm**: `core-platform`
- **Frontend Client**: `web` (public client)
- **Backend Client**: `api` (bearer-only)

### 👥 **Role Model**

The platform uses a hierarchical role-based access control (RBAC) system with composite roles:

#### **Core Roles (Realm-level)**

| Role | Description | Permissions |
|------|-------------|-------------|
| `CORE_ROLE_USER` | Basic user role | Self-service operations (profile, password change) |
| `CORE_ROLE_USER_MANAGER` | User management role | User CRUD, role assignment, password reset |
| `CORE_ROLE_ADMIN` | System administrator | Full system access, all operations |

#### **Role Hierarchy (Composite Structure)**

```
CORE_ROLE_ADMIN (top-level)
├── includes: CORE_ROLE_USER_MANAGER
│   └── includes: CORE_ROLE_USER
└── Result: ADMIN has all USER_MANAGER + USER permissions

CORE_ROLE_USER_MANAGER (mid-level)  
├── includes: CORE_ROLE_USER
└── Result: USER_MANAGER has all USER permissions + management

CORE_ROLE_USER (base-level)
└── Basic authenticated user permissions
```

#### **Endpoint Authorization**

| Endpoint Pattern | Required Role | Description |
|------------------|---------------|-------------|
| `POST /api/frontend-logs` | *authenticated only* | Frontend logging (no specific role needed) |
| `GET/PUT /api/me/**` | `CORE_ROLE_USER` | Self-service operations |
| `GET/POST/PUT/DELETE /api/users/**` | `CORE_ROLE_USER_MANAGER` | User management |
| `GET/POST /api/roles/**` | `CORE_ROLE_USER_MANAGER` | Role management |

#### **Test Users**

| Username | Password | Assigned Role | Effective Permissions |
|----------|----------|---------------|----------------------|
| `test` | `Test.1234` | `CORE_ROLE_USER` | Self-service only |
| `test_admin` | `Test.1234` | `CORE_ROLE_ADMIN` | Full system access (includes USER_MANAGER + USER) |

#### **Frontend Role-Gated UI**

- **User Management Page**: Only visible to users with `CORE_ROLE_USER_MANAGER` or `CORE_ROLE_ADMIN`
- **Admin Menu Items**: Hidden for users without proper roles
- **Action Buttons**: Disabled/hidden based on user permissions
- **Security Logging**: Unauthorized access attempts are logged for monitoring

#### **JWT Token Structure**

Roles are mapped from Keycloak JWT `realm_access.roles` claim to Spring Security authorities:

```json
{
  "realm_access": {
    "roles": ["CORE_ROLE_USER", "offline_access", "uma_authorization"]
  }
}
```

Backend maps these roles 1:1 without prefix (uses `hasAuthority("CORE_ROLE_USER")` instead of `hasRole()`).

## 🎨 Keycloak Themes Setup

### 🎯 **Automatic Theme Configuration**

The Core Platform uses a custom **Material Design theme** (`core-material`) that automatically matches the frontend design. **No manual configuration is required** - the theme is set up automatically when Docker starts.

#### ✅ **What happens automatically:**

1. **Theme Auto-Loading**: Docker Compose mounts the custom theme from `themes/core-material/` 
2. **Realm Configuration**: The `core-platform` realm is pre-configured to use `core-material` theme
3. **Hot Reload**: Theme changes are reflected immediately during development
4. **Consistent Design**: Login, account management, and password reset pages all use Material UI styling

#### 🎨 **Theme Features:**

- **Material Design 3** components (text fields, buttons, cards)
- **Plus Jakarta Sans** font (same as frontend)
- **Consistent color palette** (#5D87FF primary, etc.)
- **Responsive design** for mobile and desktop
- **Localization support** (Czech/English)

#### 📁 **Theme Structure:**
```
themes/core-material/
├── theme.properties          # Main theme configuration
├── login/                    # Login pages
│   ├── template.ftl         # Base layout template
│   ├── login.ftl            # Login form with Material UI
│   ├── login-reset-password.ftl # Password reset
│   └── resources/css/styles.css # Material Design CSS
├── account/                  # Account management
│   ├── theme.properties     # Account theme config
│   └── resources/css/account-styles.css # Account CSS
└── email/                    # Email templates
    └── theme.properties     # Email theme config
```

#### 🔧 **Configuration Files:**

- **Realm Config**: `docker/keycloak/realm-core-platform.json`
  - Pre-configured with `"loginTheme": "core-material"`
  - Pre-configured with `"accountTheme": "core-material"`
  - Versioned in Git for consistency across environments

- **Docker Environment**: 
  - `KC_THEME_DEFAULT=core-material` - Sets global default
  - `KC_SPI_THEME_CACHE_THEMES=false` - Enables hot reload
  - Volume mount: `../themes/core-material:/opt/keycloak/themes/core-material:ro`

#### 🛠️ **Development Workflow:**

1. **Making Theme Changes:**
   ```bash
   # Edit theme files
   vim themes/core-material/login/resources/css/styles.css
   
   # Changes are reflected immediately (hot reload enabled)
   # No restart required
   ```

2. **Testing Theme:**
   - **Login**: https://core-platform.local/realms/core-platform/protocol/openid-connect/auth?client_id=web&response_type=code&redirect_uri=https://core-platform.local
   - **Account**: https://core-platform.local/realms/core-platform/account/
   - **Admin Console**: https://core-platform.local/admin/

3. **Debugging Theme Issues:**
   ```bash
   # Check if theme loaded correctly
   docker-compose logs keycloak | grep -i theme
   
   # Verify CSS loading in browser DevTools
   # Network tab should show 200 OK for /resources/css/styles.css
   ```

#### 🚨 **Important Notes:**

- **No React/Vite**: Keycloak 26.x uses FreeMarker templates (.ftl), not React components
- **CSS Only**: All styling is pure CSS with Material Design classes
- **Automatic Setup**: Theme is configured via Docker environment and realm import
- **Version Control**: All theme configurations are versioned in Git
- **No Manual Steps**: After `docker-compose up`, theme is ready to use

#### 🎯 **Result:**

After starting the Docker stack, all Keycloak pages (login, password reset, account management) will automatically use the same Material UI design as your frontend application - consistent colors, fonts, and component styling across the entire platform.

## 📚 Development

### 🛡️ **Frontend** (React + Vite + MUI)
```bash
cd frontend
npm install
npm run dev  # Development server
npm run build  # Production build
```

### 🏗️ **Backend** (Spring Boot + Java 21)
```bash
cd backend  
./mvnw spring-boot:run  # Development server
./mvnw clean package    # Build JAR
```

### 🗄️ **Database** (PostgreSQL)
- Access via PgAdmin: http://localhost:5050
- Direct connection: `localhost:5432`
- Credentials: `core` / `core` / `core`

## 📊 Monitoring

- **Grafana**: http://localhost:3001 (admin/admin)
- **Prometheus**: http://localhost:9091
- **Loki**: http://localhost:3100
- **Application logs** via Docker Loki driver

## 🔒 Security

### 🛡️ **Development**
- Self-signed SSL certificates
- Permissive CORS policies
- Debug logging enabled
- All ports exposed

### 🚀 **Production**
- Let's Encrypt SSL certificates
- Restrictive CORS policies  
- Rate limiting on all endpoints
- Security headers (CSP, HSTS, etc.)
- Monitoring access restricted to private networks
- Secrets management required

## 🤝 Contributing

1. Create feature branch: `git checkout -b feature/amazing-feature`
2. Make changes and test locally
3. Commit changes: `git commit -m 'Add amazing feature'`  
4. Push to branch: `git push origin feature/amazing-feature`
5. Open Pull Request

## 📝 License

This project is licensed under the MIT License - see the LICENSE file for details.

---

**🎉 Happy coding!** For issues or questions, please open a GitHub issue.
