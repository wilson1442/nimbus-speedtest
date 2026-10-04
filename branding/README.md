# Brand assets

`nimbus-logo-original.png` — the supplied source artwork (1254×1254, cloud + speedometer
mark over the "Nimbus / SPEED TEST" wordmark, on the light plate #FCFDFC). This is the
master file; everything shipped is derived from it.

Files the app actually uses (single source of truth — do not duplicate the exports here,
edit these):

| Asset | Path |
|---|---|
| In-app mark (dashboard top bar + screen headers) | `src/app/src/main/res/drawable-xxxhdpi/nimbus_logo.png` |
| Adaptive launcher icon (API 26+) | `src/app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml` + `mipmap-*/ic_launcher_foreground.png` |
| Legacy launcher icon (API 24/25) | `src/app/src/main/res/mipmap-*/ic_launcher.png`, `ic_launcher_round.png` |
| Android TV banner (320×180) | `src/app/src/main/res/drawable/tv_banner.png` |

Note: the artwork **cannot be cut to transparency** — its cloud fades into the same
near-white as its plate across a wide band, so any colour-key/flood-fill punches out the
dial interior and the white speed-lines (see `handoff/alpha-diagnostic.png`). Derived
assets therefore keep the plate, recoloured to sit on whatever background they're used
with (the in-app mark uses the app background #F4F6F3; the adaptive icon's background
layer is the plate colour).
