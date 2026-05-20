# StockPro iOS

Application iOS native de gestion de stock pour Bibliotheque Badr — Marrakech.

## Prérequis

- Xcode 16+
- iOS 17+ (appareil ou simulateur)
- Backend Flask en cours d'exécution sur `localhost:5001`

## Installation

1. Ouvrir le dossier `ios/StockPro` dans Xcode :
   ```bash
   open ios/StockPro/Package.swift
   ```
   Ou double-cliquer sur `Package.swift` dans le Finder.

2. Sélectionner le schéma `StockPro` et un simulateur iOS 17+.

3. Build and Run (Cmd+R).

## Architecture

MVVM + Service Layer avec injection de dépendances manuelle via `@Environment`.

### Structure

```
StockPro/
├── App/               # Point d'entrée, DI Container
├── Views/             # SwiftUI Views par module
├── ViewModels/        # ViewModel par écran
├── Services/          # Protocoles + implémentations API
├── Networking/        # APIClient, Endpoint, DTOs
├── Data/              # SwiftData modèles + Store
├── Security/          # Keychain, PIN manager
├── DesignSystem/      # Tokens, composants réutilisables
└── Utilities/         # AppError, Logger, Reachability
```

## Roadmap

Voir `docs/planning-artifacts/StockPro_iOS_ARCHITECTURE.md`.

## API

Le backend doit être accessible. Configurer l'URL dans `Info.plist` > `API_BASE_URL`.
