import io
from datetime import datetime

from escpos.printer import Network, Usb
from escpos import escpos


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
        self._add_empty()
        self._add(_center('Bibliotheque Badr', self.width))
        self._add(_center('Rue Mohammed V, Gueliz', self.width))
        self._add(_center('Marrakech, Maroc', self.width))
        self._add_empty()
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
