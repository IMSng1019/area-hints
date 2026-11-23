"""在中英同句处分别指定语种，给键名和品牌发音留出停连。"""


def segments(line):
    line = line.replace("“", "").replace("”", "")
    if " X" in line:
        return [("all_zh", line.replace(" X", "艾克斯"))]
    for term, pronunciation in (("Area Hints", "Area hints."), ("Xaero", "Zayro.")):
        if term in line:
            start, _, end = line.partition(term)
            parts = [("all_zh", start.strip()), ("en", pronunciation), ("all_zh", end.strip())]
            return [(lang, phrase) for lang, phrase in parts if phrase.strip("，。！？；、… ")]
    return [("zh", line)]


def cache_key(parts):
    import hashlib
    name = parts[0][1] if len(parts) == 1 else repr(parts)
    return hashlib.sha256(name.encode()).hexdigest()[:10]
