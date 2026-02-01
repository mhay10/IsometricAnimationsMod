
# Isometric Animations Mod

A Minecraft mod that captures sections of your world and generates isometric animations,
perfect for showcasing redstone contraptions or dynamic scenes in your Minecraft world.

[Watch Example Render Video](https://youtube.com/shorts/_vldU3FVRaE?feature=share)

## Features

- Capture a rectangular area of your Minecraft world
- Generate smooth-ish isometric animations
- Adjustable scale, rotation, slant and duration settings
- Export animations as files for easy sharing

## Dependencies

This mod requires the following dependencies to be installed:

- [Fabric API](https://fabricmc.net/use/) - Essential modding library for Fabric
- [Isometric Renders](https://modrinth.com/mod/isometric-renders) - Core rendering library
- [owo-lib](https://modrinth.com/mod/owo-lib) - Dependency of Isometric Renders

Make sure all dependencies are installed and compatible with your Minecraft version.

You also need to have [FFmpeg](https://ffmpeg.org/download.html) installed and available in your system PATH for video encoding.

## Usage

### Getting Help

To see command usage information in-game, use:
```
/animate help
```

**Note:** The `/animate` command is a **client-side command**. This means:
- It will NOT appear in `/help` (which only lists server commands)
- You can use it in singleplayer or multiplayer
- Use Tab completion for parameter suggestions
- Type `/animate help` to see the full command syntax

### Command Structure

```
/animate <pos1> <pos2> <scale> <rotation> <slant> <duration> [fps]
````

### Parameters

- `<pos1>`: The first corner of the rectangular area to capture
- `<pos2>`: The opposite corner of the rectangular area to capture
- `<scale>`: Scale factor for the animation (0 - 500)
- `<rotation>`: Rotation angle in degrees (0 - 360)
- `<slant>`: Slant angle in degrees (-90 - 90)
- `<duration>`: Duration of the animation in seconds
- `[fps]`: **OPTIONAL** - Frames per second (20-1000, default: 1000)

### Example Command

This command captures a region from `(-4, -51, 5)` to `(5, -59, 4)` with 
a scale of 125, rotation of 200 degrees, slant of 20 degrees, and duration of 1.5 seconds.
```
/animate -4 -51 5 5 -59 4 125 200 20 1.5
```

## Output Files

Generated animations and frames are saved in your Minecraft instance directory under:

```
+-- .minecraft/
|   +-- isoanimations/
|   |   +-- animations/
|   |   |   +-- animation_YYYY_MM_DD_HH-MM-SS.mp4
|   |   |   +-- ...
|   |   +-- frames/
|   |       +-- frame_XXXXX.png
|   |       +-- ...
```

## Troubleshooting

### Common Issues

- **Command not recognized:** Ensure the mod is correctly installed and loaded.
- **FFmpeg not found:** Make sure FFmpeg is installed and added to your system PATH
- **Output files not found:** Check the `.minecraft/isoanimations/` directory for generated files.

### Known Issues

- Occasionally, some frames may not render correctly. If this happens, try re-running the command.
- Glitchy animations may occur depending on the complexity of the scene and settings used. Experiment with different parameters for better results.

### Getting Help

If you encounter any bugs, crashes, or unexpected behavior, please open an issue on the [Issue Page](https://github.com/mhay10/IsometricAnimationsMod/issues)
with the following information:

- Minecraft version
- Fabric Loader version
- Mod version and dependencies versions
- Full command used
- Any relevant logs or crash reports
- Detailed description of the issue

Your feedback is invaluable in helping improve the mod!

## Contributing

Contributions are welcome! If you'd like to contribute, please fork the repository and create a pull request with your changes.
