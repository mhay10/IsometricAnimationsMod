import re
import pathlib

root = pathlib.Path("src")
java_files = list(root.rglob("*.java"))

unused = []
for f in java_files:
    text = f.read_text(encoding="utf-8")
    imports = re.findall(r"^import\s+([^;]+);", text, flags=re.M)
    for imp in imports:
        # get simple name
        if imp.endswith(".*"):
            parts = imp.split(".")
            simple = parts[-2]
        else:
            simple = imp.split(".")[-1]
        occurrences = len(re.findall(r"\b" + re.escape(simple) + r"\b", text))
        if occurrences <= 1:
            unused.append((str(f), imp, occurrences))

if not unused:
    print("No likely-unused imports found.")
else:
    for f, imp, count in unused:
        print(f + " -> " + imp + " (occurrences=" + str(count) + ")")

print(
    "\nNote: This is a heuristic (simple-name occurrences). Please verify before removing."
)
