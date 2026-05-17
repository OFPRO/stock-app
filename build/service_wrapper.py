"""
StockPro Service Wrapper - NSSM integration for Windows service mode.
Installs/uninstalls StockPro as a Windows service using NSSM.

Usage:
    python service_wrapper.py install <data-dir> [port]
    python service_wrapper.py uninstall
    python service_wrapper.py status
"""

import os
import sys
import subprocess
import winreg


NSSM_EXE = os.path.join(os.path.dirname(__file__), 'nssm.exe')
SERVICE_NAME = 'StockProService'
SERVICE_DISPLAY = 'StockPro - Gestion de Stock'
APP_EXE = os.path.join(os.path.dirname(__file__), 'stock-app.exe')


def _nssm(*args):
    cmd = [NSSM_EXE] + list(args)
    result = subprocess.run(cmd, capture_output=True, text=True)
    if result.returncode != 0:
        print(f'NSSM error: {result.stderr.strip()}')
    return result.returncode == 0


def install(data_dir, port=5001):
    stock_exe = os.path.join(os.path.dirname(__file__), 'stock-app.exe')
    if not os.path.exists(stock_exe):
        print(f'Error: {stock_exe} not found')
        return False

    os.makedirs(data_dir, exist_ok=True)
    log_dir = os.path.join(data_dir, 'logs')
    os.makedirs(log_dir, exist_ok=True)

    if not _nssm('install', SERVICE_NAME, stock_exe):
        return False

    args = f'--data-dir "{data_dir}" --port {port} --service'
    _nssm('set', SERVICE_NAME, 'AppParameters', args)
    _nssm('set', SERVICE_NAME, 'DisplayName', SERVICE_DISPLAY)
    _nssm('set', SERVICE_NAME, 'Description',
          'StockPro - Application de gestion de stock pour Bibliotheque Badr')
    _nssm('set', SERVICE_NAME, 'Start', 'SERVICE_AUTO_START')
    _nssm('set', SERVICE_NAME, 'AppStdout',
          os.path.join(log_dir, 'stockpro.log'))
    _nssm('set', SERVICE_NAME, 'AppStderr',
          os.path.join(log_dir, 'stockpro-error.log'))
    _nssm('set', SERVICE_NAME, 'AppRotateFiles', '1')
    _nssm('set', SERVICE_NAME, 'AppRotateSeconds', '86400')
    _nssm('set', SERVICE_NAME, 'AppRotateBytes', '10485760')

    if _nssm('start', SERVICE_NAME):
        print(f'Service {SERVICE_NAME} installed and started successfully')
        return True
    return False


def uninstall():
    _nssm('stop', SERVICE_NAME)
    if _nssm('remove', SERVICE_NAME, 'confirm'):
        print(f'Service {SERVICE_NAME} removed successfully')
        return True
    return False


def status():
    result = subprocess.run(
        [NSSM_EXE, 'status', SERVICE_NAME],
        capture_output=True, text=True
    )
    print(f'Service status: {result.stdout.strip() or result.stderr.strip()}')
    return result.returncode == 0


if __name__ == '__main__':
    if len(sys.argv) < 2:
        print(__doc__)
        sys.exit(1)

    command = sys.argv[1].lower()

    if command == 'install' and len(sys.argv) >= 3:
        data_dir = sys.argv[2]
        port = int(sys.argv[3]) if len(sys.argv) > 3 else 5001
        sys.exit(0 if install(data_dir, port) else 1)
    elif command == 'uninstall':
        sys.exit(0 if uninstall() else 1)
    elif command == 'status':
        sys.exit(0 if status() else 1)
    else:
        print(__doc__)
        sys.exit(1)