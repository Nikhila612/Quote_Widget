#!/usr/bin/env python3
"""Checks shared/quotes.json against the content rules in the plan.

Run from the repo root:  python3 tools/validate_quotes.py
Exits non-zero and lists every problem if any rule is broken.
"""
import json
import re
import sys
from pathlib import Path

ALLOWED_TAGS = {"happiness", "courage", "calm", "growth", "kindness", "gratitude"}
MAX_WORDS = 25
MAX_CHARS = 160
BLOCKED_WORDS = {"god", "lord", "jesus", "allah", "heaven's"}  # tone rule: no religion

path = Path(__file__).resolve().parent.parent / "shared" / "quotes.json"
quotes = json.loads(path.read_text(encoding="utf-8"))
problems = []
seen_ids, seen_texts = set(), {}


def norm(text):
    return re.sub(r"[^a-z0-9 ]", "", text.lower()).strip()


for i, q in enumerate(quotes):
    where = f"#{i + 1} {q.get('id', '?')}"
    for key in ("id", "text", "author", "source", "tags"):
        if not q.get(key):
            problems.append(f"{where}: missing '{key}'")
    qid, text = q.get("id", ""), q.get("text", "")
    if not re.fullmatch(r"q\d{4}", qid):
        problems.append(f"{where}: id must look like q0001")
    if qid in seen_ids:
        problems.append(f"{where}: duplicate id")
    seen_ids.add(qid)
    if norm(text) in seen_texts:
        problems.append(f"{where}: same text as {seen_texts[norm(text)]}")
    seen_texts[norm(text)] = qid
    words = len(text.split())
    if words > MAX_WORDS:
        problems.append(f"{where}: {words} words (max {MAX_WORDS})")
    if len(text) > MAX_CHARS:
        problems.append(f"{where}: {len(text)} characters (max {MAX_CHARS})")
    if text != text.strip() or "  " in text:
        problems.append(f"{where}: extra spaces")
    bad_tags = set(q.get("tags", [])) - ALLOWED_TAGS
    if bad_tags:
        problems.append(f"{where}: unknown tags {sorted(bad_tags)}")
    if set(norm(text).split()) & BLOCKED_WORDS:
        problems.append(f"{where}: religious wording")

ids = [q.get("id") for q in quotes]
if ids != sorted(ids):
    problems.append("ids are not in order")

if problems:
    print(f"{len(problems)} problem(s) in {path.name}:")
    print("\n".join(f"  - {p}" for p in problems))
    sys.exit(1)

authors = {q["author"] for q in quotes}
tags = {t: sum(t in q["tags"] for q in quotes) for t in sorted(ALLOWED_TAGS)}
print(f"OK: {len(quotes)} quotes from {len(authors)} authors. Tags: {tags}")
