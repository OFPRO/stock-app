import os

_SERVICES_DIR = os.path.dirname(os.path.abspath(__file__))
_PROJECT_DIR = os.path.dirname(_SERVICES_DIR)

FONT_PATH = os.path.join(_PROJECT_DIR, 'static', 'fonts', 'DejaVuSans.ttf')
FONT_PATH_BOLD = os.path.join(_PROJECT_DIR, 'static', 'fonts', 'DejaVuSans-Bold.ttf')
FONT_NAME = 'DejaVu'


def setup_pdf(pdf):
    pdf.add_font(FONT_NAME, '', FONT_PATH, uni=True)
    pdf.add_font(FONT_NAME, 'B', FONT_PATH_BOLD, uni=True)
