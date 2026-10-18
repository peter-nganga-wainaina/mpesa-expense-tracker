#!/usr/bin/env python3
"""Reports test changes made during one Claude turn and flags ones that may weaken existing tests.

  snapshot  (UserPromptSubmit) copies the test sources aside, inside .git so nothing is tracked.
  report    (Stop) compares the tests against that copy. New tests get a short note; removed or
            changed assertion lines, deleted test functions and new @Ignores block the stop once,
            so Claude has to explain or revert them (see "Never delete or weaken a test").
"""
import difflib
import json
import re
import shutil
import subprocess
import sys
from pathlib import Path

TEST_ROOT = Path("app/src/test")
ASSERTION = re.compile(r"\b(assert\w*|fail|check\w*|require\w*|expect\w*)\s*[({]|\bshould\w*\b|\bexpected\w*\s*=")
TEST_FUN = re.compile(r"@Test\b[\s\S]*?\bfun\s+(`[^`]+`|\w+)\s*\(")
IGNORE = re.compile(r"@Ignore\b")


def repo_root() -> Path:
    # The script lives in <repo>/.claude/hooks/, so this works whatever directory the hook runs from.
    return Path(__file__).resolve().parents[2]


def snapshot_dir(root: Path) -> Path:
    out = subprocess.run(["git", "rev-parse", "--absolute-git-dir"], capture_output=True, text=True, check=True, cwd=root)
    return Path(out.stdout.strip()) / "claude-test-snapshot"


def kotlin_files(base: Path) -> dict[str, list[str]]:
    if not base.is_dir():
        return {}
    return {str(TEST_ROOT / p.relative_to(base)): p.read_text(encoding="utf-8").splitlines() for p in sorted(base.rglob("*.kt"))}


def snapshot(root: Path) -> None:
    target = snapshot_dir(root)
    shutil.rmtree(target, ignore_errors=True)
    if (root / TEST_ROOT).is_dir():
        shutil.copytree(root / TEST_ROOT, target)


def test_names(lines: list[str]) -> list[str]:
    return TEST_FUN.findall("\n".join(lines))


def removed_assertions(path: str, before: list[str], after: list[str]) -> list[str]:
    """Assertion lines that were deleted or rewritten, each with what replaced it (if anything)."""
    findings = []
    matcher = difflib.SequenceMatcher(a=before, b=after, autojunk=False)
    for tag, i1, i2, j1, j2 in matcher.get_opcodes():
        if tag not in ("replace", "delete"):
            continue
        replacement = [line.strip() for line in after[j1:j2]]
        for offset, line in enumerate(before[i1:i2]):
            if ASSERTION.search(line) and line.strip() not in replacement:
                # Show only the most similar new line, not every line added alongside it.
                closest = difflib.get_close_matches(line.strip(), replacement, n=1, cutoff=0.5)
                findings.append(f"{path}:{i1 + offset + 1} removed `{line.strip()}`"
                                + (f" → now `{closest[0]}`" if closest else ""))
    return findings


def compare(before: dict[str, list[str]], after: dict[str, list[str]]) -> tuple[list[str], list[str]]:
    """Returns (notes about additions, flags about possible weakening)."""
    notes, flags = [], []
    for path in sorted(before.keys() - after.keys()):
        flags.append(f"{path}: test file deleted ({len(test_names(before[path]))} tests)")
    for path in sorted(after.keys() - before.keys()):
        notes.append(f"{path}: new file, {len(test_names(after[path]))} tests")
    for path in sorted(before.keys() & after.keys()):
        old, new = before[path], after[path]
        if old == new:
            continue
        old_names, new_names = test_names(old), test_names(new)
        added = [n for n in new_names if n not in old_names]
        gone = [n for n in old_names if n not in new_names]
        if added:
            notes.append(f"{path}: {len(added)} new test(s): " + ", ".join(added))
        for name in gone:
            flags.append(f"{path}: test removed or renamed: {name}")
        new_ignores = len(IGNORE.findall("\n".join(new))) - len(IGNORE.findall("\n".join(old)))
        if new_ignores > 0:
            flags.append(f"{path}: {new_ignores} new @Ignore")
        flags.extend(removed_assertions(path, old, new))
    return notes, flags


def report(root: Path, hook_input: dict) -> int:
    snap = snapshot_dir(root)
    if not snap.is_dir():
        return 0
    notes, flags = compare(kotlin_files(snap), kotlin_files(root / TEST_ROOT))
    if not notes and not flags:
        return 0
    summary = "\n".join(["Test changes this turn:"] + [f"  + {n}" for n in notes] + [f"  ⚠ {f}" for f in flags])
    output = {"systemMessage": summary}
    # Block only once per turn: when Claude has already been sent back, let it stop.
    if flags and not hook_input.get("stop_hook_active"):
        output["decision"] = "block"
        output["reason"] = (
            "⚠ Existing test lines changed this turn:\n" + "\n".join(f"- {f}" for f in flags)
            + "\nFor each one, explain to the user why it doesn't weaken the test (e.g. a rename or a"
            " reformat), or revert it. Changing an expected value to match the code needs the user's agreement."
        )
    print(json.dumps(output, ensure_ascii=False))
    return 0


def main() -> int:
    mode = sys.argv[1] if len(sys.argv) > 1 else ""
    hook_input = json.loads(sys.stdin.read() or "{}")
    root = repo_root()
    if mode == "snapshot":
        snapshot(root)
        return 0
    if mode == "report":
        return report(root, hook_input)
    print(f"usage: {sys.argv[0]} snapshot|report", file=sys.stderr)
    return 1


if __name__ == "__main__":
    sys.exit(main())
