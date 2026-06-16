import os
import logging
import threading
from datetime import datetime

from services.escpos_receipt import EscposPrinter, build_escpos_commands
from services.pdf_saver import save_receipt_pdf


LOG_DIR = os.environ.get('STOCKPRO_DATA_DIR', os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), 'logs'))
LOG_FILE = os.path.join(LOG_DIR, 'printing.log')

try:
    os.makedirs(LOG_DIR, exist_ok=True)
except OSError:
    LOG_DIR = None
    LOG_FILE = None

logger = logging.getLogger('printing')
logger.setLevel(logging.DEBUG)
if LOG_FILE:
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


def discover_printers():
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


def list_printers():
    result = discover_printers()
    return result


def check_printer_status(config):
    if not config or not config.get('host'):
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
        escpos_text = build_escpos_commands(ticket_data)
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


def auto_print_async(ticket_data, printer_config):
    try:
        print_receipt(ticket_data, printer_config)
    except Exception as e:
        _log('error', f"Erreur impression asynchrone: {e}")
