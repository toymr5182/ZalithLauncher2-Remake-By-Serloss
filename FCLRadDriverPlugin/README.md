# FCL RADV Xclipse 540 Driver Plugin

An Android APK that FCL (Fold Craft Launcher) recognizes as a **Vulkan driver plugin**.
It packages the community RADV driver for Samsung Xclipse GPUs.

This is not a renderer plugin. FCL renderer plugins supply OpenGL libraries. RDNA is
reached through Vulkan, so the correct FCL plugin type is a driver plugin. Pair it with
FCL's built-in **Zink** renderer (OpenGL on Vulkan).

## Contents

| Item | Value |
|---|---|
| Driver | RADV Xclipse (Mesa 27.0.0-devel) |
| Upstream | [JimVulkan/radv-xclipse](https://github.com/JimVulkan/radv-xclipse) v1.8.0 |
| Driver library | `vulkan.radeon.so` (arm64-v8a only) |
| GPU | Samsung Xclipse 540 (Exynos 1580, Galaxy A56 / Tab S10 FE) |
| Minimum Android | 14 (API 34) |
| FCL metadata | `fclPlugin`, `driver`, `driver_lib`, `driver_gpu=any` |

`driver_gpu=any` is required because FCL only knows the `adreno` and `mali` families.
Xclipse is neither, so the plugin must opt out of that gate.

## Usage

1. Install the APK.
2. In FCL open the instance: **Version settings -> Driver** and select
   `RADV Xclipse 540 (RDNA, Mesa 27)`.
3. Set **Renderer** to **Zink**.
4. Clear the instance shader cache after switching drivers.
5. Do not enable a "prefer system driver" option; that bypasses custom drivers.

The first launch performs FCL's Vulkan capability check. If the driver fails to load,
FCL falls back to the system Vulkan driver.

## Warning

This driver is experimental. Its upstream project reports that a GPU hang can freeze the
device and that the phone may reboot as a safety reset. Reboot the phone after a hang
before starting another game. Switch the driver back to the system Vulkan driver if a
game crashes. Ray tracing is not available on the Xclipse 540.

## Build

```sh
scripts/fetch-radv.sh
./gradlew assembleDebug
```

The APK is written to `app/build/outputs/apk/debug/app-debug.apk`. The fetch script pins
the upstream archive by SHA-256:

```
0545b1b24817cc0ed4a9e4fc440a7080cbaf69a9ce5d1e4bb22b0e697129c223
```

The GitHub Actions workflow `.github/workflows/build-radv-plugin.yml` runs the same steps
and uploads the APK as an artifact.

## Licensing

The driver and Mesa components are distributed under their upstream licenses. The build
copies `NOTICE.txt` from the upstream archive into `app/src/main/assets/` so it ships
inside the APK. The Xclipse changes are MIT licensed; Mesa files keep their own licenses.
This project is not affiliated with Samsung, AMD, Mesa, FCL, or the RADV Xclipse authors.

## Known limitations

- The Exynos 1580 profile in this repository's Zalith fork forces the system Vulkan
  driver and its native driver loader only understands the fixed Turnip library name.
  This APK targets FCL, which supports `driver_lib` and custom drivers. The fork needs a
  small patch before it can use this plugin.
- Zink on RADV Xclipse is untested upstream. Only the driver itself has been tested.
