import json
from pathlib import Path
import tempfile
import unittest

from update_notices import advance, update_files, version_key


class UpdateNoticesTest(unittest.TestCase):
    def test_beta_advances_latest_without_changing_recommended(self):
        promos = {"1.20.1-latest": "0.1.0-beta.5", "1.20.1-recommended": "0.0.9"}
        self.assertEqual(advance(promos, "1.20.1", "0.1.0-beta.6"),
                         {"1.20.1-latest": "0.1.0-beta.6", "1.20.1-recommended": "0.0.9"})

    def test_republishing_an_old_tag_does_not_roll_back_notices(self):
        promos = {"1.21.1-latest": "0.1.0-beta.10"}
        self.assertEqual(advance(promos, "1.21.1", "0.1.0-beta.6"), promos)
        self.assertEqual(advance(promos, "1.21.1", "0.1.0-beta.10"), promos)

    def test_stable_release_advances_both_channels(self):
        self.assertEqual(advance({"1.20.1-latest": "0.1.0-beta.10"}, "1.20.1", "0.1.0"),
                         {"1.20.1-latest": "0.1.0", "1.20.1-recommended": "0.1.0"})

    def test_numbered_qualifiers_are_compared_numerically(self):
        versions = ["0.1.0-alpha.1", "0.1.0-beta.5", "0.1.0-beta.10", "0.1.0-rc.1", "0.1.0", "0.2.0-beta.1"]
        self.assertEqual(sorted(reversed(versions), key=version_key), versions)

    def test_invalid_version_fails_before_writing(self):
        with self.assertRaises(ValueError):
            version_key("0.1.0-beta.6+forge.1.20.1")

    def test_files_are_loader_specific_and_have_no_changelogs(self):
        with tempfile.TemporaryDirectory(prefix="siegeworks-update-test-") as temporary:
            root = Path(temporary)
            (root / "stonecutter.properties.toml").write_text(
                'mod.id = "example"\n[forge."1.20.1"]\n[neoforge."1.21.1"]\n[fabric."1.20.1"]\n[fabric."1.21.1"]\n', encoding="utf-8")
            update_files(root, "0.1.0-beta.6")
            for loader, minecraft in (("forge", "1.20.1"), ("neoforge", "1.21.1")):
                path = root / "updates" / f"{loader}.json"
                data = json.loads(path.read_text(encoding="utf-8"))
                self.assertEqual(data, {
                    "homepage": f"https://modrinth.com/mod/example/versions?g={minecraft}&l={loader}",
                    "promos": {f"{minecraft}-latest": "0.1.0-beta.6"},
                })
            fabric = json.loads((root / "updates/fabric.json").read_text(encoding="utf-8"))
            self.assertEqual(fabric["promos"], {"1.20.1-latest": "0.1.0-beta.6", "1.21.1-latest": "0.1.0-beta.6"})
            update_files(root, "0.1.0-beta.5")
            self.assertEqual(json.loads((root / "updates/forge.json").read_text(encoding="utf-8"))["promos"],
                             {"1.20.1-latest": "0.1.0-beta.6"})


if __name__ == "__main__":
    unittest.main()
