# -*- mode: python ; coding: utf-8 -*-
import os
import sys
import escpos

ROOT = os.getcwd()
ESCROOT = os.path.dirname(escpos.__file__)
block_cipher = None

a = Analysis(
    [os.path.join(ROOT, 'app.py')],
    pathex=[],
    binaries=[],
    datas=[
        (os.path.join(ROOT, 'templates'), 'templates'),
        (os.path.join(ROOT, 'static'), 'static'),
        (os.path.join(ROOT, 'routes'), 'routes'),
        (os.path.join(ESCROOT, 'capabilities.json'), 'escpos'),
        (os.path.join(ROOT, 'VERSION'), '.'),
    ],
    hiddenimports=[
        'flask',
        'jinja2',
        'jinja2.ext',
        'sqlite3',
        'csv',
        'html',
        'io',
        'datetime',
        'random',
        'argparse',
        'webbrowser',
        'os',
        'sys',
        'http',
        'http.server',
        'http.client',
        'email',
        'email.mime',
        'threading',
        'time',
        'PIL',
    ],
    hookspath=[],
    hooksconfig={},
    runtime_hooks=[],
    excludes=[
        'tests',
        'seed.py',
        'reset_test_db.py',
        'add_indexes.py',
        'pytest',
        '_pytest',
        'tkinter',
        'matplotlib',
        'numpy',
        'cv2',
    ],
    win_no_prefer_redirects=False,
    win_private_assemblies=False,
    cipher=block_cipher,
    noarchive=False,
)

pyz = PYZ(a.pure, a.zipped_data, cipher=block_cipher)

exe = EXE(
    pyz,
    a.scripts,
    a.binaries,
    a.zipfiles,
    a.datas,
    [],
    name='stock-app',
    debug=False,
    bootloader_ignore_signals=False,
    strip=False,
    upx=True,
    upx_exclude=[],
    runtime_tmpdir=None,
    console=True,
    disable_windowed_traceback=False,
    argv_emulation=False,
    target_arch=None,
    codesign_identity=None,
    entitlements_file=None,
)

if sys.platform == 'win32':
    exe.icon = os.path.join(ROOT, 'build', 'assets', 'icon.ico')