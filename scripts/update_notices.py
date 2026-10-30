"""Advance the loader update files after a successful release upload."""

import argparse
import json
from pathlib import Path
import re
import tomllib


def version_key(version: str) -> tuple:
    match = re.fullmatch(r"(\d+)\.(\d+)\.(\d+)(?:-(alpha|beta|rc)\.(\d+))?", version)
    if match is None:
        raise ValueError(f"Unsupported release version: {version}")
    major, minor, patch, qualifier, number = match.groups()
    stage = {"alpha": 0, "beta": 1, "rc": 2, None: 3}[qualifier]
    return int(major), int(minor), int(patch), stage, int(number or 0)


def advance(promos: dict, minecraft_version: str, version: str) -> dict:
    updated = dict(promos)
    newest = version_key(version)
    names = [f"{minecraft_version}-latest"]
    if "-" not in version:
        names.append(f"{minecraft_version}-recommended")
    for name in names:
        if name not in updated or newest > version_key(updated[name]):
            updated[name] = version
    return updated


def update_files(root: Path, version: str) -> None:
    version_key(version)
    properties = tomllib.loads((root / "stonecutter.properties.toml").read_text(encoding="utf-8"))
    project = properties["mod"]["id"]
    directory = root / "updates"
    directory.mkdir(exist_ok=True)
    for loader in ("forge", "neoforge", "fabric"):
        path = directory / f"{loader}.json"
        data = json.loads(path.read_text(encoding="utf-8")) if path.exists() else {}
        promos = data.get("promos", {})
        for minecraft_version in properties[loader]:
            promos = advance(promos, minecraft_version, version)
        minecraft_version = next(iter(properties[loader]))
        data = {
            "homepage": data.get("homepage")
            or f"https://modrinth.com/mod/{project}/versions?g={minecraft_version}&l={loader}",
            "promos": promos,
        }
        content = json.dumps(data, indent=2) + "\n"
        if not path.exists() or path.read_text(encoding="utf-8") != content:
            path.write_text(content, encoding="utf-8")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("version")
    parser.add_argument("--root", type=Path, default=Path(__file__).resolve().parents[1])
    args = parser.parse_args()
    update_files(args.root.resolve(), args.version)
