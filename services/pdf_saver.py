import os
from datetime import datetime
from fpdf import FPDF


RECEIPTS_DIR = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), 'receipts')


class ReceiptPdf:
    def __init__(self):
        self.pdf = FPDF(orientation='P', unit='mm', format=(80, 200))
        self.pdf.set_auto_page_break(auto=True, margin=5)
        self.pdf.add_page()

    def _add_line(self, text, size=8, bold=False, align='C'):
        self.pdf.set_font('Helvetica', 'B' if bold else '', size)
        self.pdf.cell(0, 4, text, align=align, new_x='LMARGIN', new_y='NEXT')

    def _add_pair(self, left, right, size=8):
        self.pdf.set_font('Helvetica', '', size)
        w = self.pdf.w - 2 * self.pdf.l_margin
        self.pdf.cell(w * 0.55, 4, left)
        self.pdf.cell(w * 0.45, 4, right, align='R', new_x='LMARGIN', new_y='NEXT')

    def _add_separator(self, char='-'):
        self.pdf.set_font('Helvetica', '', 6)
        self.pdf.cell(0, 2, char * 48, align='C', new_x='LMARGIN', new_y='NEXT')

    def header(self, ticket_number):
        self._add_line('Bibliotheque Badr', size=12, bold=True)
        self._add_line('Rue Mohammed V, Gueliz', size=7)
        self._add_line('Marrakech, Maroc', size=7)
        self._add_line('', size=2)
        self._add_line('REÇU DE CAISSE', size=10, bold=True)
        self._add_line(ticket_number, size=8)
        self._add_separator()

    def info(self, created_at, session_number):
        dt_str = created_at[:16] if created_at else '-'
        self._add_pair('Date:', dt_str)
        self._add_pair('Session:', session_number if session_number else '-')
        self._add_separator()

    def table_header(self):
        self.pdf.set_font('Helvetica', 'B', 7)
        w = self.pdf.w - 2 * self.pdf.l_margin
        col_w = [w * 0.45, w * 0.15, w * 0.20, w * 0.20]
        self.pdf.cell(col_w[0], 4, 'Article')
        self.pdf.cell(col_w[1], 4, 'Qte', align='C')
        self.pdf.cell(col_w[2], 4, 'Prix', align='R')
        self.pdf.cell(col_w[3], 4, 'Total', align='R', new_x='LMARGIN', new_y='NEXT')
        self.pdf.set_font('Helvetica', '', 6)
        self.pdf.cell(0, 1, '-' * 48, align='C', new_x='LMARGIN', new_y='NEXT')

    def add_item(self, name, qty, price, total):
        w = self.pdf.w - 2 * self.pdf.l_margin
        col_w = [w * 0.45, w * 0.15, w * 0.20, w * 0.20]
        name_trunc = name[:28] if name else ''
        self.pdf.set_font('Helvetica', '', 6)
        self.pdf.cell(col_w[0], 4, name_trunc)
        self.pdf.set_font('Helvetica', '', 7)
        self.pdf.cell(col_w[1], 4, str(qty), align='C')
        self.pdf.cell(col_w[2], 4, f'{price:.2f}', align='R')
        self.pdf.cell(col_w[3], 4, f'{total:.2f}', align='R', new_x='LMARGIN', new_y='NEXT')

    def totals(self, subtotal, discount, tax, total):
        self._add_separator()
        self._add_pair('Sous-total:', f'{subtotal:.2f} DH')
        if discount > 0:
            self._add_pair('Remise:', f'{discount:.2f} DH')
        self._add_pair('TVA (20%):', f'{tax:.2f} DH')
        self._add_separator()
        self.pdf.set_font('Helvetica', 'B', 10)
        w = self.pdf.w - 2 * self.pdf.l_margin
        self.pdf.cell(w * 0.55, 6, 'TOTAL:')
        self.pdf.cell(w * 0.45, 6, f'{total:.2f} DH', align='R', new_x='LMARGIN', new_y='NEXT')
        self._add_separator()

    def payment(self, method, tendered, change):
        self._add_pair('Paiement:', method.title())
        self._add_pair('Reçu:', f'{tendered:.2f} DH')
        self._add_pair('Monnaie:', f'{change:.2f} DH')
        self._add_separator()

    def footer(self):
        self._add_line('', size=4)
        self._add_line('Merci pour votre visite!', size=7)
        self._add_line('', size=4)
        self._add_line('-' * 20, size=6)
        self._add_line('Signature Caissier', size=6)

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
        self._add_line('', size=8)
        return self.pdf


def save_receipt_pdf(ticket_data):
    os.makedirs(RECEIPTS_DIR, exist_ok=True)
    ticket_number = ticket_data.get('ticket_number', 'unknown')
    filename = f'{ticket_number}.pdf'
    filepath = os.path.join(RECEIPTS_DIR, filename)
    pdf = ReceiptPdf()
    doc = pdf.build(ticket_data)
    doc.output(filepath)
    return filepath
