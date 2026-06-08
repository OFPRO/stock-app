#!/usr/bin/env python3
"""
StockPro macOS Build Script
Builds StockPro.app and packages it as a DMG.

Usage:
    python build/macos/build_macos.py          # Full build
    python build/macos/build_macos.py --bundle # App bundle only
    python build/macos/build_macos.py --dmg    # DMG only (from existing .app)
"""

import os, sys, shutil, subprocess, argparse

ROOT = os.path.join(os.path.dirname(__file__), '..', '..')
BUILD_DIR = os.path.join(ROOT, 'build', 'macos')
DIST_DIR = os.path.join(ROOT, 'dist', 'macos')
SPEC_FILE = os.path.join(BUILD_DIR, 'pyinstaller_macos.spec')
ICON_SCRIPT = os.path.join(BUILD_DIR, 'gen_icon.py')
APP_NAME = 'StockPro.app'
DMG_NAME = 'StockPro.dmg'
VENV_PYTHON = os.path.join(ROOT, '.venv', 'bin', 'python3')


def clean():
    for d in [DIST_DIR]:
        if os.path.exists(d):
            shutil.rmtree(d)
    os.makedirs(DIST_DIR, exist_ok=True)
    print('[CLEAN] Dist directory cleaned')


def gen_icon():
    print('[ICON] Generating macOS icon...')
    subprocess.run([sys.executable, ICON_SCRIPT], check=True)
    print('[ICON] Done')


def bundle_pyinstaller():
    print('[PYINSTALLER] Bundling StockPro.app...')
    python = VENV_PYTHON if os.path.exists(VENV_PYTHON) else sys.executable
    subprocess.run(
        [python, '-m', 'PyInstaller', SPEC_FILE,
         '--distpath', DIST_DIR,
         '--workpath', os.path.join(BUILD_DIR, 'build_tmp')],
        check=True, cwd=BUILD_DIR
    )

    app_path = os.path.join(DIST_DIR, APP_NAME)
    if os.path.exists(app_path):
        total = 0
        for dirpath, _, filenames in os.walk(app_path):
            for fn in filenames:
                fp = os.path.join(dirpath, fn)
                total += os.path.getsize(fp)
        size_mb = total / (1024 * 1024)
        print(f'[PYINSTALLER] Bundle created: {app_path} ({size_mb:.1f} MB)')
    else:
        print(f'[ERROR] {app_path} not found')
        sys.exit(1)


def build_dmg():
    print('[DMG] Building disk image...')
    app_path = os.path.join(DIST_DIR, APP_NAME)
    if not os.path.exists(app_path):
        print(f'[ERROR] {app_path} not found. Run --bundle first.')
        sys.exit(1)

    dmg_path = os.path.join(DIST_DIR, DMG_NAME)
    staging = os.path.join(DIST_DIR, 'dmg_staging')
    if os.path.exists(staging):
        shutil.rmtree(staging)
    os.makedirs(staging)
    shutil.copytree(app_path, os.path.join(staging, APP_NAME))

    os.symlink('/Applications', os.path.join(staging, 'Applications'))

    subprocess.run([
        'hdiutil', 'create', '-volname', 'StockPro',
        '-srcfolder', staging,
        '-ov', '-format', 'UDZO',
        dmg_path
    ], check=True)

    shutil.rmtree(staging)
    size_mb = os.path.getsize(dmg_path) / (1024 * 1024)
    print(f'[DMG] Created: {dmg_path} ({size_mb:.1f} MB)')
    print(f'[DMG] SHA256: ', end='')
    subprocess.run(['shasum', '-a', '256', dmg_path])


def copy_to_output():
    output_dir = os.path.join(ROOT, 'stock-app-install', 'macos')
    os.makedirs(output_dir, exist_ok=True)

    dmg_src = os.path.join(DIST_DIR, DMG_NAME)
    if os.path.exists(dmg_src):
        shutil.copy2(dmg_src, os.path.join(output_dir, DMG_NAME))
        print(f'[OUTPUT] Copied DMG to {output_dir}/{DMG_NAME}')
    else:
        print(f'[WARN] DMG not found at {dmg_src}')


def main():
    parser = argparse.ArgumentParser(description='StockPro macOS Build')
    parser.add_argument('--bundle', action='store_true', help='PyInstaller bundle only')
    parser.add_argument('--dmg', action='store_true', help='DMG only (from existing .app)')
    parser.add_argument('--clean', action='store_true', help='Clean dist directory')
    args = parser.parse_args()

    if args.clean or not any([args.bundle, args.dmg]):
        clean()

    if args.dmg:
        build_dmg()
        copy_to_output()
        return

    if args.bundle or not args.dmg:
        gen_icon()
        bundle_pyinstaller()

    if not args.bundle:
        build_dmg()
        copy_to_output()

    print('\n[DONE] macOS build complete!')


if __name__ == '__main__':
    main()
