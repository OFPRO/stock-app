#!/usr/bin/env python3
"""Generate StockPro .icns icon from SVG using macOS built-in tools."""

import subprocess, shutil, os, sys, tempfile

HERE = os.path.dirname(os.path.abspath(__file__))
OUTPUT_ICNS = os.path.join(HERE, 'icon.icns')

SVG = '''<svg xmlns="http://www.w3.org/2000/svg" width="1024" height="1024" viewBox="0 0 1024 1024">
  <defs>
    <linearGradient id="bg" x1="0%" y1="0%" x2="100%" y2="100%">
      <stop offset="0%" style="stop-color:#2563eb;stop-opacity:1" />
      <stop offset="100%" style="stop-color:#1d4ed8;stop-opacity:1" />
    </linearGradient>
  </defs>
  <rect width="1024" height="1024" rx="200" ry="200" fill="url(#bg)"/>
  <text x="512" y="710" font-family="Helvetica,Arial,sans-serif" font-size="700" font-weight="bold" fill="white" text-anchor="middle">S</text>
</svg>'''

SIZES = [
    ('icon_16x16.png', 16),
    ('icon_16x16@2x.png', 32),
    ('icon_32x32.png', 32),
    ('icon_32x32@2x.png', 64),
    ('icon_128x128.png', 128),
    ('icon_128x128@2x.png', 256),
    ('icon_256x256.png', 256),
    ('icon_256x256@2x.png', 512),
    ('icon_512x512.png', 512),
    ('icon_512x512@2x.png', 1024),
]


def main():
    tmp = tempfile.mkdtemp()
    try:
        svg = os.path.join(tmp, 'icon.svg')
        with open(svg, 'w') as f:
            f.write(SVG)

        big_png = os.path.join(tmp, 'icon_1024.png')
        subprocess.run(['qlmanage', '-t', '-s', '1024', '-o', tmp, svg],
                       capture_output=True)
        os.rename(os.path.join(tmp, 'icon.svg.png'), big_png)

        iconset = os.path.join(tmp, 'StockPro.iconset')
        os.makedirs(iconset, exist_ok=True)

        for name, size in SIZES:
            out = os.path.join(iconset, name)
            subprocess.run(['sips', '-z', str(size), str(size), big_png,
                            '--out', out], capture_output=True)

        subprocess.run(['iconutil', '--convert', 'icns',
                        '--output', OUTPUT_ICNS, iconset], check=True)
        print(f'[OK] Icon created: {OUTPUT_ICNS}')
        kb = os.path.getsize(OUTPUT_ICNS) / 1024
        print(f'     Size: {kb:.0f} KB')
    finally:
        shutil.rmtree(tmp)


if __name__ == '__main__':
    main()
