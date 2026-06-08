# PRD — Multi-Caisse StockPro

**Version**: 1.0
**Date**: 2026-06-08
**Cycle**: OMNI Phase 2b

## Objectif

Permettre à 2-3 caisses physiques dans le même magasin d'opérer simultanément sur StockPro, avec synchronisation temps réel des stocks et isolation des sessions par caisse.

## Fonctionnalités

### F1 — Gestion des caisses enregistrées
- CRUD des caisses physiques (nom + code)
- Seed automatique "Caisse 1" à l'initialisation
- Activation/désactivation d'une caisse

### F2 — Sessions par caisse
- Ouverture de session liée à une caisse (`register_id`)
- Une seule session ouverte par caisse (pas de verrou global)
- Affichage du nom de caisse dans la barre de session
- Fermeture de session scope à la caisse

### F3 — Synchronisation temps réel (SSE)
- Connexion SSE persistante entre chaque caisse et le serveur
- Événements poussés :
  - `stock-update` : mise à jour des quantités produits
  - `transaction` : nouvelle transaction sur une autre caisse
  - `ping` : keepalive toutes les 30s
- Réception côté client : `EventSource` dans `pos.js`
- Pas de dépendance externe (Flask pur)

### F4 — Prévention des conflits stock
- Vérification des produits du panier à chaque `stock-update`
- Alerte visuelle si un produit a été déstocké par une autre caisse
- Ajustement automatique des quantités si nécessaire
- Blocage à la validation si stock insuffisant (déjà existant via `WHERE quantity >= ?`)

### F5 — Dashboard multi-caisse
- Nouveau KPI : état de chaque caisse (ouverte/fermée)
- Pour chaque caisse ouverte : session number, total ventes, nb transactions
- Lien pour basculer vers l'interface de la caisse

### F6 — Rétrocompatibilité
- Si `register_id` non fourni, comportement actuel (session unique)
- Migration de la table `pos_sessions` sans perte de données
- Les sessions existantes restent valides

## Critères d'acceptation

1. Deux caisses peuvent ouvrir une session simultanément
2. Une vente sur Caisse 1 met à jour le stock visible sur Caisse 2 en <1s
3. Si Caisse 2 a un produit dans son panier et que Caisse 1 le vend, Caisse 2 reçoit une alerte
4. Deux caisses ne peuvent pas vendre le même stock (blocage atomique SQL)
5. La fermeture d'une caisse ne ferme pas l'autre
6. Le dashboard affiche l'état de toutes les caisses en temps réel
7. Les sessions existantes (sans register_id) restent accessibles

## Métriques de succès

- 0 conflit de doublon de vente (vérifié par les logs)
- Temps de propagation stock < 1s (SSE)
- Aucune régression sur les tests existants
