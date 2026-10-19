#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
python3 scripts/verify-source.py
if command -v kotlinc >/dev/null 2>&1 && command -v java >/dev/null 2>&1; then
  T="$(mktemp -d)"; trap 'rm -rf "$T"' EXIT
  cat > "$T/Smoke.kt" <<'KOTLIN'
import com.minimalstudio.app.util.ExternalLinks
fun main() {
    check(ExternalLinks.safeHttpUrl("store.steampowered.com/app/123") == "https://store.steampowered.com/app/123")
    check(ExternalLinks.safeHttpUrl("https://example.org/x") == "https://example.org/x")
    check(ExternalLinks.safeHttpUrl("javascript:alert(1)") == null)
    check(ExternalLinks.safeHttpUrl("file:///tmp/a") == null)
    check(ExternalLinks.safeHttpUrl(" ") == null)
    println("PASS: Offline Kotlin link validation")
}
KOTLIN
  kotlinc app/src/main/java/com/minimalstudio/app/util/ExternalLinks.kt "$T/Smoke.kt" -include-runtime -d "$T/smoke.jar"
  java -jar "$T/smoke.jar"
else
  echo 'SKIP: local Kotlin JVM not installed'
fi
python3 - <<'PY'
import sqlite3
c=sqlite3.connect(':memory:')
c.execute('CREATE TABLE cards (id TEXT PRIMARY KEY NOT NULL, title TEXT NOT NULL)')
c.execute("INSERT INTO cards VALUES ('card-1', 'Тест')")
c.execute('ALTER TABLE cards ADD COLUMN deletedAt INTEGER NOT NULL DEFAULT 0')
assert c.execute('SELECT title, deletedAt FROM cards').fetchone()==('Тест',0)
print('PASS: SQLite migration SQL adds deletedAt without losing legacy rows')
PY
