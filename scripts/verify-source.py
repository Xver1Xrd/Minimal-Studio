#!/usr/bin/env python3
"""Dependency-free structural checks. NOT a substitute for Android compilation/tests."""
from pathlib import Path
from xml.etree import ElementTree as ET
import re

root = Path(__file__).resolve().parents[1]
manifest = root / 'app/src/main/AndroidManifest.xml'
android = '{http://schemas.android.com/apk/res/android}'
m = ET.parse(manifest).getroot()
perms = [p.get(android + 'name') for p in m.findall('uses-permission')]
assert 'android.permission.INTERNET' not in perms, 'Offline app must not request Internet access'
assert 'android.permission.READ_EXTERNAL_STORAGE' not in perms
provider = m.find('application/provider')
assert provider is not None and provider.get(android + 'exported') == 'false'

build = (root / 'app/build.gradle.kts').read_text()
assert 'versionCode = 2' in build and 'versionName = "1.1.0"' in build
assert 'compose-bom:2026.06.00' in build
assert 'isMinifyEnabled = true' in build
assert 'targetSdk = 35' in build

db = (root / 'app/src/main/java/com/minimalstudio/app/data/StudioDatabase.kt').read_text()
repo = (root / 'app/src/main/java/com/minimalstudio/app/data/StudioRepository.kt').read_text()
assert 'version = 2' in db and 'MIGRATION_1_2' in db
assert 'fallbackToDestructiveMigration' not in db
assert 'database.withTransaction' in repo and 'copyLimited' in repo
assert 'suspend fun restoreCard' in repo and 'suspend fun emptyTrash' in repo

source_files = list((root / 'app/src/main/java').rglob('*.kt'))
assert len(source_files) >= 16
for path in source_files:
    text = path.read_text()
    assert text.startswith('package '), path
    assert '\x00' not in text, path
for path in (root / 'app/src/main').rglob('*.xml'):
    ET.parse(path)
print(f'PASS: {len(source_files)} Kotlin sources, Android manifests/resources, offline permissions, DB migration, backup and release settings')
