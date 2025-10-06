# Dev Container Setup

Production-like development environment using Docker Compose Watch.

## 🎯 Philosophy

**NO Vite dev server** - we use production builds with automatic rebuild on file changes. This ensures:

- ✅ Development matches production exactly
- ✅ No surprises when deploying
- ✅ Same build process everywhere
- ✅ Real nginx routing and HTTPS

## 🚀 Quick Start

```bash
# Start Dev Container with services
make dev:up

# Verify everything works
make dev:check

# Start watching for changes (optional, runs in foreground)
make dev:watch
```

## 📂 Architecture

```
.devcontainer/
├── docker-compose.devcontainer.yml  # Overlay config with watch mode
└── README.md                         # This file

docker/
└── docker-compose.yml                # Base production config
```

## 🔄 How Watch Mode Works

### Frontend
- **Trigger**: Changes to `frontend/src/**/*`
- **Action**: Rebuild → `npm run build` → Deploy to nginx
- **Speed**: ~30-60 seconds per rebuild
- **Browser**: Manual refresh needed (F5)

### Backend
- **Trigger**: Changes to `backend/src/**/*`
- **Action**: Rebuild → `mvn clean package` → Restart container
- **Speed**: ~1-2 minutes per rebuild
- **Debug**: Attach to `localhost:5005`

## 💡 Usage Tips

### Manual Rebuild (faster than watch)
```bash
# Rebuild frontend only
make dev:rebuild-fe

# Rebuild backend only
make dev:rebuild-be
```

### Debugging
```bash
# Backend Java debug port: localhost:5005
# Use VS Code launch configuration (F5)

# View logs
make dev:logs

# Check health
make dev:check
```

### Common Workflows

**Frontend Development:**
```bash
1. Edit frontend/src/components/MyComponent.tsx
2. Wait ~30-60s for rebuild (or run: make dev:rebuild-fe)
3. Refresh browser (F5)
4. Repeat
```

**Backend Development:**
```bash
1. Edit backend/src/main/java/.../MyController.java
2. Wait ~1-2min for rebuild (or run: make dev:rebuild-be)
3. Test API changes
4. Repeat
```

## 🐛 Troubleshooting

### Watch not triggering rebuilds?
```bash
# Verify watch is running
docker compose -f docker/docker-compose.yml -f .devcontainer/docker-compose.devcontainer.yml ps

# Start watch manually
make dev:watch
```

### Builds failing?
```bash
# Check logs
make dev:logs

# Restart services
make dev:restart

# Nuclear option - full rebuild
make dev:down
docker system prune -af
make dev:up
```

### Frontend changes not appearing?
```bash
# Clear browser cache
make clear-browser-cache

# Or manually: DevTools (F12) → Application → Clear storage
```

## 📊 Available Commands

| Command | Description |
|---------|-------------|
| `make dev:up` | Start Dev Container services |
| `make dev:down` | Stop Dev Container services |
| `make dev:watch` | Start watch mode (foreground) |
| `make dev:check` | Run environment sanity checks |
| `make dev:logs` | View all logs |
| `make dev:rebuild-fe` | Rebuild frontend only |
| `make dev:rebuild-be` | Rebuild backend only |
| `make dev:restart` | Restart all services |

## 🔧 VS Code Integration

The project includes pre-configured VS Code tasks:

- **F1 → Tasks: Run Task → "Dev: Start with Watch"**
- **F1 → Tasks: Run Task → "Dev: Check Environment"**
- **F1 → Tasks: Run Task → "Frontend: Rebuild in Container"**
- **F5 → Attach Java Debugger** (when backend is running)

## �� Learning More

- [Docker Compose Watch docs](https://docs.docker.com/compose/file-watch/)
- [Main README](../README.md) - Production setup
- [Multitenancy docs](../docs/MULTITENANCY_ARCHITECTURE.md)

## ❓ FAQ

**Q: Why no Vite dev server?**  
A: Production builds ensure we catch issues early. Vite dev server can hide problems that only appear in production.

**Q: Can I use hot module replacement?**  
A: Not with this setup. We prioritize production parity over dev speed. Rebuilds take ~30-60s.

**Q: How do I speed up rebuilds?**  
A: Use `make dev:rebuild-fe` for frontend-only changes. Maven cache makes backend rebuilds faster.

**Q: Can I attach a debugger?**  
A: Yes! Backend exposes Java debug port at `localhost:5005`. Use VS Code launch config (F5).
