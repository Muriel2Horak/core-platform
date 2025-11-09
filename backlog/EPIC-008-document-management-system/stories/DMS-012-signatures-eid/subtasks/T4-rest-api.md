---
id: DMS-012-T4
story: DMS-012
title: "Public Signing Page"
status: todo
assignee: ""
estimate: "2 hours"
created: 2025-11-09
updated: 2025-11-09
---

# DMS-012-T4: Public Signing Page

> **Parent Story:** [DMS-012](../README.md)  
> **Status:** todo | **Estimate:** 2 hours

## 🎯 Subtask Goal

Public Signing Page

## ✅ Acceptance Criteria

- [ ] Implementation matches parent story specification
- [ ] All files created/modified as specified
- [ ] Code follows project coding standards
- [ ] Unit tests written and passing (coverage >80%)
- [ ] Integration tests (if applicable)
- [ ] Code review approved
- [ ] Documentation updated

## 📂 Files to Modify/Create

**See parent story [`../README.md`](../README.md) for exact file paths.**

## 🔧 Implementation Details

### Code Example 1 (tsx)

```tsx
export function PublicSigningPage() {
    const { token } = useParams();
    const [request, setRequest] = useState<SigningRequest>();
    
    useEffect(() => {
        api.get(`/api/dms/signing/${token}`).then(res => setRequest(res.data));
    }, [token]);
    
    const handleSign = async () => {
        // Redirect to BankID
        const bankIdUrl = await api.post(`/api/dms/signing/${token}/initiate`);
        window.location.href = bankIdUrl.data.redirectUrl;
    };
    
    return (
        <Container maxWidth="md">
            <Typography variant="h4">Sign Document</Typography>
            <Typography>Document: {request?.documentName}</Typography>
            <Typography>Signer: {request?.signerEmail}</Typography>
            <Button onClick={handleSign}>Sign with BankID</Button>
        </Container>
    );
}
```

**For complete implementation details, see parent story [`../README.md`](../README.md).**

## 🧪 Testing

- [ ] Unit tests for new code
- [ ] Integration tests for API endpoints (if applicable)
- [ ] E2E tests for user workflows (if applicable)
- [ ] Test coverage >80%

**Test scenarios:** See parent story Testing section.

## 📝 Notes

This subtask is part of DMS-012. Complete specification including database migrations, API contracts, and UI mockups is documented in the parent story README.md.

## ✅ Definition of Done

- [ ] Code implemented per parent story
- [ ] All tests passing (unit + integration)
- [ ] Code review completed and approved
- [ ] Documentation updated (if needed)
- [ ] Committed with message: `feat(DMS-012-T4): Public Signing Page`
