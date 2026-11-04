#!/usr/bin/env python3
"""Checks each quote's wording against the public-domain text of its author's works.

Downloads source texts from Project Gutenberg into tools/.texts (git-ignored),
then reports for every quote:
  FOUND    the wording appears in the author's text (punctuation ignored)
  CLOSE    a near match was found; the actual wording is printed so the quote can be fixed
  COLLECTION  not in the author's own texts, but word-for-word in a quotation or proverb collection
  MISSING  no match: a translation, a letter, a work not on Gutenberg, or wrong wording
  SKIPPED  proverbs and authors with no texts listed in tools/sources.json

Usage: python3 tools/check_quotes.py [quotes.json] [--cache DIR] [--ids q0300-q0400]
"""
import argparse, csv, json, re, sys, time, urllib.request
from difflib import SequenceMatcher
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
ap = argparse.ArgumentParser()
ap.add_argument("quotes", nargs="?", default=str(ROOT / "shared" / "quotes.json"))
ap.add_argument("--cache", default=str(ROOT / "tools" / ".texts"))
ap.add_argument("--ids", help="range like q0259-q0400")
args = ap.parse_args()

cache = Path(args.cache)
cache.mkdir(parents=True, exist_ok=True)
sources = json.loads((ROOT / "tools" / "sources.json").read_text())
quotes = json.loads(Path(args.quotes).read_text(encoding="utf-8"))
if args.ids:
    lo, hi = args.ids.split("-")
    quotes = [q for q in quotes if lo <= q["id"] <= hi]

UA = {"User-Agent": "quote-widget-checker/1.0"}


def fetch(url, tries=5):
    for attempt in range(tries):
        try:
            with urllib.request.urlopen(urllib.request.Request(url, headers=UA), timeout=90) as r:
                return r.read()
        except Exception as e:  # the catalogue is often busy: wait and retry
            if attempt == tries - 1:
                raise
            time.sleep(5 * (attempt + 1))


CATALOG_URL = "https://www.gutenberg.org/cache/epub/feeds/pg_catalog.csv"
_catalog = None


def catalog():
    """Gutenberg's catalogue (about 20 MB), downloaded once into the cache."""
    global _catalog
    if _catalog is None:
        path = cache / "pg_catalog.csv"
        if not path.exists():
            path.write_bytes(fetch(CATALOG_URL))
        with path.open(encoding="utf-8") as f:
            _catalog = [r for r in csv.DictReader(f) if r["Type"] == "Text" and r["Language"] == "en"]
    return _catalog


def text_for(work):
    """Download the lowest-numbered English book matching [author text, title text]."""
    author, title = work
    slug = re.sub(r"[^a-z0-9]+", "-", f"{author} {title}".lower()).strip("-")
    path = cache / f"{slug}.txt"
    if path.exists():
        return path.read_text(encoding="utf-8", errors="ignore")
    matches = [r for r in catalog() if author.lower() in r["Authors"].lower() and title.lower() in r["Title"].lower()]
    if not matches:
        print(f"  not on Gutenberg: {author} / {title}", file=sys.stderr)
        path.write_text("")
        return ""
    book = min(matches, key=lambda r: int(r["Text#"]))
    num = book["Text#"]
    try:
        text = fetch(f"https://www.gutenberg.org/cache/epub/{num}/pg{num}.txt").decode("utf-8", errors="ignore")
    except Exception as e:
        print(f"  could not download #{num}: {e} (will retry next run)", file=sys.stderr)
        return ""
    path.write_text(text, encoding="utf-8")
    (cache / f"{slug}.title").write_text(f"{num} {book['Title']}")
    print(f"  downloaded #{num} {book['Title'][:70]}", file=sys.stderr)
    return text


def tokens(s):
    s = s.lower().replace("’", "'").replace("‘", "'").replace("—", " ").replace("–", " ").replace("_", " ")
    return re.findall(r"[a-z0-9']+", s)


corpus = {}
secondary = {}
for work in sources.get("_secondary", []):
    secondary[work[1]] = tokens(text_for(work))
for author, queries in sources.items():
    if author.startswith("_"):
        continue
    toks = []
    for work in queries:
        toks += tokens(text_for(work))
    corpus[author] = toks

counts = {"FOUND": 0, "CLOSE": 0, "COLLECTION": 0, "MISSING": 0, "SKIPPED": 0}
def best_match(words, toks):
    """Best share of the quote's words found in order near one spot in toks, and that spot's text."""
    index = {}
    for i, t in enumerate(toks):
        if t in words:
            index.setdefault(t, []).append(i)
    best, best_at = 0.0, None
    anchors = sorted((w for w in set(words) if w in index), key=lambda w: len(index[w]))[:3]
    for w in anchors:
        off = words.index(w)
        for i in index[w][:4000]:
            start = max(0, i - off - 3)
            window = toks[start:start + len(words) + 6]
            m = SequenceMatcher(None, words, window, autojunk=False)
            blocks = [b for b in m.get_matching_blocks() if b.size]
            ratio = sum(b.size for b in blocks) / len(words)
            if ratio > best:
                best, best_at = ratio, " ".join(window[blocks[0].b:blocks[-1].b + blocks[-1].size])
    return best, best_at


for q in quotes:
    words = tokens(q["text"])
    toks = corpus.get(q["author"])
    if not toks:
        status, note = "SKIPPED", q["source"]
    else:
        # anchor on the quote's rarest word, then compare windows around each occurrence
        index = {}
        for i, t in enumerate(toks):
            if t in words:
                index.setdefault(t, []).append(i)
        best, best_at = 0.0, None
        anchors = sorted((w for w in set(words) if w in index), key=lambda w: len(index[w]))[:3]
        for w in anchors:
            off = words.index(w)
            for i in index[w][:4000]:
                start = max(0, i - off - 3)
                window = toks[start:start + len(words) + 6]
                m = SequenceMatcher(None, words, window, autojunk=False)
                blocks = m.get_matching_blocks()
                hit = sum(b.size for b in blocks)
                ratio = hit / len(words)
                if ratio > best:
                    first = next(b for b in blocks if b.size)
                    last = [b for b in blocks if b.size][-1]
                    best, best_at = ratio, " ".join(window[first.b:last.b + last.size])
        if best >= 0.999:
            status, note = "FOUND", ""
        elif best >= 0.75:
            status, note = "CLOSE", f"{best:.0%} text: \"{best_at}\""
        else:
            status, note = "MISSING", f"best {best:.0%}"
    if status in ("MISSING", "SKIPPED"):
        for name, stoks in secondary.items():
            score, _ = best_match(words, stoks)
            if score >= 0.999:
                status, note = "COLLECTION", f"found in {name}"
                break
    counts[status] += 1
    if status != "FOUND":
        print(f"{status:8} {q['id']} {q['author']}: {q['text']}\n         {note}")

print(f"\n{counts}")
