# TASK-020-003-02: Trivy scanning pro image

## 🎯 Goal
Skenovat Docker image pred release.

## 📋 Tasks
- [ ] Zapojit Trivy scan do build pipeline.
- [ ] Nastavit severity thresholdy.
- [ ] Exportovat SARIF/JSON report.

## 📤 Output
- Trivy reporty pro image.
- CI job pro container scan.

## ✅ Acceptance Criteria for This Subtask
- [ ] Image scan bezi pred release.
- [ ] Critical/High CVE failuje pipeline.
- [ ] Report je ulozen jako artefakt.
