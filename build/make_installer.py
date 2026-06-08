#!/usr/bin/env python3
"""
StockPro Build Orchestrator
Builds the complete Windows installer package.

Usage:
    python make_installer.py              # Full build
    python make_installer.py --bundle     # PyInstaller bundle only
    python make_installer.py --installer  # Inno Setup installer only
    python make_installer.py --portable   # Portable ZIP only (macOS/Linux)
"""

import os
import sys
import shutil
import subprocess
import argparse

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
BUILD_DIR = os.path.join(ROOT, 'build')
DIST_DIR = os.path.join(ROOT, 'dist')
SPEC_FILE = os.path.join(BUILD_DIR, 'pyinstaller.spec')
INNO_SCRIPT = os.path.join(BUILD_DIR, 'installer.iss')


def clean():
    for d in [DIST_DIR]:
        if os.path.exists(d):
            shutil.rmtree(d)
    os.makedirs(DIST_DIR, exist_ok=True)
    print('[CLEAN] Dist directory cleaned')


def install_deps():
    print('[DEPS] Installing Python dependencies...')
    subprocess.run(
        [sys.executable, '-m', 'pip', 'install', '-r',
         os.path.join(ROOT, 'requirements.txt')],
        check=True
    )
    subprocess.run(
        [sys.executable, '-m', 'pip', 'install', 'pyinstaller'],
        check=True
    )
    print('[DEPS] Dependencies installed')


def bundle_pyinstaller():
    print('[PYINSTALLER] Bundling application...')
    subprocess.run(
        [sys.executable, '-m', 'PyInstaller', SPEC_FILE,
         '--distpath', DIST_DIR, '--workpath',
         os.path.join(BUILD_DIR, 'build_tmp')],
        check=True, cwd=ROOT
    )
    print(f'[PYINSTALLER] Bundle created at {os.path.join(DIST_DIR, "stock-app")}')


def build_portable():
    """Build a portable zip (cross-platform fallback)"""
    print('[PORTABLE] Building portable package...')

    bundle_dir = os.path.join(DIST_DIR, 'stock-app')
    os.makedirs(bundle_dir, exist_ok=True)

    app_files = [
        'app.py', 'VERSION', 'requirements.txt',
    ]

    for f in app_files:
        src = os.path.join(ROOT, f)
        if os.path.exists(src):
            shutil.copy2(src, bundle_dir)

    ignore = shutil.ignore_patterns('__pycache__', '*.bak', '*.pyc')
    for dirname in ['routes', 'templates', 'static']:
        src = os.path.join(ROOT, dirname)
        dst = os.path.join(bundle_dir, dirname)
        if os.path.exists(src):
            if os.path.exists(dst):
                shutil.rmtree(dst)
            shutil.copytree(src, dst, ignore=ignore)

    launcher = os.path.join(bundle_dir, 'start.command')
    with open(launcher, 'w') as f:
        f.write('#!/bin/bash\n')
        f.write('cd "$(dirname "$0")"\n')
        f.write(f'{sys.executable} app.py --open-browser\n')
    os.chmod(launcher, 0o755)

    bat_content = '''@echo off
cd /d "%~dp0"
start "" stock-app.exe --open-browser
'''
    with open(os.path.join(bundle_dir, 'start.bat'), 'w') as f:
        f.write(bat_content)

    shutil.make_archive(
        os.path.join(DIST_DIR, 'StockPro-Portable'),
        'zip', DIST_DIR, 'stock-app'
    )
    shutil.rmtree(bundle_dir)
    print(f'[PORTABLE] Created {os.path.join(DIST_DIR, "StockPro-Portable.zip")}')


def build_installer():
    """Build Inno Setup installer (Windows only)"""
    iscc = shutil.which('iscc')
    if not iscc:
        # Try common paths
        for p in [
            r'C:\Program Files (x86)\Inno Setup 6\ISCC.exe',
            r'C:\Program Files\Inno Setup 6\ISCC.exe',
            r'C:\Program Files (x86)\Inno Setup 5\ISCC.exe',
        ]:
            if os.path.exists(p):
                iscc = p
                break

    if not iscc:
        print('[ERROR] Inno Setup (iscc) not found.')
        print('  Install Inno Setup from: https://jrsoftware.org/isdl.php')
        print('  Or build portable: python make_installer.py --portable')
        sys.exit(1)

    print('[INNO] Building installer...')
    subprocess.run([iscc, INNO_SCRIPT], check=True, cwd=BUILD_DIR)

    installer = os.path.join(DIST_DIR, 'StockPro-Setup.exe')
    if os.path.exists(installer):
        size_mb = os.path.getsize(installer) / (1024 * 1024)
        print(f'[INNO] Installer created: {installer} ({size_mb:.1f} MB)')
    else:
        print('[ERROR] Installer not found after build')
        sys.exit(1)


def main():
    parser = argparse.ArgumentParser(description='StockPro Build Orchestrator')
    parser.add_argument('--bundle', action='store_true',
                        help='PyInstaller bundle only')
    parser.add_argument('--installer', action='store_true',
                        help='Inno Setup installer only')
    parser.add_argument('--portable', action='store_true',
                        help='Build portable ZIP (cross-platform)')
    parser.add_argument('--clean', action='store_true',
                        help='Clean dist directory')
    args = parser.parse_args()

    if args.clean or not any([args.bundle, args.installer, args.portable]):
        clean()

    if args.portable:
        install_deps()
        build_portable()
        return

    if args.bundle or (not args.installer and not args.portable):
        install_deps()
        bundle_pyinstaller()

    if args.installer or (not args.bundle and not args.portable):
        build_installer()

    print('\n[DONE] Build complete!')


if __name__ == '__main__':
    main()