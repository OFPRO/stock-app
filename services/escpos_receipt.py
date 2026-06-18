import io
import os
from datetime import datetime

from escpos.printer import Network, Usb
from escpos import escpos


_PROJECT_ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
_LOGO_PATH = os.path.join(_PROJECT_ROOT, 'static', 'img', 'logo.png')


def _render_logo_escpos(max_width=384):
    try:
        from PIL import Image
        img = Image.open(_LOGO_PATH).convert('L')
        w, h = img.size
        if w > max_width:
            ratio = max_width / w
            w, h = int(w * ratio), int(h * ratio)
        img = img.resize((w, h), Image.LANCZOS)
        pixels = list(img.getdata())
        xl = (w + 7) // 8
        cmd = b'\x1b\x61\x01'
        cmd += b'\x1d\x76\x30\x00'
        cmd += bytes([xl & 0xFF, (xl >> 8) & 0xFF, h & 0xFF, (h >> 8) & 0xFF])
        row = bytearray()
        for y in range(h):
            for xb in range(xl):
                byte_val = 0
                for b in range(8):
                    px = xb * 8 + b
                    if px < w and pixels[y * w + px] < 128:
                        byte_val |= 1 << (7 - b)
                row.append(byte_val)
        cmd += bytes(row)
        return cmd
    except Exception:
        return b''


def _encode(text):
    return text.encode('cp437', errors='replace')


def _center(text, width=42):
    return text.center(width)


def _rjust(text, width=10):
    return text.rjust(width)


def _pair(left, right, width=42, sep=' '):
    lw = int(width * 0.6)
    rw = width - lw
    left = left[:lw].ljust(lw)
    right = str(right)[:rw].rjust(rw)
    return left + right


def _sep(width=42, char='-'):
    return char * width


class EscposReceiptBuilder:
    def __init__(self, width=42):
        self.width = width
        self.lines = []

    def _add(self, text=''):
        self.lines.append(text)
        return self

    def _add_empty(self, count=1):
        for _ in range(count):
            self._add()
        return self

    def header(self, ticket_number):
        self._add_empty(3)
        self._add(_center('REÇU DE CAISSE', self.width))
        self._add(_center(ticket_number, self.width))
        self._add(_sep(self.width))
        return self

    def info(self, created_at, session_number):
        self._add(_pair('Date:', created_at[:16] if created_at else '-'))
        self._add(_pair('Session:', session_number if session_number else '-'))
        self._add(_sep(self.width))
        return self

    def table_header(self):
        self._add(_pair(_pair('Article', 'Qte', width=30), _pair('Prix', 'Total', width=12), width=self.width))
        self._add(_sep(self.width))
        return self

    def add_item(self, name, qty, price, total):
        name_line = name[:28] if name else ''
        self._add(_pair(name_line, f'{qty}', width=self.width, sep='  '))
        self._add(_pair(f'{price:.2f}', f'{total:.2f}', width=self.width, sep='  '))
        return self

    def totals(self, subtotal, discount, tax, total):
        self._add(_sep(self.width))
        self._add(_pair('Sous-total:', f'{subtotal:.2f}'))
        if discount > 0:
            self._add(_pair('Remise:', f'{discount:.2f}'))
        self._add(_pair('TVA (20%):', f'{tax:.2f}'))
        self._add(_sep(self.width))
        self._add(_center(f'TOTAL: {total:.2f} DH', self.width))
        self._add(_sep(self.width))
        return self

    def payment(self, method, tendered, change):
        self._add(_pair('Paiement:', method.title()))
        self._add(_pair('Reçu:', f'{tendered:.2f}'))
        self._add(_pair('Monnaie:', f'{change:.2f}'))
        self._add(_sep(self.width))
        return self

    def footer(self):
        self._add_empty()
        self._add(_center('Merci pour votre visite!', self.width))
        self._add_empty()
        self._add(_center('-' * 20, self.width))
        self._add(_center('Signature Caissier', self.width))
        self._add_empty(3)
        return self

    def build(self, data):
        self.header(data.get('ticket_number', ''))
        self.info(data.get('created_at', ''), data.get('session_number', ''))
        self.table_header()
        for item in data.get('items', []):
            self.add_item(
                item.get('product_name', 'Article'),
                item.get('quantity', 0),
                item.get('unit_price', 0),
                item.get('line_total', 0),
            )
        self.totals(
            data.get('subtotal', 0),
            data.get('discount_total', 0),
            data.get('tax_amount', 0),
            data.get('total', 0),
        )
        self.payment(
            data.get('payment_method', 'cash'),
            data.get('tendered_amount', 0),
            data.get('change_amount', 0),
        )
        self.footer()
        return '\n'.join(self.lines)


_USBPRINT_GUID = '{28d78fad-5a12-11d1-ae5b-0000f803a963}'


def _build_usbprint_path(vid, pid, instance_id):
    vid_clean = vid.replace('0x', '').lower().zfill(4)
    pid_clean = pid.replace('0x', '').lower().zfill(4)
    return (
        f'\\\\?\\USB#VID_{vid_clean}&PID_{pid_clean}#{instance_id}'
        f'#{_USBPRINT_GUID}'
    )


def _find_usbprint_device_path(vid, pid):
    if not vid or not pid:
        return None
    try:
        import ctypes
        from ctypes import wintypes
        setupapi = ctypes.windll.setupapi
        kernel32 = ctypes.windll.kernel32

        class GUID(ctypes.Structure):
            _fields_ = [
                ('Data1', wintypes.DWORD),
                ('Data2', wintypes.WORD),
                ('Data3', wintypes.WORD),
                ('Data4', wintypes.BYTE * 8),
            ]

        g_parts = _USBPRINT_GUID.strip('{}').split('-')
        guid = GUID(
            int(g_parts[0], 16), int(g_parts[1], 16), int(g_parts[2], 16),
            (wintypes.BYTE * 8)(*bytes.fromhex(g_parts[3] + g_parts[4]))
        )

        vid_clean = vid.replace('0x', '').lower().zfill(4)
        pid_clean = pid.replace('0x', '').lower().zfill(4)
        target = f"vid_{vid_clean}&pid_{pid_clean}"

        hdev = setupapi.SetupDiGetClassDevsW(
            ctypes.byref(guid), None, None, 0x12
        )
        if not hdev or hdev == ctypes.c_void_p(-1).value:
            return None

        class SP_DEVICE_INTERFACE_DATA(ctypes.Structure):
            _fields_ = [
                ('cbSize', wintypes.DWORD),
                ('InterfaceClassGuid', GUID),
                ('Flags', wintypes.DWORD),
                ('Reserved', ctypes.c_ulong),
            ]

        index = 0
        while True:
            iface_data = SP_DEVICE_INTERFACE_DATA()
            iface_data.cbSize = ctypes.sizeof(SP_DEVICE_INTERFACE_DATA)
            if not setupapi.SetupDiEnumDeviceInterfaces(
                hdev, None, ctypes.byref(guid), index,
                ctypes.byref(iface_data)
            ):
                break
            req_size = wintypes.DWORD(0)
            setupapi.SetupDiGetDeviceInterfaceDetailW(
                hdev, ctypes.byref(iface_data), None, 0,
                ctypes.byref(req_size), None
            )
            if kernel32.GetLastError() != 122:
                index += 1
                continue
            buf = ctypes.create_string_buffer(req_size.value)
            ctypes.memmove(buf, ctypes.byref(wintypes.DWORD(4)), 4)
            if setupapi.SetupDiGetDeviceInterfaceDetailW(
                hdev, ctypes.byref(iface_data),
                buf, req_size.value, None, None
            ):
                path = ctypes.wstring_at(ctypes.addressof(buf) + 4)
                if target in path.lower():
                    setupapi.SetupDiDestroyDeviceInfoList(hdev)
                    return path
            index += 1
        setupapi.SetupDiDestroyDeviceInfoList(hdev)
    except Exception:
        pass
    return None


class _WindowsRawPrinter:
    def __init__(self, printer_name, vid='', pid='', instance_id=''):
        self._handle = None
        self._printer_name = printer_name
        self._vid = vid
        self._pid = pid
        self._instance_id = instance_id
        self._open()

    def _open(self):
        import win32file
        share_mode = 1 | 2

        if self._vid and self._pid:
            path = _find_usbprint_device_path(self._vid, self._pid)
            if path:
                try:
                    self._handle = win32file.CreateFile(
                        path, win32file.GENERIC_WRITE, share_mode,
                        None, win32file.OPEN_EXISTING, 0, None,
                    )
                    self._is_file = True
                    return
                except Exception:
                    pass

        if self._vid and self._pid and self._instance_id:
            path = _build_usbprint_path(self._vid, self._pid, self._instance_id)
            try:
                self._handle = win32file.CreateFile(
                    path, win32file.GENERIC_WRITE, share_mode,
                    None, win32file.OPEN_EXISTING, 0, None,
                )
                self._is_file = True
                return
            except Exception:
                pass

        try:
            if self._printer_name:
                import win32print
                self._handle = win32print.OpenPrinter(self._printer_name)
                return
        except Exception:
            pass

        raise RuntimeError(
            f"Impossible d'ouvrir l'imprimante: {self._printer_name}. "
            f"Vérifiez qu'elle est branchée et que le pilote Windows est installé."
        )

    def _ensure_doc(self):
        if hasattr(self, '_doc_started') and self._doc_started:
            return
        if hasattr(self, '_is_file'):
            return
        import win32print
        win32print.StartDocPrinter(self._handle, 1, ('StockPro', None, 'RAW'))
        win32print.StartPagePrinter(self._handle)
        self._doc_started = True

    def _raw(self, data):
        self._ensure_doc()
        if hasattr(self, '_is_file'):
            import win32file
            win32file.WriteFile(self._handle, data)
        else:
            import win32print
            win32print.WritePrinter(self._handle, data)

    def text(self, text):
        self._raw(text.encode('cp437', errors='replace'))

    def set(self, align='left', font='a', width=1, height=1, density=8, invert=0, smooth=False, bold=False):
        pass

    def close(self):
        if self._handle is not None:
            try:
                if hasattr(self, '_doc_started') and self._doc_started and not hasattr(self, '_is_file'):
                    import win32print
                    win32print.EndPagePrinter(self._handle)
                    win32print.EndDocPrinter(self._handle)
            except Exception:
                pass
            try:
                if hasattr(self, '_is_file'):
                    import win32file
                    win32file.CloseHandle(self._handle)
                else:
                    import win32print
                    win32print.ClosePrinter(self._handle)
            except Exception:
                pass
            self._handle = None

    def cut(self):
        self._raw(b'\x1d\x56\x42\x00')

    def qr(self, content, size=6, center=True):
        if center:
            self._raw(b'\x1b\x61\x01')
        data = content.encode('utf-8')
        pl = len(data) + 3
        ph = pl >> 8
        pl = pl & 0xFF
        self._raw(b'\x1d\x28\x6b' + bytes([pl, ph, 49, 80, 48]))
        self._raw(b'\x1d\x28\x6b' + bytes([len(data) + 3, (len(data) + 3) >> 8, 49, 81, 48]) + data)
        self._raw(b'\x1d\x28\x6b' + bytes([3, 0, 49, 82, size * 16 + 0]))

    def barcode(self, content, bc='CODE128', height=50, width=2):
        data = content.encode('utf-8')
        self._raw(b'\x1d\x68' + bytes([height]))
        self._raw(b'\x1d\x77' + bytes([width]))
        if bc == 'CODE128':
            self._raw(b'\x1d\x6b\x49' + bytes([len(data)]) + data)
        else:
            self._raw(b'\x1d\x6b\x45' + data + b'\x00')


class EscposPrinter:
    def __init__(self, config):
        self.config = config
        self._printer = None

    def connect(self):
        conn_type = self.config.get('connection_type', 'network')
        if conn_type == 'network':
            host = self.config.get('host', '')
            port = int(self.config.get('port', 9100))
            self._printer = Network(host, port)
        elif conn_type == 'usb':
            vendor = self.config.get('usb_vendor_id', '')
            product = self.config.get('usb_product_id', '')
            if vendor and product:
                self._printer = Usb(int(vendor, 16), int(product, 16))
            else:
                raise RuntimeError("Configuration USB incomplète")
        elif conn_type == 'windows':
            printer_name = self.config.get('printer_name') or self.config.get('host', '')
            if not printer_name:
                raise RuntimeError("Nom d'imprimante Windows manquant")
            vid = self.config.get('usb_vendor_id', '')
            pid = self.config.get('usb_product_id', '')
            instance_id = self.config.get('instance_id', '')
            self._printer = _WindowsRawPrinter(printer_name, vid, pid, instance_id)
        else:
            raise RuntimeError(f"Type de connexion inconnu: {conn_type}")

    def disconnect(self):
        if self._printer:
            try:
                self._printer.close()
            except Exception:
                pass
            self._printer = None

    def check_status(self):
        try:
            self.connect()
            self._printer._raw(b'\x10\x04\x01')
            self.disconnect()
            return {'status': 'online'}
        except Exception as e:
            return {'status': 'offline', 'error': str(e)}

    def print_text(self, text):
        if not self._printer:
            self.connect()
        self._printer.set(align='left', font='a', width=1, height=1, density=8, invert=0, smooth=False)
        for line in text.split('\n'):
            if line.startswith('\x1d'):
                raw_data = eval(line)
                self._printer._raw(raw_data)
                continue
            self._printer.text(line + '\n')

    def print_receipt_escpos(self, text):
        if not self._printer:
            self.connect()
        logo_bytes = _render_logo_escpos()
        if logo_bytes:
            self._printer._raw(logo_bytes)
            self._printer._raw(b'\x1b\x61\x00')
            self._printer._raw(b'\x1bd\x03')
        lines = text.split('\n')
        for line in lines:
            if line.startswith('CENTER:'):
                content = line[7:]
                self._printer.set(align='center', font='a', width=1, height=1)
                self._printer.text(content + '\n')
            elif line.startswith('BOLD:'):
                content = line[5:]
                self._printer.set(align='left', font='a', width=1, height=1, bold=True)
                self._printer.text(content + '\n')
            elif line.startswith('BIG:'):
                content = line[4:]
                self._printer.set(align='center', font='a', width=2, height=2, bold=True)
                self._printer.text(content + '\n')
            elif line == 'CUT':
                self._printer.cut()
            elif line == 'FEED:3':
                self._printer._raw(b'\x1bd\x03')
            elif line == 'FEED:5':
                self._printer._raw(b'\x1bd\x05')
            elif line == 'SEPARATOR':
                sep = '-' * 42
                self._printer.set(align='center', font='a', width=1, height=1)
                self._printer.text(sep + '\n')
            elif line.startswith('QR:'):
                content = line[3:]
                self._printer.set(align='center')
                self._printer.qr(content, size=6, center=True)
            elif line.startswith('BARCODE:'):
                content = line[8:]
                self._printer.set(align='center')
                self._printer.barcode(content, 'CODE128', height=50, width=2)
            else:
                self._printer.set(align='left', font='a', width=1, height=1)
                self._printer.text(line + '\n')

    def cut_paper(self):
        if self._printer:
            self._printer.cut()

    def feed(self, n=3):
        if self._printer:
            self._printer._raw(b'\x1bd' + bytes([n]))


def build_escpos_commands(ticket_data, width=42):
    builder = EscposReceiptBuilder(width)
    text = builder.build(ticket_data)
    return text
