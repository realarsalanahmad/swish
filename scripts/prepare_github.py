"""Build a source-only Swish archive and reject common local-secret artifacts."""

from pathlib import Path
import argparse
import hashlib
import json
import os
import zipfile

ROOT = Path(__file__).resolve().parents[1]
SKIP_DIRS = {".git", ".gradle", ".kotlin", ".idea", ".vscode", ".agent", "build", "node_modules", "__pycache__", "legacy", "diagnostics", "takeout", "graphify-out"}
SKIP_NAMES = {"local.properties", "key.properties", ".env", ".DS_Store"}
SKIP_SUFFIXES = {".apk", ".aab", ".jks", ".keystore", ".log", ".hprof", ".dmp"}
SKIP_PREFIXES = ("app/src/test/resources/explore/", "app/src/test/resources/shorts/")


def source_files():
    for directory, directories, names in os.walk(ROOT):
        directories[:] = sorted(name for name in directories if name not in SKIP_DIRS)
        for name in sorted(names):
            path = Path(directory) / name
            relative = path.relative_to(ROOT)
            if path.is_symlink() or relative.as_posix().startswith(SKIP_PREFIXES):
                continue
            if path.name in SKIP_NAMES or path.suffix.lower() in SKIP_SUFFIXES:
                continue
            if path.name.startswith(".env.") and path.name != ".env.example":
                continue
            yield path, relative


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--output", type=Path, required=True)
    arguments = parser.parse_args()
    output = arguments.output.resolve()
    if output == ROOT or ROOT in output.parents:
        raise SystemExit("Put the output archive outside the source repository.")
    files = list(source_files())
    required = {"LICENSE", "COPYRIGHT.md", "README.md", "privacy.md", "gradlew", "gradle/wrapper/gradle-wrapper.jar"}
    included = {relative.as_posix() for _, relative in files}
    missing = required - included
    if missing:
        raise SystemExit("Required source files missing: " + ", ".join(sorted(missing)))
    output.parent.mkdir(parents=True, exist_ok=True)
    with zipfile.ZipFile(output, "w", zipfile.ZIP_DEFLATED) as archive:
        for path, relative in files:
            archive.write(path, "Swish/" + relative.as_posix())
    digest = hashlib.sha256(output.read_bytes()).hexdigest()
    output.with_suffix(output.suffix + ".sha256").write_text(digest + "  " + output.name + "\n", encoding="utf-8")
    print(json.dumps({"archive": str(output), "files": len(files), "bytes": output.stat().st_size, "sha256": digest}))


if __name__ == "__main__":
    main()
