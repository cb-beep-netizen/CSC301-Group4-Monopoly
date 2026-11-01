"""
check_constraints.py - static check of the CSC301 constraints on code/*.java
 1. Imports: only java.util.ArrayList/Scanner/Random/Collections and java.io.*
 2. No banned collections, java.util.LinkedList, Comparator or streams anywhere
 3. The 8 required recursive methods contain no for/while/do and call themselves
 4. No '% size' style index arithmetic
Usage: python check_constraints.py <code folder>
AI log: Conversation 01, Prompt 01.13 (Claude).
"""
import re, sys, os
folder = sys.argv[1] if len(sys.argv) > 1 else "code"
ALLOWED_UTIL = {"ArrayList", "Scanner", "Random", "Collections"}
BANNED = ["ArrayDeque", "Stack", "Vector", "PriorityQueue", "Deque", "HashMap", "TreeMap", "HashSet",
          r"\bLinkedList<", "java.util.LinkedList", "LinkedHashMap", "Hashtable", "TreeSet", "Comparator",
          ".stream(", "java.util.stream", r"\bIterator<", r"\bList<", r"\bMap<", r"\bSet<"]
REQUIRED = {"Board.java": ["step", "ownsGroup", "findByName", "findNearest"],
            "Game.java": ["takeTurn", "landOn"],
            "TransitSquare.java": ["transitRent"], "Player.java": ["netWorth"]}
problems = []

def strip(src):
    src = re.sub(r"/\*.*?\*/", "", src, flags=re.S)
    src = re.sub(r"//[^\n]*", "", src)
    return re.sub(r'"(\\.|[^"\\])*"', '""', src)

def body(src, name):
    m = re.search(r"\b(public|private|protected)[^;{]*\b" + name + r"\s*\([^)]*\)\s*\{", src)
    if not m: return None
    i, depth = m.end(), 1
    while depth:
        depth += {"{": 1, "}": -1}.get(src[i], 0); i += 1
    return src[m.end():i - 1]

for fn in sorted(os.listdir(folder)):
    if not fn.endswith(".java"): continue
    raw = open(os.path.join(folder, fn), encoding="utf-8").read()
    src = strip(raw)
    for imp in re.findall(r"^\s*import\s+([\w.*]+)\s*;", raw, re.M):
        if imp.startswith("java.util."):
            if imp.split(".")[-1] not in ALLOWED_UTIL: problems.append(f"{fn}: import {imp} not allowed")
        elif not imp.startswith("java.io."): problems.append(f"{fn}: import {imp} (check it is allowed)")
    code_no_list = src.replace("ArrayList<", "AL<")
    for b in BANNED:
        pat = b if b.startswith("\\b") else re.escape(b)
        if re.search(pat, code_no_list): problems.append(f"{fn}: banned usage '{b.replace(chr(92)+'b','')}'")
    if re.search(r"%\s*\w*\.?size\(\)|%\s*size\b", src): problems.append(f"{fn}: '% size' index arithmetic")
    for m in REQUIRED.get(fn, []):
        b = body(src, m)
        if b is None: problems.append(f"{fn}: required method {m} not found"); continue
        if re.search(r"\b(for|while|do)\b", b): problems.append(f"{fn}: {m} contains a loop")
        if not re.search(r"\b" + m + r"\s*\(", b): problems.append(f"{fn}: {m} does not call itself")
        print(f"OK   {fn}:{m} - recursive, no loops" if not re.search(r"\b(for|while|do)\b", b) else "")
print()
print("CONSTRAINT CHECK:", "PASS" if not problems else "FAIL")
for p in problems: print("  -", p)
sys.exit(1 if problems else 0)
