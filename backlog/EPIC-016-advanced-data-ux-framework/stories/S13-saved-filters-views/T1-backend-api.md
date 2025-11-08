# T1: Backend API pro Saved Views

**Story:** [S13: Saved Filters & Views](README.md)  
**Effort:** 12 hours  
**Priority:** P0  
**Dependencies:** None

---

## 📋 TASK DESCRIPTION

Implementovat backend API pro ukládání/načítání saved views - `SavedView` entity, CRUD endpointy, sharing logic.

---

## 🎯 ACCEPTANCE CRITERIA

1. **SavedView entity** - name, filters (JSON), isShared, owner, version
2. **CRUD endpoints** - GET/POST/PATCH/DELETE `/api/saved-views`
3. **Sharing logic** - owner může sdílet view s jinými usery
4. **Version tracking** - každá změna vytvoří novou verzi

---

## 🏗️ IMPLEMENTATION

```java
// backend/src/main/java/cz/muriel/core/views/SavedView.java
@Entity
@Table(name = "saved_views")
public class SavedView {
  @Id
  private UUID id;
  
  private String name;
  
  @Column(columnDefinition = "jsonb")
  @Type(type = "jsonb")
  private FilterConfig filters;  // {"assignees": ["user1"], "priorities": ["HIGH"]}
  
  private boolean isShared;
  
  @ManyToOne
  private User owner;
  
  @ManyToMany
  private Set<User> sharedWith;
  
  private Integer version;
  
  @CreatedDate
  private Instant createdAt;
  
  @LastModifiedDate
  private Instant updatedAt;
}

// Filter config (JSON serialized)
@Data
public class FilterConfig {
  private List<UUID> assignees;
  private List<String> priorities;
  private List<String> types;
  private List<String> tags;
  private String groupBy;
}
```

### Controller

```java
// backend/src/main/java/cz/muriel/core/views/SavedViewController.java
@RestController
@RequestMapping("/api/saved-views")
public class SavedViewController {
  
  @Autowired
  private SavedViewService service;
  
  // List user's views (own + shared with them)
  @GetMapping
  public List<SavedViewDTO> listViews(@AuthenticationPrincipal User user) {
    return service.findViewsForUser(user);
  }
  
  // Create new view
  @PostMapping
  public SavedViewDTO createView(
    @RequestBody SavedViewCreateDTO dto,
    @AuthenticationPrincipal User user
  ) {
    return service.createView(dto, user);
  }
  
  // Update view (creates new version)
  @PatchMapping("/{id}")
  public SavedViewDTO updateView(
    @PathVariable UUID id,
    @RequestBody SavedViewUpdateDTO dto,
    @AuthenticationPrincipal User user
  ) {
    return service.updateView(id, dto, user);
  }
  
  // Share view with users
  @PostMapping("/{id}/share")
  public void shareView(
    @PathVariable UUID id,
    @RequestBody Set<UUID> userIds,
    @AuthenticationPrincipal User user
  ) {
    service.shareView(id, userIds, user);
  }
  
  // Delete view (only owner)
  @DeleteMapping("/{id}")
  public void deleteView(@PathVariable UUID id, @AuthenticationPrincipal User user) {
    service.deleteView(id, user);
  }
}
```

### Service

```java
// backend/src/main/java/cz/muriel/core/views/SavedViewService.java
@Service
public class SavedViewService {
  
  @Autowired
  private SavedViewRepository repository;
  
  public List<SavedViewDTO> findViewsForUser(User user) {
    // Own views + views shared with them
    return repository.findByOwnerOrSharedWithContaining(user, user)
      .stream()
      .map(this::toDTO)
      .collect(Collectors.toList());
  }
  
  public SavedViewDTO createView(SavedViewCreateDTO dto, User owner) {
    SavedView view = new SavedView();
    view.setName(dto.getName());
    view.setFilters(dto.getFilters());
    view.setOwner(owner);
    view.setVersion(1);
    return toDTO(repository.save(view));
  }
  
  public SavedViewDTO updateView(UUID id, SavedViewUpdateDTO dto, User user) {
    SavedView view = repository.findById(id)
      .orElseThrow(() -> new NotFoundException("View not found"));
    
    if (!view.getOwner().equals(user)) {
      throw new ForbiddenException("Only owner can update view");
    }
    
    // ✅ Version tracking - increment version
    view.setName(dto.getName());
    view.setFilters(dto.getFilters());
    view.setVersion(view.getVersion() + 1);
    
    return toDTO(repository.save(view));
  }
  
  public void shareView(UUID id, Set<UUID> userIds, User owner) {
    SavedView view = repository.findById(id)
      .orElseThrow(() -> new NotFoundException("View not found"));
    
    if (!view.getOwner().equals(owner)) {
      throw new ForbiddenException("Only owner can share view");
    }
    
    Set<User> users = userRepository.findAllById(userIds)
      .stream()
      .collect(Collectors.toSet());
    
    view.setSharedWith(users);
    view.setShared(true);
    repository.save(view);
  }
}
```

---

## ✅ TESTING

```java
@Test
void createView_success() {
  SavedViewCreateDTO dto = new SavedViewCreateDTO();
  dto.setName("High Priority Tasks");
  dto.setFilters(new FilterConfig(null, List.of("HIGH"), null, null, "NONE"));
  
  SavedViewDTO result = controller.createView(dto, owner);
  
  assertThat(result.getName()).isEqualTo("High Priority Tasks");
  assertThat(result.getVersion()).isEqualTo(1);
}

@Test
void updateView_incrementsVersion() {
  SavedView view = createView("View 1", owner);
  
  SavedViewUpdateDTO dto = new SavedViewUpdateDTO();
  dto.setName("Updated View");
  
  SavedViewDTO result = controller.updateView(view.getId(), dto, owner);
  
  assertThat(result.getVersion()).isEqualTo(2);
}

@Test
void shareView_onlyOwnerCanShare() {
  SavedView view = createView("View 1", owner);
  
  assertThrows(ForbiddenException.class, () -> {
    service.shareView(view.getId(), Set.of(otherUser.getId()), otherUser);
  });
}
```

---

## 📦 DELIVERABLES

- [ ] SavedView entity with JSONB filters
- [ ] CRUD endpoints
- [ ] Sharing logic
- [ ] Version tracking
- [ ] Unit tests (70%+ coverage)

---

**Estimated:** 12 hours
