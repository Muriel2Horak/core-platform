# EntityView SDK - React Hooks

React hooks SDK pro práci s entitami v core-platform.

## 📦 Instalace

SDK je součástí projektu, import přes barrel export:

```javascript
import { useEntityView, useEntityMutation, useEntityList } from '@/hooks';
```

## 🎯 Hooks

### useEntityView

Hook pro načítání a správu jedné entity.

**Parametry:**
- `entityType` (string) - Typ entity (např. 'User', 'Order', 'Product')
- `entityId` (string) - ID entity
- `options` (object) - Konfigurace
  - `enabled` (boolean) - Automatické načtení (default: true)
  - `refetchInterval` (number) - Interval auto-refresh v ms
  - `onSuccess` (function) - Callback při úspěchu
  - `onError` (function) - Callback při chybě

**Vrací:**
- `data` - Data entity
- `loading` - Stav načítání
- `error` - Chyba
- `refetch` - Funkce pro manuální refresh
- `isSuccess` - Boolean příznak úspěchu
- `isError` - Boolean příznak chyby

**Příklad:**

```javascript
function UserProfile({ userId }) {
  const { data, loading, error, refetch } = useEntityView('User', userId, {
    refetchInterval: 30000, // Refresh každých 30s
    onSuccess: (user) => console.log('User loaded:', user),
  });

  if (loading) return <Spinner />;
  if (error) return <Error message={error.message} />;

  return (
    <div>
      <h1>{data.name}</h1>
      <button onClick={refetch}>Refresh</button>
    </div>
  );
}
```

---

### useEntityMutation

Hook pro mutace entity (CRUD operace).

**Parametry:**
- `entityType` (string) - Typ entity

**Vrací:**
- `create(data)` - Vytvoření nové entity
- `update(entityId, data)` - Update celé entity
- `patch(entityId, data)` - Částečný update entity
- `remove(entityId)` - Smazání entity
- `loading` - Stav načítání
- `error` - Chyba

**Příklad:**

```javascript
function UserForm() {
  const { create, update, remove, loading } = useEntityMutation('User');
  const [formData, setFormData] = useState({});

  const handleSubmit = async () => {
    try {
      if (formData.id) {
        await update(formData.id, formData);
      } else {
        await create(formData);
      }
      alert('Saved!');
    } catch (err) {
      alert('Error: ' + err.message);
    }
  };

  const handleDelete = async () => {
    try {
      await remove(formData.id);
      alert('Deleted!');
    } catch (err) {
      alert('Error: ' + err.message);
    }
  };

  return (
    <form onSubmit={handleSubmit}>
      <input 
        value={formData.name} 
        onChange={(e) => setFormData({ ...formData, name: e.target.value })}
      />
      <button type="submit" disabled={loading}>Save</button>
      {formData.id && <button onClick={handleDelete}>Delete</button>}
    </form>
  );
}
```

---

### useEntityList

Hook pro načítání seznamu entit s filtrováním a stránkováním.

**Parametry:**
- `entityType` (string) - Typ entity
- `options` (object) - Konfigurace
  - `filters` (object) - Filtrovací kritéria
  - `page` (number) - Číslo stránky (0-indexed)
  - `pageSize` (number) - Počet položek na stránku
  - `sortBy` (string) - Pole pro řazení
  - `sortOrder` (string) - Směr řazení ('asc' nebo 'desc')
  - `enabled` (boolean) - Automatické načtení (default: true)
  - `onSuccess` (function) - Callback při úspěchu
  - `onError` (function) - Callback při chybě

**Vrací:**
- `data` - Pole entit
- `loading` - Stav načítání
- `error` - Chyba
- `pagination` - Info o stránkování (page, pageSize, totalElements, totalPages)
- `refetch` - Funkce pro manuální refresh
- `isSuccess` - Boolean příznak úspěchu
- `isError` - Boolean příznak chyby

**Příklad:**

```javascript
function UserList() {
  const [filters, setFilters] = useState({ status: 'active' });
  const [page, setPage] = useState(0);

  const { data, loading, pagination, refetch } = useEntityList('User', {
    filters,
    page,
    pageSize: 20,
    sortBy: 'createdAt',
    sortOrder: 'desc',
  });

  if (loading) return <Spinner />;

  return (
    <div>
      <input 
        placeholder="Filter by status"
        onChange={(e) => setFilters({ ...filters, status: e.target.value })}
      />
      
      <table>
        {data.map((user) => (
          <tr key={user.id}>
            <td>{user.name}</td>
            <td>{user.email}</td>
          </tr>
        ))}
      </table>

      <Pagination
        page={pagination.page}
        totalPages={pagination.totalPages}
        onPageChange={setPage}
      />
    </div>
  );
}
```

---

### useOptimisticUpdate

Hook pro optimistické updaty s rollback funkcionalitou.

**Parametry:**
- `entityType` (string) - Typ entity
- `entityId` (string) - ID entity

**Vrací:**
- `data` - Aktuální data (optimistická)
- `updateOptimistic(updates)` - Optimistický update
- `commit()` - Potvrzení změn na server
- `rollback()` - Rollback změn
- `isDirty` - Boolean příznak neuložených změn

**Příklad:**

```javascript
function UserEditor({ userId }) {
  const { data, updateOptimistic, commit, rollback, isDirty } = useOptimisticUpdate('User', userId);
  
  const handleChange = (field, value) => {
    updateOptimistic({ [field]: value });
  };

  const handleSave = async () => {
    try {
      await commit();
      alert('Saved!');
    } catch (err) {
      alert('Error - changes rolled back');
    }
  };

  return (
    <div>
      <input 
        value={data?.name || ''} 
        onChange={(e) => handleChange('name', e.target.value)}
      />
      
      {isDirty && (
        <>
          <button onClick={handleSave}>Save</button>
          <button onClick={rollback}>Cancel</button>
        </>
      )}
    </div>
  );
}
```

---

## 🔧 API Client Integrace

SDK používá `apiClient` ze služeb:

```javascript
// frontend/src/services/api.js
import axios from 'axios';

export const apiClient = axios.create({
  baseURL: '/api',
  headers: {
    'Content-Type': 'application/json',
  },
});

// Interceptory pro auth, error handling atd.
apiClient.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});
```

---

## 📋 Backend Endpoints

SDK očekává tyto REST endpointy:

```
GET    /api/entities/{entityType}/{id}           - Načtení entity
POST   /api/entities/{entityType}               - Vytvoření entity
PUT    /api/entities/{entityType}/{id}          - Update entity
PATCH  /api/entities/{entityType}/{id}          - Částečný update
DELETE /api/entities/{entityType}/{id}          - Smazání entity
GET    /api/entities/{entityType}?page=0&size=20 - Seznam entit
```

### Formát odpovědi pro seznam:

```json
{
  "content": [...],
  "number": 0,
  "size": 20,
  "totalElements": 100,
  "totalPages": 5
}
```

---

## 🎨 Pokročilé použití

### Kombinace hooks

```javascript
function UserManagement() {
  const [selectedId, setSelectedId] = useState(null);
  
  // Seznam uživatelů
  const { data: users, refetch: refetchList } = useEntityList('User', {
    filters: { status: 'active' },
    pageSize: 10,
  });

  // Detail vybraného uživatele
  const { data: selectedUser } = useEntityView('User', selectedId, {
    enabled: !!selectedId,
  });

  // Mutace
  const { update, remove } = useEntityMutation('User');

  const handleUpdate = async (updates) => {
    await update(selectedId, updates);
    refetchList(); // Refresh seznamu po update
  };

  const handleDelete = async () => {
    await remove(selectedId);
    setSelectedId(null);
    refetchList();
  };

  return (
    <div>
      <UserList users={users} onSelect={setSelectedId} />
      {selectedUser && (
        <UserDetail 
          user={selectedUser} 
          onUpdate={handleUpdate}
          onDelete={handleDelete}
        />
      )}
    </div>
  );
}
```

### Custom hook wrapper

```javascript
// hooks/useUser.js
export function useUser(userId) {
  return useEntityView('User', userId, {
    refetchInterval: 60000, // Refresh každou minutu
    onError: (err) => {
      if (err.response?.status === 404) {
        window.location.href = '/users';
      }
    },
  });
}

// Použití
function UserProfile({ userId }) {
  const { data: user, loading } = useUser(userId);
  // ...
}
```

---

## ⚡ Performance Tips

1. **Conditional fetching**: Použij `enabled: false` pro lazy loading
2. **Debounce filters**: Pro vyhledávání použij debounce
3. **Cache**: API client může mít cache layer (React Query, SWR)
4. **Optimistic updates**: Pro rychlé UI response
5. **Pagination**: Vždy použij stránkování pro velké seznamy

---

## 🧪 Testing

```javascript
import { renderHook, waitFor } from '@testing-library/react';
import { useEntityView } from './useEntityView';

test('should fetch entity', async () => {
  const { result } = renderHook(() => useEntityView('User', '123'));

  expect(result.current.loading).toBe(true);

  await waitFor(() => {
    expect(result.current.loading).toBe(false);
    expect(result.current.data).toBeDefined();
  });
});
```

---

## 📝 License

MIT © core-platform
