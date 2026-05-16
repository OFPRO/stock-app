# S3: Frontend React — correction URL PDF

## Objectif
Corriger l'URL de génération PDF dans le React frontend. Actuellement, `PosPage.tsx` utilise `res.document_number` au lieu de `res.id` pour l'URL du PDF invoice.

## Bug corrigé
- Bug 3 (critique) : `window.open('/api/invoices/${res.document_number}/pdf')` → 404 car la route Flask attend un INT id.

## Modifications

### 1. `app.py` — `create_pos_transaction()`  (ligne 1905-1909)
Ajouter `document_id` dans la réponse JSON :
```python
return jsonify({
    'success': True,
    'document_number': doc_number,
    'document_id': inv_id,       # <-- NOUVEAU
    'document_type': doc_type,
    'total': total,
    ...
})
```

### 2. `frontend/src/components/pos/PosPage.tsx` (ligne 150-154)
Remplacer `res.document_number` par `res.document_id` pour l'URL PDF :
```tsx
if (res.document_number) {
  const url = res.document_type === "ticket"
    ? `/api/pos/tickets/${res.document_number}`
    : `/api/invoices/${res.document_id}/pdf`   // <-- corrigé
  window.open(url, "_blank")
}
```

## Test
```bash
# 1. Vérifier que le backend retourne document_id
curl -s -X POST http://localhost:5001/api/pos/transactions \
  -H 'Content-Type: application/json' \
  -d '{
    "session_id": 1,
    "customer_id": 1,
    "items": [{"product_id": 10, "quantity": 1, "unit_price": 4.80, "product_name": "Agraffes", "product_sku": "AGR002"}],
    "payment_method": "cash",
    "tendered_amount": 10
  }'
```

## Critères d'acceptance
- [ ] `window.open` utilise `/api/invoices/<ID>/pdf` (pas `<NUM>/pdf`)
- [ ] Le PDF d'une facture POS s'ouvre correctement (pas 404)
- [ ] Les tickets POS continuent de fonctionner
