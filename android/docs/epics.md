# Epics — StockPro Android

## Epic 0 : Socle technique (P0)
**Objectif :** Infrastructure réseau, base de données locale, DI, design system

| Story | Effort | Description |
|-------|--------|-------------|
| 0.1 | M | Retrofit services pour tous les endpoints API |
| 0.2 | M | Room entities + DAOs pour le cache offline |
| 0.3 | S | Repository interfaces + implémentations (cache-then-network) |
| 0.4 | M | DTOs : port des 9 fichiers DTOs iOS (~50 modèles) |
| 0.5 | M | Hilt modules (réseau, base de données, repositories) |
| 0.6 | M | Design system composables (Button, Badge, Card, TextField, Skeleton, ErrorView) |
| 0.7 | S | Thème Material 3 (brand #1E3A6F, accent #F5A623, dark/light) |
| 0.8 | S | Navigation Compose setup (NavHost) + MainActivity |

---

## Epic 1 : Dashboard (P0)
**Objectif :** Page d'accueil avec KPIs et graphiques

| Story | Effort | Description |
|-------|--------|-------------|
| 1.1 | M | DashboardViewModel avec chargement de 6 KPIs parallèles |
| 1.2 | L | Cartes KPI (12 indicateurs : CA, ventes, stock, alertes, etc.) |
| 1.3 | L | Graphiques : line (ventes journalières), donut (catégories), bar (top produits) |
| 1.4 | S | Boutons d'action rapide (nouvelle vente, produit, commande) |
| 1.5 | S | Pull-to-refresh + skeleton loading |

---

## Epic 2 : Produits (P0)
**Objectif :** Gestion des produits avec pricing tiers

| Story | Effort | Description |
|-------|--------|-------------|
| 2.1 | M | ProductListViewModel + liste avec recherche |
| 2.2 | M | ProductDetailView : infos, pricing tiers (4 niveaux), stock, mouvements |
| 2.3 | L | ProductFormView : création/édition (nom, SKU, prix, catégorie, code-barres) |
| 2.4 | S | Swipe-to-delete avec confirmation |
| 2.5 | S | Filtre produits archivés / actifs |

---

## Epic 3 : Caisse / POS (P0)
**Objectif :** Point de vente complet — le module le plus complexe

| Story | Effort | Description |
|-------|--------|-------------|
| 3.1 | M | Gestion de session (ouvrir/fermer, comptage caisse, dépôt) |
| 3.2 | XL | POSView : panier, recherche produit, scan code-barres, sélection client |
| 3.3 | L | Paiement : cash/carte/mixte, calcul monnaie, crédit client |
| 3.4 | M | Pricing tiers : application auto du discount selon le client |
| 3.5 | M | Mouvements de caisse (entrée/sortie d'argent) |
| 3.6 | M | Ticket de caisse / reçu |
| 3.7 | S | Meilleures ventes (best-sellers pour raccourcis) |

---

## Epic 4 : Scanner (P0)
**Objectif :** Scan code-barres camera + manuel

| Story | Effort | Description |
|-------|--------|-------------|
| 4.1 | M | CameraX + ML Kit barcode scanning |
| 4.2 | S | Saisie manuelle code-barres |
| 4.3 | S | Historique des scans |

---

## Epic 5 : Clients (P1)
**Objectif :** Gestion des clients

| Story | Effort | Description |
|-------|--------|-------------|
| 5.1 | S | CustomerListViewModel + liste avec recherche |
| 5.2 | S | CustomerDetailView : contact, type (school/student/company/particulier) |
| 5.3 | M | CustomerFormView : création/édition, badge fidélité |

---

## Epic 6 : Fournisseurs (P1)
**Objectif :** Gestion des fournisseurs

| Story | Effort | Description |
|-------|--------|-------------|
| 6.1 | S | SupplierListViewModel + liste avec recherche |
| 6.2 | S | SupplierDetailView |
| 6.3 | S | SupplierFormView |

---

## Epic 7 : Entrepôts & Zones (P1)
**Objectif :** Gestion des entrepôts et zones de stockage

| Story | Effort | Description |
|-------|--------|-------------|
| 7.1 | S | WarehouseListView + création |
| 7.2 | S | LocationListView avec filtre entrepôt + création/édition |

---

## Epic 8 : Mouvements de Stock (P1)
**Objectif :** Historique et création de mouvements

| Story | Effort | Description |
|-------|--------|-------------|
| 8.1 | M | StockMovementListView : historique avec filtres (type, entrepôt, produit) |
| 8.2 | M | Création mouvement (entrée/sortie/transfert entre zones/entrepôts) |

---

## Epic 9 : Commandes Fournisseur (P1)
**Objectif :** Gestion des bons de commande

| Story | Effort | Description |
|-------|--------|-------------|
| 9.1 | M | OrderListView : filtre par statut (brouillon/reçu/payé/annulé) |
| 9.2 | M | OrderDetailView + items |

---

## Epic 10 : Factures (P1)
**Objectif :** Gestion des factures clients

| Story | Effort | Description |
|-------|--------|-------------|
| 10.1 | M | InvoiceListView : filtre par statut |
| 10.2 | M | InvoiceDetailView + items |

---

## Epic 11 : Notifications (P1)
**Objectif :** Centre d'alertes

| Story | Effort | Description |
|-------|--------|-------------|
| 11.1 | S | NotificationListView : liste avec indicateur non-lu |
| 11.2 | S | Marquer comme lu / tout marquer lu |

---

## Epic 12 : Règles de Réapprovisionnement (P2)
**Objectif :** Règles auto + suggestions

| Story | Effort | Description |
|-------|--------|-------------|
| 12.1 | S | ReorderRuleListView + création |
| 12.2 | S | Suggestions de réapprovisionnement |

---

## Epic 13 : Authentification (P2)
**Objectif :** Sécurité PIN

| Story | Effort | Description |
|-------|--------|-------------|
| 13.1 | M | Écran PIN 6 chiffres avec clavier custom |
| 13.2 | S | SHA-256 hash + EncryptedSharedPreferences |
| 13.3 | S | 5 tentatives max + lockout 30s |

---

## Epic 14 : Paramètres (P2)
**Objectif :** Configuration app

| Story | Effort | Description |
|-------|--------|-------------|
| 14.1 | S | Écran À propos (version, API URL) |
| 14.2 | S | Changement de PIN |
| 14.3 | S | Mode debug : reset/seed data |

---

## Priorité d'implémentation

Phase 4 — BUILD :
1. **Epic 0** (socle) → 2. **Epic 1** (dashboard) → 3. **Epic 2** (produits) → 4. **Epic 3** (POS)
5. **Epic 4** (scanner) → 6. **Epic 5-11** (CRUD P1) → 7. **Epic 12-14** (P2)
