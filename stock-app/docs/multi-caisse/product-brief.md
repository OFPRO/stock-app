# Product Brief — Multi-Caisse (Multi-Register POS)

**Version**: 1.0
**Date**: 2026-06-08
**Statut**: Approuvé
**Cycle OMNI**: Phase 1 → Phase 2

## Résumé

Ajout du support multi-caisse pour le module POS de StockPro. Permet à 2-3 caisses physiques dans le même point de vente d'opérer simultanément sur la même base de données SQLite, avec synchronisation temps réel via Server-Sent Events (SSE) pour prévenir les doublons et les erreurs de calcul.

## Contexte

Actuellement, StockPro ne supporte qu'une seule session de caisse à la fois. La contrainte est explicite dans `open_pos_session()` : une vérification empêche l'ouverture d'une seconde session si une autre est déjà ouverte. Pour un magasin avec plusieurs points de vente physiques, cette limitation bloque l'utilisation simultanée par plusieurs caissiers.

## Problème

- Un seul POS actif à la fois
- Impossible d'avoir 2 caissiers qui encaissent en parallèle
- Pas de visibilité sur l'activité des autres caisses

## Solution

1. **Table `pos_registers`** : enregistrement des caisses physiques (nom + code)
2. **Migration `pos_sessions.register_id`** : chaque session est liée à une caisse
3. **Suppression du verrou global** : une session par register, pas par système
4. **SSE temps réel** : synchronisation instantanée du stock entre caisses
5. **Dashboard multi-caisse** : visibilité sur l'état de toutes les caisses

## Décisions clés

| Décision | Choix | Raison |
|----------|-------|--------|
| Nombre max de caisses | 2-3 | Petit/moyen magasin, pas de grande surface |
| Temps réel | SSE (Server-Sent Events) | Push natif, pas de polling, compatible Flask |
| Identification | Nom de caisse uniquement | Pas de login caissier, pas d'auth |
| Base de données | SQLite unique (inchangée) | Même backend Flask, même DB |
| Verrouillage stock | Optimistic locking existant | `WHERE quantity >= ?` déjà atomique |

## Non-concerné

- Application Android (POS Android non modifié)
- Application iOS
- Installateurs / packaging
- Authentification / rôles utilisateur
