@echo off
REM StockPro Post-Installation Script
REM Vérifie que le port 5001 est libre et configure l'environnement

setlocal enabledelayedexpansion

echo === StockPro - Post-Installation ===
echo.

REM Vérifier si le port 5001 est utilisé
netstat -ano | findstr ":5001 " > nul 2>&1
if %errorlevel% equ 0 (
    echo [AVERTISSEMENT] Le port 5001 est deja utilise.
    echo Veuillez fermer l'application qui utilise ce port.
    echo.
    choice /M "Continuer quand meme"
    if !errorlevel! equ 2 exit /b
)

REM Créer le dossier de logs
if not exist "%APPDATA%\StockPro\logs" (
    mkdir "%APPDATA%\StockPro\logs"
    echo [OK] Dossier de logs cree
)

REM Vérifier l'intégrité des fichiers
if not exist "%~dp0stock-app.exe" (
    echo [ERREUR] stock-app.exe introuvable !
    pause
    exit /b 1
)

echo [OK] Installation terminee avec succes !
echo.
echo Pour lancer StockPro :
echo   1. Double-cliquez sur le raccourci "StockPro" dans le menu Demarrer
echo   2. Ou executez : %~dp0stock-app.exe --open-browser
echo.
echo Les donnees sont stockees dans : %APPDATA%\StockPro\
echo.
pause