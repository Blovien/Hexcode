# shapetools

This converts SVG files into preprocessed $1 template JSON files that the shape detector uses directly.

Drop an SVG into the Shapes/ directory (where the PNGs live), run this script, and it spits out a `.template.json` next to the SVG. The server picks those up automatically on boot.

## setup

You need Python 3.10+ and pip.

```
cd shapetools
pip install -r requirements.txt
```

That's it.

## usage

```
python process.py
```

It scans `src/main/resources/Server/Hexcode/Shapes/` for SVG files. If an SVG doesn't have a matching `.template.json` next to it, it processes the SVG and creates one. If the template already exists, it skips it.

If you want to regenerate a template, just delete the `.template.json` file and run it again.

## what it actually does

It reads the SVG path data, walks along the curve, and samples 256 raw points. Then it runs those through the same preprocessing pipeline as the $1-Fixed detector: resample to 64 points, scale to a 250x250 bounding box, and translate the centroid to the origin. The output is ready to use — the detector loads it and compares against player input directly without any extra processing.

The y-axis gets flipped by default because SVGs use y-down and the game uses y-up (pitch). You can toggle this in `config.py` if your SVGs are already in the right orientation.

## config

Everything is in `config.py`:

- `SHAPES_DIR` — where to look for SVGs (defaults to the Shapes/ resource directory)
- `RAW_SAMPLE_POINTS` — how many raw points to sample from the SVG before preprocessing (default 256)
- `FLIP_Y` — flip the y-axis (default True)
- `N` — resample count, must match the detector (default 64)
- `SQUARE_SIZE` — bounding box size, must match the detector (default 250)
