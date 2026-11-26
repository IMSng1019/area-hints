"""把方块提瓦特域名的 XZ 多边形转换为宣传片的 SVG 描边路径。"""

import argparse
import json
import math
from pathlib import Path


def simplify(points, tolerance=0.4):
    """按画布像素控制等比例投影后的折线误差。"""
    if len(points) <= 2:
        return points
    (x, y), (end_x, end_y) = points[0], points[-1]
    dx, dy = end_x - x, end_y - y
    length_sq = dx * dx + dy * dy
    maximum, split = 0, 0
    for index, (px, py) in enumerate(points[1:-1], start=1):
        along = ((px - x) * dx + (py - y) * dy) / length_sq if length_sq else 0
        along = min(1, max(0, along))
        distance = math.hypot(px - x - along * dx, py - y - along * dy)
        if distance > maximum:
            maximum, split = distance, index
    if maximum <= tolerance:
        return [points[0], points[-1]]
    return simplify(points[:split + 1], tolerance)[:-1] + simplify(points[split:], tolerance)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("world_json", type=Path)
    options = parser.parse_args()
    records = json.loads(options.world_json.read_text(encoding="utf-8-sig"))
    vertices = [v for record in records for v in record["vertices"]]
    x_min, x_max = (ext(v["x"] for v in vertices) for ext in (min, max))
    z_min, z_max = (ext(v["z"] for v in vertices) for ext in (min, max))
    width, margin = 1280, 12
    scale = (width - 2 * margin) / (x_max - x_min)
    height = math.ceil((z_max - z_min) * scale + 2 * margin)
    areas = []
    for record in sorted(records, key=lambda item: item["level"]):
        # BlueMap 俯视图中东向右、南向下，X 与 Z 共用一个比例。
        points = [(margin + (v["x"] - x_min) * scale, margin + (v["z"] - z_min) * scale)
                  for v in record["vertices"]]
        ring = simplify(points + [points[0]])[:-1]
        if len(set(ring)) < 3:
            ring = points
        path = "M" + "L".join(f"{x:.1f},{y:.1f}" for x, y in ring) + "Z"
        area = {key: record[key] for key in ("name", "level", "color")} | {"d": path}
        if record["name"] == "风起地":
            # 实景地名的引线接在第一枚原始边界顶点。
            area["labelAnchor"] = [round(coord, 1) for coord in points[0]]
        areas.append(area)
    output = Path(__file__).resolve().parents[1] / "src/performance-map.json"
    bounds = {"minX": x_min, "maxX": x_max, "minZ": z_min, "maxZ": z_max}
    output.write_text(json.dumps({"width": width, "height": height, "margin": margin,
                                  "bounds": bounds, "areas": areas},
                                 ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


if __name__ == "__main__":
    main()
