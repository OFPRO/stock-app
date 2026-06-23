import os
import sys
import logging
import threading
from datetime import datetime

from services.escpos_receipt import EscposPrinter, build_escpos_commands
from services.pdf_saver import save_receipt_pdf, RECEIPTS_DIR


LOG_DIR = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), 'logs')
LOG_FILE = os.path.join(LOG_DIR, 'printing.log')

os.makedirs(LOG_DIR, exist_ok=True)

logger = logging.getLogger('printing')
logger.setLevel(logging.DEBUG)
_fh = logging.FileHandler(LOG_FILE, encoding='utf-8')
_fh.setFormatter(logging.Formatter('[%(asctime)s] %(message)s', datefmt='%Y-%m-%d %H:%M:%S'))
logger.addHandler(_fh)
logger.propagate = False


def _log(level, msg):
    getattr(logger, level)(msg)


_KNOWN_PRINTER_IDS = {
    0x04b8: 'Epson', 0x0416: 'Epson', 0x0483: 'Deli',
    0x066b: 'Xprinter', 0x0521: 'Star', 0x0456: 'Star',
    0x03f0: 'HP', 0x04a9: 'Canon', 0x04f9: 'Brother',
    0x0922: 'Dymo', 0x0fe6: 'Zebra', 0x0a5f: 'Zebra',
    0x067b: 'Prolific', 0x1a86: 'QinHeng',
}


def _discover_windows():
    devices = []

    # Phase 1: Build port→queue mapping and list of all local printers
    port_to_queue = {}
    all_local = []
    try:
        import win32print
        for p in win32print.EnumPrinters(2):
            if len(p) < 8:
                continue
            flags, _, pname, _, _, _, _, port = p[:8]
            is_local = bool(flags & 0x00001000)
            if pname and port and is_local:
                port_to_queue[port.upper()] = pname
            if pname and is_local:
                all_local.append(pname)
    except ImportError:
        return devices

    # Phase 2: Scan USB registry to get VID/PID + match queue name via port
    try:
        import winreg
        base = r'SYSTEM\CurrentControlSet\Enum\USB'
        key = winreg.OpenKey(winreg.HKEY_LOCAL_MACHINE, base)
        i = 0
        while True:
            try:
                subkey_name = winreg.EnumKey(key, i)
                i += 1
                if not subkey_name.startswith('VID_'):
                    continue
                try:
                    dev_key = winreg.OpenKey(key, subkey_name)
                except OSError:
                    continue
                j = 0
                while True:
                    try:
                        instance = winreg.EnumKey(dev_key, j)
                        j += 1
                    except OSError:
                        break
                    inst_key = winreg.OpenKey(dev_key, instance)
                    try:
                        svc, _ = winreg.QueryValueEx(inst_key, 'Service')
                    except OSError:
                        svc = ''
                    try:
                        desc, _ = winreg.QueryValueEx(inst_key, 'DeviceDesc')
                    except OSError:
                        desc = ''
                    if svc.lower() != 'usbprint' and 'usbprint' not in desc.upper():
                        winreg.CloseKey(inst_key)
                        continue
                    parts = subkey_name.replace('VID_', '').split('&PID_')
                    if len(parts) != 2:
                        winreg.CloseKey(inst_key)
                        continue
                    vid = parts[0].lower()
                    pid = parts[1].split('&')[0].lower()

                    # Read Windows port name for this USB device
                    port_name = None
                    try:
                        params_key = winreg.OpenKey(inst_key, 'Device Parameters')
                        port_name, _ = winreg.QueryValueEx(params_key, 'PortName')
                        winreg.CloseKey(params_key)
                    except OSError:
                        pass

                    # Cross-reference port name to real printer queue name
                    queue_name = None
                    if port_name:
                        queue_name = port_to_queue.get(port_name.upper())

                    device_name = queue_name or (
                        desc.split(';')[-1].strip() if ';' in desc else desc
                    )
                    manufacturer = _KNOWN_PRINTER_IDS.get(int(vid, 16), 'USB Printer')
                    devices.append({
                        'name': device_name or f'USB Printer ({vid}:{pid})',
                        'vendor_id': f'0x{vid}',
                        'product_id': f'0x{pid}',
                        'instance_id': instance,
                        'manufacturer': manufacturer,
                        'description': device_name,
                        'connection_type': 'windows',
                    })
                    winreg.CloseKey(inst_key)
                winreg.CloseKey(dev_key)
            except OSError:
                break
        winreg.CloseKey(key)
    except ImportError:
        pass

    # Phase 3: If no USB devices found, list all local printers as fallback
    if not devices:
        seen = set()
        for qname in all_local:
            if qname and qname not in seen:
                seen.add(qname)
                devices.append({
                    'name': qname,
                    'vendor_id': '',
                    'product_id': '',
                    'instance_id': '',
                    'manufacturer': 'Windows Printer',
                    'description': qname,
                    'connection_type': 'windows',
                })

    return devices


def _discover_pyusb():
    devices = []
    try:
        import usb.core
        import usb.backend.libusb1
    except ImportError:
        return devices
    try:
        found = usb.core.find(find_all=True)
        if found:
            for dev in found:
                vid = dev.idVendor
                pid = dev.idProduct
                if vid is None:
                    continue
                manufacturer = _KNOWN_PRINTER_IDS.get(vid, 'Inconnu')
                try:
                    desc = usb.util.get_string(dev, dev.iProduct) if dev.iProduct else None
                except Exception:
                    desc = None
                try:
                    manu_str = usb.util.get_string(dev, dev.iManufacturer) if dev.iManufacturer else None
                except Exception:
                    manu_str = None
                name = desc or f'{manufacturer} (0x{vid:04x}:0x{pid:04x})'
                devices.append({
                    'name': name,
                    'vendor_id': f'0x{vid:04x}',
                    'product_id': f'0x{pid:04x}',
                    'manufacturer': manu_str or manufacturer,
                    'description': desc or '',
                    'bus': str(dev.bus).zfill(3),
                    'address': str(dev.address).zfill(3),
                })
    except Exception:
        pass
    return devices


def discover_printers():
    if sys.platform == 'win32':
        devices = _discover_windows()
        if devices:
            return devices
    return _discover_pyusb()


def list_printers():
    result = discover_printers()
    return result


def check_printer_status(config):
    if not config:
        return {'status': 'not_configured'}
    conn_type = config.get('connection_type', 'network')
    if conn_type == 'network' and not config.get('host'):
        return {'status': 'not_configured'}
    if conn_type == 'usb' and not config.get('usb_vendor_id'):
        return {'status': 'not_configured'}
    if conn_type == 'windows' and not config.get('printer_name') and not config.get('host'):
        return {'status': 'not_configured'}
    try:
        printer = EscposPrinter(config)
        return printer.check_status()
    except Exception as e:
        return {'status': 'offline', 'error': str(e)}


def print_receipt(ticket_data, printer_config):
    result = {
        'ticket_number': ticket_data.get('ticket_number', 'unknown'),
        'pdf_path': None,
        'print_status': None,
        'print_error': None,
        'generated_at': datetime.now().isoformat(),
    }

    pdf_path = None
    try:
        pdf_path = save_receipt_pdf(ticket_data)
        result['pdf_path'] = pdf_path
        _log('info', f"PDF sauvegardé: {pdf_path}")
    except Exception as e:
        _log('error', f"Erreur sauvegarde PDF: {e}")
        result['print_error'] = f"Erreur PDF: {str(e)}"
        return result

    if not printer_config or not printer_config.get('auto_print', 1):
        _log('info', "Impression automatique désactivée")
        result['print_status'] = 'skipped'
        return result

    try:
        paper_width = printer_config.get('paper_width', 80)
        escpos_text = build_escpos_commands(ticket_data, width=paper_width)
        printer = EscposPrinter(printer_config)
        printer.connect()
        printer.print_receipt_escpos(escpos_text)
        printer.feed(3)
        printer.cut_paper()
        printer.disconnect()

        result['print_status'] = 'success'
        printer_name = printer_config.get('host', printer_config.get('printer_name', 'Deli 886BW'))
        _log('info', f"Ticket {result['ticket_number']} → {printer_name} | SUCCÈS")
    except Exception as e:
        result['print_status'] = 'error'
        result['print_error'] = str(e)
        _log('error', f"Ticket {result['ticket_number']} → ÉCHEC: {e}")

    return result


def print_ticket_raw(ticket_data, printer_config):
    """Print ticket as raw ESC/POS text — no PDF, no auto_print check."""
    result = {
        'ticket_number': ticket_data.get('ticket_number', 'unknown'),
        'print_status': None,
        'print_error': None,
    }
    try:
        paper_width = printer_config.get('paper_width', 80)
        escpos_text = build_escpos_commands(ticket_data, width=paper_width)
        printer = EscposPrinter(printer_config)
        printer.connect()
        printer.print_receipt_escpos(escpos_text)
        printer.feed(3)
        printer.cut_paper()
        printer.disconnect()
        result['print_status'] = 'success'
        _log('info', f"Ticket {result['ticket_number']} → RAW | SUCCÈS")
    except Exception as e:
        result['print_status'] = 'error'
        result['print_error'] = str(e)
        _log('error', f"Ticket {result['ticket_number']} → RAW | ÉCHEC: {e}")
    return result


def auto_print_async(ticket_data, printer_config):
    try:
        return print_receipt(ticket_data, printer_config)
    except Exception as e:
        _log('error', f"Erreur impression asynchrone: {e}")
        return {'print_status': 'error', 'print_error': str(e)}
