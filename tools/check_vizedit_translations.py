"""检查全部语言键集，以及可视化编辑器的静态引用和格式占位符。"""
import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
LANGUAGE_DIR = ROOT / "src/main/resources/assets/areas-hint/lang"


def unique_keys(pairs):
    result = {}
    for key, value in pairs:
        if key in result:
            raise ValueError("Duplicate translation key: " + key)
        result[key] = value
    return result


languages = {
    path.stem: json.loads(path.read_text(encoding="utf-8-sig"), object_pairs_hook=unique_keys)
    for path in LANGUAGE_DIR.glob("*.json")
}
assert len(languages) == 11, "Expected all 11 languages"
base = languages["en_us"]
expected = {key for key in base if "vizedit" in key}
placeholders = re.compile(r"%(?:\d+\$)?[sdif]|\{\d+\}")
for language, values in languages.items():
    assert set(values) == set(base), (language, "Translation keys differ")
    for key in expected:
        assert values[key].strip(), (language, key, "Empty translation")
        assert sorted(placeholders.findall(values[key])) == sorted(placeholders.findall(base[key])), (language, key)
    print(f"{language}: {len(values)} keys, {len(expected)} vizedit keys, placeholders OK")

# 动态工具名、操作名和错误后缀由对应键集覆盖；此处额外检查源码内直接写出的完整键。
used = set()
for folder in ["src/main/java", "src/client/java"]:
    for path in (ROOT / folder).rglob("*.java"):
        used.update(re.findall(r'"((?:vizedit\.|help\.command\.vizedit|approval\.type\.vizedit)[a-z_.]*)"', path.read_text(encoding="utf-8-sig")))
missing = {key for key in used if not key.endswith(".") and key not in expected}
assert not missing, missing
print("All language keys, vizedit placeholders and static references match.")
