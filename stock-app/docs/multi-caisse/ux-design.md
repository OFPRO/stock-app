# UX Design — Multi-Caisse

**Cycle**: OMNI Phase 3

## Changements UI

### 1. Barre de session POS (index.html)

Avant :
```
[Session: SES-20260608-0001] [Ouverte]  [Ouvrir Caisse] [Fermer Caisse]
```

Après :
```
[Caisse 1 ▼] [Caissier: ___] [Session: SES-...] [Ouverte] [Fermer Caisse]
```

- Dropdown de sélection de caisse en haut à gauche
- Champ optionnel "Caissier" (nom libre, pas de login)
- Le bouton "Ouvrir Caisse" devient "Ouvrir [Caisse 1]"

### 2. Dashboard — Carte Caisses

Nouvelle carte affichant :
- Liste des caisses avec badge d'état
- Pour chaque caisse ouverte : session, ventes, transactions
- Indicateur temps réel (dernière mise à jour SSE)

### 3. Notifications conflit

Pop-up d'alerte quand un produit du panier est vendu par une autre caisse :
```
⚠️ Caisse 2 a vendu "iPad Pro" — stock restant: 2
```

### 4. Session par register

Aucun changement dans le workflow d'encaissement :
- Mêmes boutons, mêmes champs, mêmes écrans
- Seule la sélection initiale (quelle caisse) est nouvelle
