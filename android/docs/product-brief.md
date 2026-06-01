# Product Brief — StockPro Android

## 1. Contexte

StockPro est un système de gestion de stock pour **Bibliothèque Badr**, Marrakech.
Il existe déjà en version **Web** (Flask + React) et **iOS** (SwiftUI).
L'objectif est de produire un port Android natif avec une parité fonctionnelle complète.

## 2. Objectif

- Offrir aux utilisateurs Android la même expérience que l'app iOS existante
- Consommer la même API Flask backend (96 endpoints REST)
- Assurer la continuité entre les plateformes (même base de données, mêmes règles métier)

## 3. Public cible

- **Utilisateurs finaux :** Équipe de la Bibliothèque Badr (gestionnaires de stock, caissiers)
- **Usage :** Quotidien, en magasin (caisse, scanner) et en gestion (dashboard, stocks, factures)
- **Langue :** Français (comme l'app iOS et web)

## 4. Stack technique

| Couche | Technologie |
|--------|-------------|
| Langage | Kotlin 2.0.21 |
| UI | Jetpack Compose + Material 3 |
| Architecture | Clean Architecture multi-module (MVVM) |
| DI | Hilt 2.52 |
| Réseau | Retrofit + OkHttp (vs APIClient actor iOS) |
| Cache local | Room 2.6.1 (vs SwiftData iOS) |
| Scanner | CameraX + ML Kit (vs AVCapture iOS) |
| Auth | PIN avec SHA-256 + EncryptedSharedPreferences (même logique iOS) |
| Charts | (à définir — iOS utilise Charts framework) |
| Min SDK | 26 (Android 8.0) |
| Cible | API 35 |

## 5. Fonctionnalités (feature parity iOS)

### Core (socle technique)
- [ ] Networking : Retrofit services pour les 96 endpoints
- [ ] DTOs : Port des 9 fichiers DTOs iOS (~50 modèles)
- [ ] Offline cache : Room DAOs pour products, customers, suppliers, warehouses, KPIs, notifications
- [ ] Design System : Composants Compose réutilisables (Button, Badge, Card, TextField, Skeleton, ErrorView)
- [ ] Thème Material 3 : Brand `#1E3A6F`, Accent `#F5A623`

### Modules fonctionnels (15 features)

| Module | Priorité | Écrans iOS équivalents |
|--------|----------|----------------------|
| **Dashboard** | P0 | KPIs (12 cartes), 3 charts (line, donut, bar), quick actions |
| **Products** | P0 | Liste + recherche, détail (pricing tiers 4 niveaux), formulaire création/édition, swipe delete |
| **POS / Caisse** | P0 | Session ouvre/ferme, panier, scan code-barres, pricing tiers, paiement cash/carte/mixte, crédit, mouvements caisse, reçu |
| **Scanner** | P0 | CameraX + ML Kit, saisie manuelle, historique |
| **Customers** | P1 | Liste + recherche, détail, formulaire, types (school/student/company/particulier), badge fidélité |
| **Suppliers** | P1 | Liste + recherche, détail, formulaire |
| **Warehouses** | P1 | Liste, détail, formulaire |
| **Locations** | P1 | Liste + filtre entrepôt, détail, formulaire (rack/shelf/zone) |
| **Stock Movements** | P1 | Liste + filtres, création mouvement (entrée/sortie/transfert) |
| **Orders** | P1 | Liste + filtre statut, détail + items |
| **Invoices** | P1 | Liste + filtre statut, détail + items |
| **Notifications** | P1 | Liste, marquer lu, tout marquer lu |
| **Reorder Rules** | P2 | Liste, création, suggestions réapprovisionnement |
| **Auth** | P2 | Écran PIN 6 chiffres, clavier custom, SHA-256, 5 tentatives + lockout 30s |
| **Settings** | P2 | À propos, changement PIN, debug (reset/seed) |

### Placeholders (non implémentés dans iOS — à reporter)
- Reports (tableau de bord avancé)
- Session History (historique des sessions POS)
- Main Account (compte principal)
- Nouvelle commande (création directe)

## 6. Contraintes techniques

- Backend : `http://192.168.11.102:5001` (LAN local) — configurable
- API JSON, paramétrée avec `?` (pas de f-string SQL)
- Pricing tiers : Normal, Loyal (-15%), Student (-15%), School (-20%)
- Toutes les UI en français
- ATS désactivé pour les requêtes HTTP en dev

## 7. Architecture cible

```
App2/
├── app/                  → Point d'entrée, Hilt, navigation (NavHost)
├── core/
│   ├── data/
│   │   ├── remote/       → Retrofit interfaces + DTOs
│   │   ├── local/        → Room DAOs + entities
│   │   └── repository/   → Implémentations repository
│   ├── domain/
│   │   ├── model/        → Modèles domaine (Kotlin pur)
│   │   └── repository/   → Interfaces repository
│   └── ui/
│       ├── theme/        → Material 3 theme
│       └── components/   → Composables design system
├── feature/              → 1 module par feature
│   ├── dashboard/
│   ├── products/
│   ├── customers/
│   ├── pos/
│   └── ...
├── build-logic/          → Convention plugins Gradle
└── gradle/
    └── libs.versions.toml
```

## 8. Critères de succès

1. `./gradlew assembleDebug` passe
2. L'app se connecte au backend Flask et affiche les données réelles
3. Feature parity avec iOS vérifiée
4. Tests unitaires pour chaque ViewModel
