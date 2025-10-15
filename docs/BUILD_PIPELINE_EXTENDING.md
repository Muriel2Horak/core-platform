# How to Add New Test Stages to the Build Pipeline

## Overview

The build progress tracker is **fully dynamic** - it automatically adjusts to show any number of test stages you add to the Makefile.

## Current Pipeline Structure

### Standard `make clean` (without E2E):
1. Cleanup
2. Pre-build tests
3. Build images
4. Start services

### Full `make clean` with E2E (`RUN_E2E_FULL=true`):
1. Cleanup
2. Pre-build tests
3. Build images
4. Start services
5. E2E pre-deploy
6. E2E post-deploy

## Adding a New Test Stage

### Example: Adding "Integration Tests" stage

**Step 1: Update `_clean_inner` in Makefile**

```makefile
_clean_inner:
	@# Build dynamic step list
	@STEPS="Cleanup"; \
	STEPS="$$STEPS" "Pre-build tests" "Build images" "Start services"; \
	STEPS="$$STEPS" "Integration tests";  # ← ADD YOUR NEW STAGE HERE
	if [ "$${RUN_E2E_FULL:-false}" = "true" ]; then \
		STEPS="$$STEPS" "E2E pre-deploy" "E2E post-deploy"; \
	elif [ "$${RUN_E2E_PRE:-false}" = "true" ]; then \
		STEPS="$$STEPS" "E2E pre-deploy"; \
	fi; \
	\
	bash scripts/build/build-progress-tracker.sh init "MAKE CLEAN - FULL PIPELINE" $$STEPS;
```

**Step 2: Add execution logic to `_rebuild_with_progress`**

```makefile
_rebuild_with_progress:
	@OFFSET=$${STEP_OFFSET:-0}; \
	CURRENT=$$((1 + OFFSET)); \
	
	# ... existing steps (pre-build, build, start) ...
	
	# Add your new stage HERE (after "Start services")
	CURRENT=$$((CURRENT + 1)); \
	bash scripts/build/build-progress-tracker.sh update $$CURRENT "IN_PROGRESS" ""; \
	INT_START=$$(date +%s); \
	if $(MAKE) test-integration; then \  # ← Your test target
		INT_END=$$(date +%s); \
		INT_TIME=$$((INT_END - INT_START)); \
		bash scripts/build/build-progress-tracker.sh update $$CURRENT "DONE" "$${INT_TIME}s"; \
	else \
		INT_END=$$(date +%s); \
		INT_TIME=$$((INT_END - INT_START)); \
		bash scripts/build/build-progress-tracker.sh update $$CURRENT "FAILED" "$${INT_TIME}s"; \
		exit 1; \
	fi; \
	sleep 0.5;
	
	# ... rest of E2E stages ...
```

**Step 3: Create the test target**

```makefile
.PHONY: test-integration
test-integration:
	@echo "Running integration tests..."
	@cd backend && ./mvnw verify -P integration-tests
```

## Example: Multiple New Stages

Want to add **Unit**, **Integration**, **Performance**, and **Security** tests?

```makefile
_clean_inner:
	@STEPS="Cleanup" "Pre-build tests" "Build images" "Start services"; \
	STEPS="$$STEPS" "Unit tests" "Integration tests" "Performance tests" "Security scan"; \
	if [ "$${RUN_E2E_FULL:-false}" = "true" ]; then \
		STEPS="$$STEPS" "E2E pre-deploy" "E2E post-deploy"; \
	fi; \
	bash scripts/build/build-progress-tracker.sh init "MAKE CLEAN - EXTENDED PIPELINE" $$STEPS;
```

The progress panel will automatically show:

```
╔══════════════════════════════════════════════════════════════════════════╗
║  🏗️  MAKE CLEAN - EXTENDED PIPELINE                                    ║
╠══════════════════════════════════════════════════════════════════════════╣
║  ✅ 1/8   Cleanup                   [████████████] DONE (8s)            ║
║  ✅ 2/8   Pre-build tests           [████████████] DONE (42s)           ║
║  ✅ 3/8   Build images              [████████████] DONE (1m 25s)        ║
║  ✅ 4/8   Start services            [████████████] DONE (15s)           ║
║  ⏳ 5/8   Unit tests                [██████░░░░░░] IN PROGRESS          ║
║  ⏸️ 6/8   Integration tests         [░░░░░░░░░░░░] PENDING             ║
║  ⏸️ 7/8   Performance tests         [░░░░░░░░░░░░] PENDING             ║
║  ⏸️ 8/8   Security scan             [░░░░░░░░░░░░] PENDING             ║
╠══════════════════════════════════════════════════════════════════════════╣
║  Overall: [████████░░░░] 4/8 (50%)  │  Elapsed: 2m 30s                 ║
╚══════════════════════════════════════════════════════════════════════════╝
```

## Conditional Stages

You can make stages conditional with environment variables:

```makefile
_clean_inner:
	@STEPS="Cleanup" "Pre-build tests" "Build images" "Start services"; \
	if [ "$${RUN_PERF:-false}" = "true" ]; then \
		STEPS="$$STEPS" "Performance tests"; \
	fi; \
	if [ "$${RUN_SECURITY:-false}" = "true" ]; then \
		STEPS="$$STEPS" "Security scan"; \
	fi; \
	bash scripts/build/build-progress-tracker.sh init "MAKE CLEAN" $$STEPS;
```

Then run:
```bash
RUN_PERF=true RUN_SECURITY=true make clean
```

## Key Points

✅ **No hardcoded limits** - add as many stages as you need  
✅ **Automatic numbering** - steps are numbered dynamically (1/N, 2/N, ...)  
✅ **Progress calculation** - overall % updates automatically  
✅ **Conditional stages** - use environment variables to enable/disable stages  
✅ **Error handling** - failed stages stop the pipeline and show error details  

## Testing Your Changes

Use the demo script to test new stage layouts:

```bash
bash scripts/build/demo-progress.sh
```

Or modify the demo to test your specific configuration.
