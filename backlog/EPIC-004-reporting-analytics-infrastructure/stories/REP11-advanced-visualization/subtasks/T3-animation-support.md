# T3: Animation Support

**Story:** [S11: Advanced Visualization](README.md)  
**Effort:** 5 hours  
**Priority:** P2  
**Dependencies:** T1

---

## 📋 OBJECTIVE

Pridat animace pro casove grafy s playback ovladanim.

---

## 🏗️ IMPLEMENTATION

```typescript
<AnimatedChart data={series} controls={{ play: true, scrubber: true }} />
```

---

## ✅ Acceptance Criteria

- [ ] Animace ma play/pause + scrubber.
- [ ] Animace respektuje `prefers-reduced-motion`.
- [ ] Data jsou prednactena tak, aby animace byla plynula.

---

## ✅ DELIVERABLES

- [ ] Animation engine + controls UI
- [ ] Config pro animation speed a duration
- [ ] Integrace do dashboard builderu

---

**Estimated:** 5 hours
