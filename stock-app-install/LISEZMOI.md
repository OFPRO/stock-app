# StockPro - Gestion de Stock

Suite de gestion de stock pour **Bibliotheque Badr** — Marrakech, Maroc.

## Installateurs disponibles

### macOS 🍎
`macos/StockPro.dmg` — glisser dans Applications, double-cliquer pour lancer
- Serveur local sur `http://localhost:5001`
- Base de données SQLite auto-initialisée
- ~13 MB

### Windows 🪟
Deux options dans `windows/` :

1. **Version portable** : `StockPro-Portable.zip`
   - Décompresser, lancer `start.bat` (Windows) ou `start.command` (macOS/Linux)
   - Nécessite Python 3.12+ installé

2. **Installateur complet** (génération sur machine Windows) :
   - Aller dans le dossier `build/` du projet source
   - Lancer `python make_installer.py`
   - Nécessite : PyInstaller, Inno Setup 6+

## Utilisation
1. Lancer l'application (serveur Flask sur le port 5001)
2. Ouvrir `http://localhost:5001` dans le navigateur
3. L'interface s'affiche automatiquement au démarrage

## Données
- Base de données : `stock.db` dans le dossier de l'application
- Pas de migration nécessaire — auto-initialisation au premier lancement
- Option `--data-dir` pour choisir un dossier de données personnalisé

---
StockPro v1.0.0 — Bibliotheque Badr, Marrakech
