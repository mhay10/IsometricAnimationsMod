# Isometric Animations Mod

A Minecraft mod that captures sections of your world and renders them as smooth isometric animations.

[Example Render](example_render.mp4)

## Features

- Capture any rectangular area of your Minecraft world
- Preview and adjust your camera angle before rendering
- Generate high-quality isometric animations with customizable settings
- Export animations as MP4 videos or a series of TGA images

## Dependencies

| Dependency                                | Notes                                                      |
|-------------------------------------------|------------------------------------------------------------|
| [Fabric API](https://fabricmc.net/)       | Required                                                   |
| [Sodium](https://modrinth.com/mod/sodium) | ⚠️ Currently not fully working! Output animation is glichy |

### 🚨 NOTICE 🚨

**This mod is extremely file I/O intensive!** Every frame of the animation is tobe saved to disk
during rendering, so the speed of your storage device has a significant impact on performance.
For the best experience, run Minecraft from an _NVMe SSD_. Rendering on a mechanical drive may result
in longer render times, stuttering, or incomplete animations.

## Installation``

1. Install [Fabric Loader](https://fabricmc.net/use/) and [Fabric API](https://fabricmc.net/use/)
2. Download the latest release of this mod from
   the [Releases](https://github.com/mhay10/IsometricAnimationsMod/releases) page
3. Place the downloaded JAR file in your Minecraft `mods` folder
4. Launch Minecraft and verify that the mod is loaded correctly

## Usage

All commands use the `/isoanimations` prefix.

### Commands

---

#### `clear`

Clears the current animation state and resets any pending render.

```
/isoanimations clear
```

---

#### `testpos`

Previews the camera position and angle for the selected region **without** creating an animation.
Use this to adjust your `pitch` and `yaw` before doing a full render.

```
/isoanimations testpos <pos1> <pos2> <scale> <pitch> <yaw>
```

---

#### `create`

Captures the selected region and generates an animation.

```
/isoanimations create <pos1> <pos2> <scale> <pitch> <yaw> <duration>
```

---

### Parameters

| Parameter  |      Type      |   Range   | Description                                            |
|------------|:--------------:|:---------:|--------------------------------------------------------|
| `pos1`     | Block Position |     -     | First corner of the capture region                     |
| `pos2`     | Block Position |     -     | Opposite corner of the capture region                  |
| `scale`    |    Integer     | 100 - 500 | Zoom/scale factor of the render (bigger = farther out) |
| `pitch`    |    Integer     |  0 - 360  | Horizontal rotation angle around capture region        |
| `yaw`      |    Integer     | -90 - 90  | Vertical tilt angle around capture region              |
| `duration` |    Decimal     |   > 0.0   | Length of animation in seconds (`create` only)         |

## Output Files

Generated animations and frames are saved in `.minecraft/isoanimations` using the following structure:

```
.minecraft/
└── isoanimations/
    ├── frames/
    │   ├── frame_XXXXXX.tga
    │   └── ...
    └── animations/
        ├── animation_YYYY_MM_DD_HH-MM-SS.mp4
        └── ...
```

## Reporting Bugs

If you encounter a crash or unexpected behavior,
please [open an issue](https://github.com/mhay10/IsometricAnimationsMod/issues) and include:

- Minecraft version
- Fabric Loader version
- Mod version
- Command used
- Logs or crash reports

## Contributing

Contributions are gladly accepted! Fork the repo and open a pull request with your changes.
