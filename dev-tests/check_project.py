#!/usr/bin/env python3
"""Offline project sanity tests. Does not substitute Android SDK compilation."""
from pathlib import Path
from xml.etree import ElementTree as ET
root = Path(__file__).resolve().parents[1]
manifest = ET.parse(root / 'app/src/main/AndroidManifest.xml').getroot()
permissions = [p.attrib.get('{http://schemas.android.com/apk/res/android}name') for p in manifest.findall('uses-permission')]
assert permissions == ['android.permission.INTERNET'], permissions
files = list((root / 'app/src').rglob('*'))
assert not any(p.suffix.lower() in {'.pdf', '.doc', '.docx'} for p in files)
assert (root / '.github/workflows/bootstrap.yml').exists()
text = (root / 'app/src/main/java/com/ibnqayyim/library/BookCatalog.kt').read_text()
assert text.count('pdfUrl = "https://d1.islamhouse.com/') == 4
assert 'Избранное' not in text and 'Фаваид' not in text
source = (root / 'app/src/main/java/com/ibnqayyim/library/MainActivity.kt').read_text()
for feature in ('PdfPages.render', 'BookDownloader.download', 'bookmarkPages', 'Скачать', 'Источник'):
    assert feature in source, feature
print('PASS: permissions, bundled content, CI, catalog, reader wiring')
