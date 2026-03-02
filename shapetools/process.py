import json
import math
import sys
from pathlib import Path

from svgpathtools import svg2paths

from config import TEMPLATES_DIR, RAW_SAMPLE_POINTS, FLIP_Y, N, SQUARE_SIZE


def sample_raw_points(svg_path: Path) -> list[list[float]]:
    paths, _ = svg2paths(str(svg_path))
    if not paths:
        return []

    lengths = [p.length() for p in paths]
    total_length = sum(lengths)
    if total_length < 1e-6:
        return []

    points = []
    for i in range(RAW_SAMPLE_POINTS):
        t_global = i / (RAW_SAMPLE_POINTS - 1) * total_length
        accumulated = 0.0
        for path, length in zip(paths, lengths):
            if accumulated + length >= t_global or path is paths[-1]:
                t_local = (t_global - accumulated) / length if length > 1e-6 else 0.0
                t_local = max(0.0, min(1.0, t_local))
                point = path.point(t_local)
                x = point.real
                y = -point.imag if FLIP_Y else point.imag
                points.append([x, y])
                break
            accumulated += length

    return points


def path_length(points):
    length = 0.0
    for i in range(1, len(points)):
        dx = points[i][0] - points[i - 1][0]
        dy = points[i][1] - points[i - 1][1]
        length += math.sqrt(dx * dx + dy * dy)
    return length


def resample(points, n):
    if len(points) < 2:
        p = points[0] if points else [0.0, 0.0]
        return [list(p) for _ in range(n)]

    total = path_length(points)
    interval = total / (n - 1)

    new_points = [list(points[0])]
    accumulated = 0.0
    i = 1

    while i < len(points):
        dx = points[i][0] - points[i - 1][0]
        dy = points[i][1] - points[i - 1][1]
        seg_len = math.sqrt(dx * dx + dy * dy)

        if accumulated + seg_len >= interval:
            t = (interval - accumulated) / seg_len
            qx = points[i - 1][0] + t * dx
            qy = points[i - 1][1] + t * dy
            new_points.append([qx, qy])
            points = [[qx, qy]] + points[i:]
            i = 1
            accumulated = 0.0
        else:
            accumulated += seg_len
            i += 1

    while len(new_points) < n:
        new_points.append(list(new_points[-1]))

    return new_points[:n]


def scale_to(points):
    min_x = min(p[0] for p in points)
    max_x = max(p[0] for p in points)
    min_y = min(p[1] for p in points)
    max_y = max(p[1] for p in points)
    w = max_x - min_x
    h = max_y - min_y
    if w < 1e-6:
        w = 1.0
    if h < 1e-6:
        h = 1.0
    return [[p[0] * (SQUARE_SIZE / w), p[1] * (SQUARE_SIZE / h)] for p in points]


def translate_to(points):
    cx = sum(p[0] for p in points) / len(points)
    cy = sum(p[1] for p in points) / len(points)
    return [[p[0] - cx, p[1] - cy] for p in points]


def is_closed(points):
    if len(points) < 2:
        return False
    dx = points[0][0] - points[-1][0]
    dy = points[0][1] - points[-1][1]
    close_dist = math.sqrt(dx * dx + dy * dy)
    total = path_length(points)
    return close_dist < total * 0.15


def canonicalize_start(points):
    if not is_closed(points):
        return points
    cx = sum(p[0] for p in points) / len(points)
    cy = sum(p[1] for p in points) / len(points)
    best_idx = 0
    best_angle_dist = float("inf")
    for i, p in enumerate(points):
        angle = abs(math.atan2(p[1] - cy, p[0] - cx))
        if angle < best_angle_dist:
            best_angle_dist = angle
            best_idx = i
    if best_idx == 0:
        return points
    return points[best_idx:] + points[:best_idx]


def preprocess(points):
    resampled = resample(points, N)
    scaled = scale_to(resampled)
    translated = translate_to(scaled)
    return canonicalize_start(translated)


def process_all():
    templates_dir = TEMPLATES_DIR.resolve()
    if not templates_dir.exists():
        print(f"templates directory not found: {templates_dir}")
        sys.exit(1)

    svgs = sorted(templates_dir.glob("*.svg"))
    if not svgs:
        print(f"no SVG files found in {templates_dir}")
        return

    processed = 0
    skipped = 0

    for svg_path in svgs:
        name = svg_path.stem
        template_path = templates_dir / f"{name}.json"

        if template_path.exists():
            print(f"  skip: {name} (already processed)")
            skipped += 1
            continue

        raw = sample_raw_points(svg_path)
        if not raw:
            print(f"  skip: {name} (no valid paths in SVG)")
            skipped += 1
            continue

        points = preprocess(raw)
        flat = [coord for point in points for coord in point]

        data = {"ShapeId": name, "Points": flat, "IsTraining": False}
        with open(template_path, "w") as f:
            json.dump(data, f, indent=2)

        print(f"  done: {name} ({len(flat)} floats)")
        processed += 1

    print(f"\n{processed} processed, {skipped} skipped, {len(svgs)} total SVGs")


if __name__ == "__main__":
    process_all()
