# Isometric Animations Mod

A Minecraft mod that captures sections of your world and renders them as smooth isometric animations.

[Example Animation](example_render.mp4)

## Features

- Capture any rectangular area of your Minecraft world
- Preview and adjust your camera angle before rendering
- Generate high-quality isometric animations with customizable settings
- Export animations as MP4 videos or a series of TGA images

## Dependencies

| Dependency                                | Notes                           |
|-------------------------------------------|---------------------------------|
| [Fabric API](https://fabricmc.net/)       | Required                        |
| [Sodium](https://modrinth.com/mod/sodium) | ⚠️ Currently not fully working! |

### 🚨 NOTICE 🚨

**This mod is extremely file I/O intensive!** Every frame of the animation is tobe saved to disk
during rendering, so the speed of your storage device has a significant impact on performance.
For best experience, run Minecraft from an _NVMe SSD_. Rendering on a mechanical drive may result
in longer render times, stuttering, or incomplete animations.

## Installation

1. Install [Fabric Loader](https://fabricmc.net/use/) and [Fabric API](https://fabricmc.net/use/)
2. Download the latest release of this mod from
   the [Releases](https://github.com/mhay10/IsometricAnimationsMod/releases) page
3. Place the downloaded JAR file in your Minecraft `mods` folder
4. Launch Minecraft and verify that mod is loaded correctly

## Usage

All commands use the `/isoanimations` prefix.

### Commands

#### `clear`

Clears the current animation state and resets any pending render.

```
/isoanimations clear
```