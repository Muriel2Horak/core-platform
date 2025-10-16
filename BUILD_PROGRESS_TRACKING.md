# 📊 Build Progress Tracking

Real-time build progress visualization with live test counting and optional split-pane monitoring.

## Features

✅ **Dynamic pipeline tracking** - Shows all 6 build stages  
✅ **Live test counting** - `145/215 tests (67%)` with real-time updates  
✅ **Progress bars** - Visual progress with percentage  
✅ **Test output** - Shows running test names  
✅ **Split-pane mode** - Fixed dashboard on top, scrolling output below (requires tmux)

## Usage

### Basic Build (with auto-split if tmux available)

```bash
make clean          # Full pipeline with E2E tests
make clean-fast     # Skip E2E tests (faster for dev)
```

**With tmux installed:**
- Terminal automatically splits into 2 panes
- **Top pane**: Live progress dashboard (updates every second)
- **Bottom pane**: Scrolling build output

**Without tmux:**
- Progress panel updates every 10 tests
- Panel scrolls with output (normal mode)

### Manual Monitoring (2 terminals)

If you prefer manual control:

**Terminal 1** - Run build:
```bash
PROGRESS_SPLIT=no make clean
```

**Terminal 2** - Watch progress:
```bash
./scripts/build/watch-progress.sh
```

This gives you a **fixed dashboard** in terminal 2 while terminal 1 scrolls output.

## Split-Pane Controls

### Install tmux (macOS)
```bash
brew install tmux
```

### Disable split-pane mode
```bash
PROGRESS_SPLIT=no make clean
```

### tmux Keyboard Shortcuts

When in split-pane mode:

- `Ctrl+B` then `↑/↓` - Navigate between panes
- `Ctrl+B` then `X` - Close current pane
- `Ctrl+B` then `D` - Detach from session (keeps running)
- `Ctrl+B` then `[` - Enter scroll mode (use arrow keys, `q` to exit)

## Progress Display

### 6-Stage Pipeline

```
╔══════════════════════════════════════════════════════════════════════════╗
║  🏗️  MAKE CLEAN - FULL PIPELINE                                        ║
╠══════════════════════════════════════════════════════════════════════════╣
║  ✅ 1/6  Cleanup                   [████████████] DONE (1s)             ║
║  ⏳ 2/6  Pre-build tests           [█████████░░░] 20/25 tests (80%)    ║
║  ⏸️ 3/6  Build images              [░░░░░░░░░░░░] PENDING              ║
║  ⏸️ 4/6  Start services            [░░░░░░░░░░░░] PENDING              ║
║  ⏸️ 5/6  E2E pre-deploy            [░░░░░░░░░░░░] PENDING              ║
║  ⏸️ 6/6  E2E post-deploy           [░░░░░░░░░░░░] PENDING              ║
╠══════════════════════════════════════════════════════════════════════════╣
║  Overall: [██░░░░░░░░░░] 2/6 (33%)  │  Elapsed: 3m 45s                 ║
╚══════════════════════════════════════════════════════════════════════════╝
```

### Test Output Format

```
→ Test 20/25: cz.muriel.core.monitoring.bff.service.TenantOrgServiceImplTest
[INFO] Tests run: 6, Failures: 0, Errors: 0, Skipped: 0
```

## Status Icons

- ⏸️ `PENDING` - Not started yet
- ⏳ `IN_PROGRESS` - Currently running
- ✅ `DONE` - Completed successfully
- ❌ `FAILED` - Failed with errors
- ⏭️ `SKIPPED` - Skipped

## Technical Details

### Progress Update Frequency

- Panel updates every **10 tests** (reduces visual flicker)
- Test counter updates in real-time: `1/25 → 10/25 → 20/25`
- Progress bar fills live based on test completion

### Components

- **build-progress-tracker.sh** - Core progress visualization
- **test-progress-parser.sh** - Parses Maven/Vitest output
- **auto-split.sh** - Automatic tmux split management
- **watch-progress.sh** - Manual monitoring helper

### State Files

- `/tmp/make-progress-shared` - Shared state between processes
- `/tmp/make-progress-panel-initialized` - First-run flag
- `backend/diagnostics/tests/.test-count-cache` - Test count cache

## Troubleshooting

### "tmux not found"

Install tmux or disable split mode:
```bash
brew install tmux
# OR
PROGRESS_SPLIT=no make clean
```

### Progress panel scrolls instead of staying fixed

This happens when:
1. tmux is not installed (fallback mode)
2. Split mode is disabled (`PROGRESS_SPLIT=no`)
3. Output is redirected to a file

**Solution:** Install tmux or use 2-terminal mode.

### Test count shows wrong percentage

The test count cache may be outdated. It rebuilds automatically on first run.

### Panel shows "Waiting for build to start..."

The build hasn't initialized yet. Wait a few seconds for state file creation.

## Examples

### Full build with split pane
```bash
make clean
```

### Quick rebuild without E2E
```bash
make clean-fast
```

### Disable auto-split
```bash
PROGRESS_SPLIT=no make clean
```

### Skip specific tests
```bash
SKIP_TEST_CLASSES="SlowTest,FlakyTest" make clean
```

### Two-terminal monitoring
```bash
# Terminal 1
PROGRESS_SPLIT=no make clean

# Terminal 2  
./scripts/build/watch-progress.sh
```

## See Also

- [Testing Guide](../TESTING_STRUCTURE.md)
- [Build Doctor](./BUILD_DOCTOR_IMPLEMENTATION.md)
- [Makefile Documentation](../README.md#make-targets)
