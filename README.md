<a id="top"></a>

<p align="center">
  <strong>English</strong> · <a href="./README.zh-CN.md">简体中文</a>
</p>

<p align="center">
  <img src="src/main/resources/assets/areas-hint/icon.png" alt="Areas Hint icon" width="128" height="128">
</p>

<h1 align="center">Areas Hint</h1>

<p align="center">
  <img alt="Minecraft build targets" src="https://img.shields.io/badge/Minecraft-1.20.1%20%7C%201.20.4%20%7C%201.20.6%20%7C%201.21.1%20%7C%201.21.4-3C8527?style=flat-square">
  <img alt="Fabric" src="https://img.shields.io/badge/Loader-Fabric-DBD0B4?style=flat-square">
  <img alt="Java" src="https://img.shields.io/badge/Java-17%20%2F%2021-ED8B00?style=flat-square&logo=openjdk&logoColor=white">
  <img alt="Source version" src="https://img.shields.io/badge/Mod-4.6.0-8A2BE2?style=flat-square">
  <img alt="License" src="https://img.shields.io/badge/License-MIT-blue?style=flat-square">
</p>

<p align="center">
  Give the places in your Minecraft world a name.
</p>

<p align="center">
  Define custom regions and show their names as players enter them, with nested areas, subtitles, descriptions, map overlays, and optional sounds.
</p>

<p align="center">
  <a href="https://modrinth.com/mod/area-hints">Download on Modrinth</a> ·
  <a href="https://github.com/IMSng1019/area-hints/issues">Report an issue</a> ·
  <a href="./LICENSE">MIT license</a>
</p>

<p align="center">
  <a href="#quickstart">Quick start</a> ·
  <a href="#commands">Commands</a> ·
  <a href="#server-config">Server configuration</a> ·
  <a href="#data-format">Data format</a> ·
  <a href="#development">Build from source</a>
</p>

---

<a id="overview"></a>

## Overview

Areas Hint is a **Minecraft Fabric** mod for naming places in your world. The current source version is **4.6.0**. A region can be a polygon with an altitude range, and regions can form a hierarchy such as **Kingdom → Capital → Throne Room**.

Use it for RPG servers, adventure maps, building showcases, or landmarks in a survival world. Players can create and edit areas through commands, clickable chat prompts, and an in-game command panel.

| Feature | What it provides |
|---|---|
| Area geometry | Polygon boundaries, altitude limits, bounding-box filtering, and nested areas |
| Titles | Area and dimension names, combined display names, subtitles, colors, and configurable title styles and sizes |
| Editing | Interactive creation, expansion, shrinking, splitting, vertex editing, renaming, and signatures |
| Descriptions | Area and dimension descriptions displayed and edited through a book interface |
| Navigation | Boundary visualization, center and random safe teleportation, Xaero overlays, and BlueMap markers |
| Server management | Edit approval, temporary previews, revision and resubmission, urbanization requests, and area game modes |
| Client preferences | Rendering mode, language, shared action key, detection frequency, and transition sounds |
| World separation | Separate area, dimension, and description data for each world or server |

The client detects areas for title display using synchronized data. The server manages persistent data, permissions, approvals, and synchronization. When area game modes are enabled, the server independently checks player positions against its own area data.

## Contents

- [Overview](#overview)
- [Versions and requirements](#versions)
- [Installation](#installation)
- [Quick start](#quickstart)
- [Core concepts](#concepts)
- [Commands](#commands)
- [Server configuration and approval](#server-config)
- [Data format and client configuration](#data-format)
- [World and server folders](#storage)
- [Architecture and source layout](#architecture)
- [Optional integrations](#compatibility)
- [Build and development](#development)
- [Further documentation](#docs)
- [Limitations](#limitations)
- [License](#license)

<a id="versions"></a>

## Versions and requirements

These are the targets currently enabled in `settings.gradle`, with dependencies from `gradle.properties`. This table describes the source build configuration; it does not imply that every target has been published or verified in-game. Check the Minecraft version on the individual Modrinth download before installing.

| Minecraft build target | Minimum Java | Minimum Fabric Loader | Fabric API used by the build |
|---|---|---|---|
| `1.20.1` | `17` | `0.16.14` | `0.92.12+1.20.1` |
| `1.20.4` (development baseline) | `17` | `0.16.14` | `0.97.2+1.20.4` |
| `1.20.6` | `21` | `0.16.14` | `0.100.8+1.20.6` |
| `1.21.1` | `21` | `0.16.14` | `0.116.17+1.21.1` |
| `1.21.4` | `21` | `0.19.5` | `0.119.4+1.21.4` |

Each JAR targets one Minecraft version. Install the matching JAR and Fabric API. Dependencies for `1.21.5`, `1.21.8`, and `1.21.11` are declared but those versions are not currently enabled as build targets. Work on `26.x` remains experimental planning.

LuckPerms, BlueMap, Mod Menu, Xaero's Minimap, Xaero's World Map, and VulkanMod are optional. See [Optional integrations](#compatibility) for their roles and the exact Xaero versions.

<a id="installation"></a>

## Installation

1. Install Fabric Loader for your Minecraft version and use the Java version listed above.
2. Install Fabric API for that Minecraft version.
3. Download the matching Areas Hint JAR from [Modrinth](https://modrinth.com/mod/area-hints) and place it in the instance's `mods` folder.
4. Start the game or server.

Install the mod on **both the client and the server** for the full experience. In singleplayer, the client and integrated server work together. Servers can require a compatible client installation with `force_use_areahint`.

<a id="quickstart"></a>

## Quick start

1. Join a world and run `/areahint easyadd` (or its alias `/areahint addarea`).
2. Follow the prompts to choose the area name, level, and parent, record polygon vertices, and set altitude and color.
3. Use the shared action key, **X by default**, to record vertices. While idle, hold it for about **0.5 seconds** to open the command panel.
4. Save the area. With the default `submit_approval: true`, edits by non-OP players wait for OP approval; OP edits take effect immediately.
5. Enter the saved or approved area to see its title. Use `/areahint boundviz` to show boundaries and `/areahint check` to list combined area names.
6. Add a subtitle with `/areahint addsubtitle`, or a book description with `/areahint adddescription`.
7. After manually editing area files or regular configuration, run `/areahint reload`. Changes to the server-wide `server-config.json` require a server restart.

<a id="concepts"></a>

## Core concepts

The project calls a named in-game area a **domain** (域名 in the Chinese interface). Here, “area” and “domain” refer to the same feature.

| Concept | Meaning |
|---|---|
| Primary vertices (`vertices`) | Polygon vertices on the horizontal `x` / `z` plane |
| Secondary vertices (`second-vertices`) | Bounding rectangle used for fast area filtering, also using `x` / `z` |
| Altitude (`altitude`) | Minimum and maximum vertical coordinates |
| Level (`level`) | An integer: `1` is a top-level area, `2` its child, `3` the next level; larger numbers mean deeper nesting |
| Parent (`base-name`) | The immediate parent area, whose level is one less; a top-level area can use `null` |
| Combined / surface name (`surfacename`) | The name used for a combined display |
| Creator signature (`signature`) | The creator's player name, or `null` |
| Additional signatures (`signatures`) | Additional players authorized under the area's editing rules |
| Subtitle (`subtitle`, `subtitlecolor`) | Optional text below the title, with its own color |
| Area type (`type`) | `rural` by default when absent; an approved urbanization changes it to `urban` |
| Game mode (`gamemode`) | An optional area game mode, used only when the server enables `force_use_areahint` |

Dimension names conceptually sit above ordinary areas at level `0.5`, but are stored separately in `dimensional_names.json`. Ordinary area titles take display priority; the dimension name is shown when the player is outside all ordinary areas.

<a id="commands"></a>

## Commands

Use `/areahint ...` in chat or open the command panel with the shared action key. Interactive buttons supply the next command arguments for you. Server commands still enforce permissions, signatures, and server configuration when launched from the panel.

In the reference below, `<argument>` is required and `[argument]` is optional. The root `/areahintc` is used for local sound settings.

### Shared action key

The `recordKey` setting defaults to `88`, the GLFW key code for **X**.

| Context | Action |
|---|---|
| Recording vertices | Tap to record the player's current coordinates |
| Recording through the graphical workflow | Hold for about 0.5 seconds to open continue / finish / cancel actions |
| Idle | Tap to read the current area or dimension description; hold to open the command panel |
| Command panel open | Press the key to close the panel |

To change it, run `/areahint replacebutton`, press a new key, then use `/areahint replacebutton confirm`. Use `cancel` to discard the change. Escape, Enter, Tab, Shift, Ctrl, Alt, and unknown keys cannot be assigned. This setting is saved locally in `config.json`.

### General and navigation

| Command | Purpose |
|---|---|
| `/areahint help` | Show command help |
| `/areahint reload` | Reload configuration and area files; administrator access by default |
| `/areahint on` / `/areahint off` | Enable or disable local area title hints |
| `/areahint check [combined-name]` | List combined names or inspect a specific one |
| `/areahint delete` | Start interactive area deletion |
| `/areahint tcp [area]` | Teleport to a safe location near an area's center |
| `/areahint udp [area]` | Find a random safe destination within an area |
| `/areahint settp [command-prefix]` | Set the teleport command prefix, such as `tp` or `minecraft:tp` |

Teleportation uses `areahint.command.teleport`; configuring its command prefix uses `areahint.command.settp`.

### Creation and geometry

| Command | Purpose |
|---|---|
| `/areahint easyadd` / `/areahint addarea` | Create an area interactively; both names support the same subcommands |
| `/areahint add <JSON>` | Add an area from JSON; administrator access by default |
| `/areahint rename` | Rename an area |
| `/areahint expandarea [area]` | Expand an area, optionally selecting it directly |
| `/areahint shrinkarea [area]` | Shrink an area, optionally selecting it directly |
| `/areahint dividearea` | Split an area |
| `/areahint addhint` / `/areahint deletehint` | Add or remove individual vertices |
| `/areahint sethigh` | Configure an area's altitude range |
| `/areahint addsignature` / `/areahint deletesignature` | Manage additional signatures |
| `/areahint replaceareagamemode` | Choose an area and game mode; requires `force_use_areahint`, with permission level 2 by default |
| `/areahint applyurbanarea` | Request urbanization; requires `submit_urban_area` |

Direct selection with `expandarea` and `shrinkarea` supports Tab completion filtered to areas the player may edit. With no area argument, a clickable list is shown. During either workflow, use `continue`, `save`, or `cancel` as prompted.

Unless LuckPerms explicitly configures the command node, `shrinkarea`, `addsignature`, and `deletesignature` require the player's name in the current area's own `signature` or `signatures`, or operator permission level 2 or above. A parent's signature does not grant access to these three commands. Expansion retains its existing rule: a signature on the current area or its immediate parent grants access, as does operator permission level 2 or above. Explicit LuckPerms node results retain their existing precedence for these editing commands.

For urbanization, non-OP players can select rural areas whose own `signature` names them or whose `signatures` list includes them. A parent's signature does not grant access. OPs with permission level 2 or above can select any eligible area. Non-OP requests require approval even when ordinary edit approval is disabled; OP changes apply immediately. The permission node `areahint.command.applyurbanarea` does not bypass the ownership check or grant approval authority.

### Titles and display

| Command | Purpose |
|---|---|
| `/areahint recolor` | Change an area's title color |
| `/areahint hintrender [cpu\|opengl\|vulkan]` | Show or change the rendering mode |
| `/areahint titlestyle` | Choose full, simple, or mixed title display |
| `/areahint titlesize` | Choose a title size |
| `/areahint addsubtitle` / `/areahint replacesubtitle` | Add or replace an area's subtitle |
| `/areahint deletesubtitle` | Remove an area's subtitle |
| `/areahint replacesubtitlecolor` | Change the subtitle color |
| `/areahint replacesubtitlesize` | Change local subtitle size |
| `/areahint frequency [1-60]` | Show or set the maximum detection frequency per second; decimals are supported |
| `/areahint boundviz` | Toggle boundary visualization |

### Descriptions and dimension names

| Command | Purpose |
|---|---|
| `/areahint adddescription` / `/areahint replacedescription` | Add or replace an area description |
| `/areahint deletedescription` | Delete an area description without deleting the area |
| `/areahint adddimensionalitydescription` / `/areahint replacedimensionalitydescription` | Add or replace a dimension description |
| `/areahint deletedimensionalitydescription` | Delete a dimension description |
| `/areahint dimensionalityname` | Manage dimension names interactively |
| `/areahint dimensionalitycolor` | Manage dimension title colors |
| `/areahint firstdimname <name>` | Name a previously unnamed dimension when first prompted |
| `/areahint firstdimname_skip` | Skip the initial dimension naming prompt |

Description edits follow administrator or authorized signature rules. Ordinary area and dimension descriptions are stored separately from the area geometry files.

### Transition sounds

| Command | Purpose |
|---|---|
| `/areahintc replacesoundevent` | Open the sound category and selection workflow |
| `/areahintc replacesoundevent select <sound-id> <pitch>` | Choose an available sound with pitch from `0.5` to `2.0` |
| `/areahintc replacesoundevent none` | Disable transition sounds |
| `/areahintc soundlevel` | Open volume selection |
| `/areahintc soundlevel <0-1>` | Set volume; decimals are supported and `0` mutes it |

Sound preferences affect only the current client. Sounds are disabled by default. The command panel provides the same controls, and the older `/areahint replacesoundevent` and `/areahint soundlevel` entry points remain available for compatibility.

### Language and diagnostics

| Command | Purpose |
|---|---|
| `/areahint language` | Choose the client language |
| `/areahint replacebutton` | Change the shared action key |
| `/areahint debug [on\|off\|status]` | Toggle, set, or inspect debugging; administrator access by default |
| `/areahint serverlanguage <language-code>` | Set the server log language; permission level 4 by default |

### Permissions

Command nodes use the `areahint.command.*` namespace. When LuckPerms is absent or a node is undefined, the mod falls back to vanilla permission levels and the applicable area editing rules. Some editing nodes override those fallback rules; urbanization ownership and OP approval authority have separate checks.

The following personal settings and first-time naming commands have their own nodes and use permission level `0` as the fallback:

| Command | LuckPerms node |
|---|---|
| `on` | `areahint.command.on` |
| `off` | `areahint.command.off` |
| `firstdimname` | `areahint.command.firstdimname` |
| `firstdimname_skip` | `areahint.command.firstdimname_skip` |
| `replacebutton` | `areahint.command.replacebutton` |
| `language` | `areahint.command.language` |
| `boundviz` | `areahint.command.boundviz` |

Some aliases share a permission node:

| Command or alias | LuckPerms node |
|---|---|
| `easyadd`, `addarea` | `areahint.command.easyadd` |
| `tcp`, `udp` | `areahint.command.teleport` |
| `addsubtitle`, `replacesubtitle` | `areahint.command.addsubtitle` |
| `deletesubtitle` | `areahint.command.deletesubtitle` |
| `replacesubtitlecolor` | `areahint.command.replacesubtitlecolor` |
| `replacesubtitlesize` | `areahint.command.replacesubtitlesize` |
| `adddescription`, `replacedescription` | `areahint.command.adddescription` |
| `adddimensionalitydescription`, `replacedimensionalitydescription` | `areahint.command.adddimensionalitydescription` |

<details>
<summary><strong>Expand the subcommand reference</strong></summary>

These are common command branches. Interactive buttons supply arguments during each workflow.

### General commands

- `/areahint help`
- `/areahint reload`
- `/areahint on`
- `/areahint off`
- `/areahint debug`
- `/areahint debug on`
- `/areahint debug off`
- `/areahint debug status`
- `/areahint serverlanguage <language-code>`

### Inspection, deletion, and teleportation

- `/areahint check`
- `/areahint check <combined-name>`
- `/areahint delete`
- `/areahint delete select <area>`
- `/areahint delete confirm`
- `/areahint delete cancel`
- `/areahint tcp`
- `/areahint tcp <area>`
- `/areahint udp`
- `/areahint udp <area>`
- `/areahint settp`
- `/areahint settp <command-prefix>`

### Dimension names

- `/areahint dimensionalityname`
- `/areahint dimensionalityname select <dimension>`
- `/areahint dimensionalityname name <new-name>`
- `/areahint dimensionalityname confirm`
- `/areahint dimensionalityname cancel`
- `/areahint dimensionalitycolor`
- `/areahint dimensionalitycolor select <dimension>`
- `/areahint dimensionalitycolor color <color>`
- `/areahint dimensionalitycolor confirm`
- `/areahint dimensionalitycolor cancel`
- `/areahint firstdimname <name>`
- `/areahint firstdimname_skip`

### Creation and basic editing

- `/areahint add <JSON>`
- `/areahint easyadd`
- `/areahint easyadd cancel`
- `/areahint easyadd level <1-3>`
- `/areahint easyadd base <parent-area>`
- `/areahint easyadd continue`
- `/areahint easyadd finish`
- `/areahint easyadd altitude auto`
- `/areahint easyadd altitude custom`
- `/areahint easyadd altitude unlimited`
- `/areahint easyadd color <color>`
- `/areahint easyadd save`
- `/areahint addarea`
- `/areahint addarea cancel`
- `/areahint addarea level <1-3>`
- `/areahint addarea base <parent-area>`
- `/areahint addarea continue`
- `/areahint addarea finish`
- `/areahint addarea altitude auto`
- `/areahint addarea altitude custom`
- `/areahint addarea altitude unlimited`
- `/areahint addarea color <color>`
- `/areahint addarea save`
- `/areahint rename`
- `/areahint rename select <area>`
- `/areahint rename confirm`
- `/areahint rename cancel`
- `/areahint recolor`
- `/areahint recolor select <area>`
- `/areahint recolor color <color>`
- `/areahint recolor confirm`
- `/areahint recolor cancel`
- `/areahint sethigh`
- `/areahint sethigh <area>`
- `/areahint sethigh custom <area>`
- `/areahint sethigh unlimited <area>`
- `/areahint sethigh cancel`
- `/areahint replaceareagamemode`
- `/areahint applyurbanarea`

### Geometry and vertex editing

- `/areahint expandarea`
- `/areahint expandarea <area>`
- `/areahint expandarea select <area>`
- `/areahint expandarea continue`
- `/areahint expandarea save`
- `/areahint expandarea cancel`
- `/areahint shrinkarea`
- `/areahint shrinkarea <area>`
- `/areahint shrinkarea select <area>`
- `/areahint shrinkarea continue`
- `/areahint shrinkarea save`
- `/areahint shrinkarea cancel`
- `/areahint dividearea`
- `/areahint dividearea select <area>`
- `/areahint dividearea continue`
- `/areahint dividearea save`
- `/areahint dividearea name <new-area-name>`
- `/areahint dividearea level <1-3>`
- `/areahint dividearea base <parent-area>`
- `/areahint dividearea color <color>`
- `/areahint dividearea cancel`
- `/areahint addhint`
- `/areahint addhint select <area>`
- `/areahint addhint continue`
- `/areahint addhint submit`
- `/areahint addhint cancel`
- `/areahint deletehint`
- `/areahint deletehint select <area>`
- `/areahint deletehint toggle <vertex-index>`
- `/areahint deletehint submit`
- `/areahint deletehint cancel`

### Display and client preferences

- `/areahint frequency`
- `/areahint frequency <1-60>` (decimals supported)
- `/areahint hintrender`
- `/areahint hintrender <cpu|opengl|vulkan>`
- `/areahint titlestyle`
- `/areahint titlestyle select <style>`
- `/areahint titlestyle cancel`
- `/areahint titlesize`
- `/areahint titlesize select <size>`
- `/areahint titlesize cancel`
- `/areahint replacesubtitlesize`
- `/areahint replacesubtitlesize select <size>`
- `/areahint replacesubtitlesize cancel`
- `/areahint replacebutton`
- `/areahint replacebutton confirm`
- `/areahint replacebutton cancel`
- `/areahint language`
- `/areahint language select <language-code>`
- `/areahint language cancel`
- `/areahint boundviz`
- `/areahintc replacesoundevent`
- `/areahintc replacesoundevent category <category> [page]`
- `/areahintc replacesoundevent instrument <sound-id> [page]`
- `/areahintc replacesoundevent select <sound-id> <pitch>`
- `/areahintc replacesoundevent none`
- `/areahintc replacesoundevent cancel`
- `/areahintc soundlevel [0-1]`

### Subtitles, descriptions, and signatures

- `/areahint addsubtitle`
- `/areahint addsubtitle select <area>`
- `/areahint addsubtitle color <color>`
- `/areahint addsubtitle confirm`
- `/areahint addsubtitle cancel`
- `/areahint replacesubtitle`
- `/areahint replacesubtitle select <area>`
- `/areahint replacesubtitle color <color>`
- `/areahint replacesubtitle confirm`
- `/areahint replacesubtitle cancel`
- `/areahint deletesubtitle`
- `/areahint deletesubtitle select <area>`
- `/areahint deletesubtitle confirm`
- `/areahint deletesubtitle cancel`
- `/areahint replacesubtitlecolor`
- `/areahint replacesubtitlecolor select <area>`
- `/areahint replacesubtitlecolor color <color>`
- `/areahint replacesubtitlecolor confirm`
- `/areahint replacesubtitlecolor cancel`
- `/areahint adddescription`
- `/areahint adddescription search [keyword]`
- `/areahint adddescription select <area>`
- `/areahint adddescription text <description>`
- `/areahint adddescription confirm`
- `/areahint adddescription cancel`
- `/areahint replacedescription`
- `/areahint replacedescription search [keyword]`
- `/areahint replacedescription select <area>`
- `/areahint replacedescription text <description>`
- `/areahint replacedescription confirm`
- `/areahint replacedescription cancel`
- `/areahint deletedescription`
- `/areahint deletedescription search [keyword]`
- `/areahint deletedescription select <area>`
- `/areahint deletedescription confirm`
- `/areahint deletedescription confirm2`
- `/areahint deletedescription cancel`
- `/areahint adddimensionalitydescription`
- `/areahint adddimensionalitydescription search [keyword]`
- `/areahint adddimensionalitydescription select <dimension-name>`
- `/areahint adddimensionalitydescription text <description>`
- `/areahint adddimensionalitydescription confirm`
- `/areahint adddimensionalitydescription cancel`
- `/areahint replacedimensionalitydescription`
- `/areahint replacedimensionalitydescription search [keyword]`
- `/areahint replacedimensionalitydescription select <dimension-name>`
- `/areahint replacedimensionalitydescription text <description>`
- `/areahint replacedimensionalitydescription confirm`
- `/areahint replacedimensionalitydescription cancel`
- `/areahint deletedimensionalitydescription`
- `/areahint deletedimensionalitydescription search [keyword]`
- `/areahint deletedimensionalitydescription select <dimension-name>`
- `/areahint deletedimensionalitydescription confirm`
- `/areahint deletedimensionalitydescription confirm2`
- `/areahint deletedimensionalitydescription cancel`
- `/areahint addsignature`
- `/areahint addsignature select <area>`
- `/areahint addsignature name <player-name>`
- `/areahint addsignature confirm`
- `/areahint addsignature cancel`
- `/areahint deletesignature`
- `/areahint deletesignature select <area>`
- `/areahint deletesignature name <player-name>`
- `/areahint deletesignature confirm`
- `/areahint deletesignature confirm2`
- `/areahint deletesignature cancel`

</details>

---

<a id="server-config"></a>

## Server configuration and approval

The server reads the global `areas-hint/server-config.json` from its game directory at startup. Restart the server after changing it. The file with the same name inside a client's world folder is a synchronized copy, refreshed on connection.

```json
{
  "submit_approval": true,
  "submit_urban_area": false,
  "force_use_areahint": false,
  "default_gamemode": "survival"
}
```

| Option | Default | Behavior |
|---|---|---|
| `submit_approval` | `true` | Authorized edits by non-OP players become requests; OP edits apply immediately |
| `submit_urban_area` | `false` | Independently enables urbanization requests and their command / panel entry |
| `force_use_areahint` | `false` | Checks the client mod during login and enables server-enforced area game modes |
| `default_gamemode` | The server's default mode | Used outside all areas or when the selected area has no valid game mode |

The example uses `survival`, but the initial `default_gamemode` comes from the server's own default mode (`gamemode` in `server.properties` on a dedicated server). Existing valid values are preserved. Missing fields and invalid values are repaired automatically. A damaged JSON file is backed up as `server-config.invalid-*.json.bak` before defaults are generated.

### Requests, previews, and replies

1. A non-OP player completes an authorized edit, such as area creation or deletion, geometry changes, renaming, color, altitude, signatures, subtitles, or descriptions. With approval enabled, the server saves a request and leaves the active data unchanged.
2. Hold the action key to open the command panel. OPs can open the approval queue; applicants can view their own approval replies.
3. An OP can add comments and **approve**, **reject**, or **return** a request for revision. Request details include temporary boundary previews, teleportation, and vertex editing without applying those edits to the active files.
4. A returned request can be revised and resubmitted with its comment history. Offline applicants receive their replies after reconnecting.

Approval does not expand the applicant's existing editing permissions. Command nodes do not grant OP approval authority. If the underlying area changes while a request is pending, approval will not silently overwrite the newer data.

Urbanization uses this same workflow whenever `submit_urban_area` is enabled, even if `submit_approval` is disabled. An OP urbanizing an eligible area applies `"type": "urban"` immediately; a non-OP player waits for approval.

Preview buttons invoke these branches automatically. The optional area index starts at `0`:

```text
/areahint tcp approval <request-id> [index]
/areahint addhint approval <request-id> [index]
/areahint deletehint approval <request-id> [index]
```

### Required client installation and area game modes

With `force_use_areahint` enabled, a joining client must support the mod's login handshake and have a mod version at least as new as the server's. Missing or outdated installations are rejected with a download address.

The server uses its own player coordinates, current dimension, and active area files to choose the deepest applicable area and apply its `gamemode`. A missing or invalid mode falls back to `default_gamemode`; it does not inherit the parent's mode. Leaving all areas also applies the default. Disabling local title hints does not disable this behavior.

Run `/areahint replaceareagamemode`, choose an area, then choose survival, creative, adventure, or spectator. The command panel exposes the same workflow. It requires permission level `2` by default and supports `areahint.command.replaceareagamemode`. Saving triggers an area reload and synchronization.

For more detailed workflow notes, see [COMMAND_USAGE.md](./COMMAND_USAGE.md) (Simplified Chinese).

---

<a id="data-format"></a>

## Data format and client configuration

### Area JSON

This is a single area object. The root of a dimension file such as `overworld.json`, `the_nether.json`, or `the_end.json` must be an **array of area objects**, for example `[ { ... } ]`. Both vertex lists use `x` / `z`; vertical limits are stored separately in `altitude`.

```json
{
  "name": "Elven Forest",
  "vertices": [
    { "x": 0, "z": 10 },
    { "x": 10, "z": 0 },
    { "x": 0, "z": -10 },
    { "x": -10, "z": 0 }
  ],
  "second-vertices": [
    { "x": -10, "z": 10 },
    { "x": 10, "z": 10 },
    { "x": 10, "z": -10 },
    { "x": -10, "z": -10 }
  ],
  "altitude": {
    "max": 100,
    "min": 0
  },
  "level": 2,
  "base-name": "Wild Continent",
  "signature": "player_name",
  "signatures": ["friend_name"],
  "color": "#55FFAA",
  "surfacename": "Elven Forest",
  "subtitle": "Wind through the ancient trees",
  "subtitlecolor": "#AAFFCC",
  "type": "rural",
  "gamemode": "adventure"
}
```

This level-2 example assumes a level-1 parent named `Wild Continent` exists. Use `level: 1` and `base-name: null` for an independent top-level area.

| Field | Purpose |
|---|---|
| `name` | Area name |
| `vertices` | Polygon vertices defining the actual shape |
| `second-vertices` | Auxiliary bounding rectangle for filtering |
| `altitude.min`, `altitude.max` | Lower and upper height limits |
| `level` | Integer hierarchy level; larger values mean deeper nesting |
| `base-name` | Immediate parent area, or `null` for an independent top-level area |
| `signature` | Creator's player name, or `null` |
| `signatures` | Additional authorized player names; may be empty or omitted |
| `color` | Title color; hexadecimal colors and the mod's flashing color values are supported |
| `surfacename` | Combined display name; may be `null` |
| `subtitle` | Optional subtitle; omitted or empty text is not displayed |
| `subtitlecolor` | Optional subtitle color, including supported flashing values |
| `type` | `rural` or `urban`; absent values are treated as `rural` |
| `gamemode` | Optional `survival`, `creative`, `adventure`, or `spectator`; applied only with `force_use_areahint` |

### Client configuration (`config.json`)

The client configuration is stored at `areas-hint/config.json` in the game directory. These are the default values; the unlocked language can follow the game's language.

```json
{
  "frequency": 1,
  "hintRender": "OpenGL",
  "titleStyle": "mixed",
  "enabled": true,
  "recordKey": 88,
  "titleSize": "medium",
  "subtitlesize": "auto",
  "boundVizEnabled": false,
  "xaeroMinimapOverlayEnabled": true,
  "xaeroWorldMapOverlayEnabled": true,
  "language": "zh_cn",
  "languageLocked": false,
  "teleportFormat": "tp",
  "soundEvent": "none",
  "soundPitch": 1.0,
  "soundLevel": 1.0
}
```

| Setting | Meaning |
|---|---|
| `frequency` | Maximum checks per second; default `1`, command range `1–60`, decimals supported. Higher values shorten the detection interval |
| `hintRender` | `CPU`, `OpenGL`, or experimental `Vulkan` |
| `titleStyle` | `full`, `simple`, or `mixed` |
| `enabled` | Whether local area title hints are enabled |
| `recordKey` | Shared action key; `88` is X |
| `titleSize` | Presets from `extra_large` to `extra_small`, or `custom:<scale>` |
| `subtitlesize` | `auto`, a size preset, or `custom:<scale>`; the older `subtitleSize` spelling is also accepted when reading |
| `boundVizEnabled` | Persistent boundary visualization setting |
| `xaeroMinimapOverlayEnabled` | Enable the overlay when a matching Xaero minimap is installed |
| `xaeroWorldMapOverlayEnabled` | Enable the overlay when a matching Xaero world map is installed |
| `language` | Language code, such as `en_us` or `zh_cn` |
| `languageLocked` | Keep the selected language instead of following the game language |
| `teleportFormat` | Teleport command prefix; defaults to `tp` |
| `soundEvent` | An available sound event ID, or `none` to disable sounds |
| `soundPitch` | Pitch from `0.5` to `2.0`; default `1.0` |
| `soundLevel` | Volume from `0.0` to `1.0`; default `1.0` |

### Title styles and rendering

| Title style | Display |
|---|---|
| `full` | Full area path |
| `simple` | Only the current area's name |
| `mixed` | A combination based on the hierarchy and context; the default style |

| Rendering mode | Notes |
|---|---|
| `CPU` | Software rendering option |
| `OpenGL` | Default rendering mode |
| `Vulkan` | Experimental; requires VulkanMod, otherwise falls back to OpenGL |

With `subtitlesize: "auto"`, the subtitle stays one size step below the title.

### Dimension names (`dimensional_names.json`)

```json
[
  {
    "dimensionId": "minecraft:overworld",
    "displayName": "Wild Continent",
    "color": "#FFFFFF",
    "signature": null
  },
  {
    "dimensionId": "minecraft:the_nether",
    "displayName": "Infernal Realm",
    "color": "#AA0000",
    "signature": null
  },
  {
    "dimensionId": "minecraft:the_end",
    "displayName": "The Last Frontier",
    "color": "#AA00AA",
    "signature": null
  }
]
```

Dimension names can appear when joining a world or leaving all ordinary areas. `color` controls their title color; `signature` participates in permissions for dimension descriptions and related actions.

### Description files

Descriptions live in the current world's `database/` directory rather than in the area geometry JSON. Each description is a separate JSON document:

```json
{
  "description": "A description that can be edited through the book interface."
}
```

---

<a id="storage"></a>

## World and server folders

Data is stored under `areas-hint/` in the game directory, not under `config/areas-hint/`. The mod creates and selects a separate folder for each world or server, keeping area files, dimension names, and descriptions isolated.

```text
.minecraft/areas-hint/
├── config.json
├── lang/
├── server-config.json                  # Global settings on a server / integrated server
├── approval-requests/                  # Server-side requests and history
│   └── <worldKey>/
│       └── <request-UUID>.json
└── <server-address>_<world-name>/
    ├── overworld.json
    ├── the_nether.json
    ├── the_end.json
    ├── dimensional_names.json
    ├── server-config.json              # Client copy refreshed on connection
    ├── approval-replies.json           # Current player's replies and history
    └── database/
        ├── overworld/
        ├── the_nether/
        └── the_end/
```

A dedicated server stores its global configuration and requests in its own game directory. Request `worldKey` values are derived from the server save path. Client reply files also record the player's UUID to keep replies separate when switching accounts.

When joining a world, the mod identifies the connection, creates missing folders and data files, selects the corresponding paths, and synchronizes the world context with the server. Area edits, descriptions, dimension names, and teleportation then use that context.

<a id="architecture"></a>

## Architecture and source layout

The client handles area detection, title animation, command interfaces, descriptions, boundary visualization, sound preferences, and map overlays. The server handles persisted data, commands, permissions, approvals, login checks, synchronization, and area game modes.

Detection filters by altitude and bounding box before applying a point-in-polygon ray-casting check. An odd number of boundary crossings means the point is inside; an even number means it is outside. Hierarchy and overlap rules then choose the applicable area.

```text
areas-hint/
├── README.md                          # English landing page
├── README.zh-CN.md                    # Simplified Chinese documentation
├── src/
│   ├── main/java/areahint/            # Shared and server code
│   │   ├── Areashint.java             # Main entry point
│   │   ├── approval/                 # Requests, previews, and replies
│   │   ├── config/                   # Server configuration
│   │   ├── gamemode/                 # Server-enforced area game modes
│   │   ├── handshake/                # Client mod login checks
│   │   ├── urban/                    # Urbanization requests
│   │   ├── permission/               # Permission nodes and LuckPerms
│   │   ├── map/                      # BlueMap integration
│   │   └── ...                       # Commands, data, files, networking, editing
│   ├── main/resources/
│   │   ├── fabric.mod.json
│   │   └── assets/areas-hint/         # Icon and translations
│   ├── client/java/areahint/          # Client detection, rendering, and interaction
│   │   ├── AreashintClient.java
│   │   ├── commandui/                # Command panel
│   │   ├── detection/                # Altitude, bounding box, and polygon checks
│   │   ├── render/                   # Titles and subtitles
│   │   ├── soundevent/               # Transition sounds
│   │   ├── xaero/                    # Shared overlay logic
│   │   └── ...
│   ├── client/resources/             # Client mixin configuration
│   └── test/java/areahint/            # Existing JUnit tests
├── versions/<Minecraft-version>/
│   ├── src/                          # Version adapters and integration implementations
│   └── build/libs/                   # Artifacts for this target
├── docs/plans/                       # Feature and multi-version design records
├── tools/                            # Maintenance and verification scripts
├── gradle/wrapper/                   # Gradle Wrapper
├── build.gradle                      # Shared build script
├── settings.gradle                   # Enabled build targets
├── stonecutter.gradle                # Active development version
├── gradle.properties                 # Mod version and per-target dependencies
└── LICENSE
```

Shared feature packages cover creation and geometry editing, subtitles, descriptions, signatures, teleportation, data, and world folders. Minecraft-specific networking, rendering, and optional integration implementations live under the corresponding `versions/` target.

<a id="compatibility"></a>

## Optional integrations

| Optional mod | Integration |
|---|---|
| LuckPerms | `areahint.command.*` permission nodes, with vanilla and area-rule fallbacks |
| BlueMap | Server-side area markers and color updates |
| Mod Menu | Client configuration screen |
| Xaero's Minimap | Area overlays on the minimap |
| Xaero's World Map | Area overlays and map interaction |
| VulkanMod | Experimental Vulkan rendering mode |

Xaero integrations check exact versions. If a mod is absent or its version does not match, its overlay is not enabled. Use the Fabric download matching the Minecraft target:

| Minecraft | Xaero's Minimap | Xaero's World Map |
|---|---|---|
| `1.20.1` | `25.3.2` | `1.39.2` |
| `1.20.4` | `25.2.0` | `1.39.2` |
| `1.20.6` | `25.2.10` | `1.39.12` |
| `1.21.1` | `25.3.2` | `1.39.2` |
| `1.21.4` | `25.3.2` | `1.39.2` |

Sodium is listed as a suggested mod in the metadata. Compatibility with other rendering, performance, and map mods depends on the Minecraft version and modpack. Optional integrations are not bundled with Areas Hint.

<a id="development"></a>

## Build and development

Run the Gradle Wrapper from the repository root. Use **JDK 21** to build all currently enabled targets. `settings.gradle` selects the build targets, while `stonecutter.gradle` currently selects `1.20.4` as the active development version.

```bash
# Build all enabled targets
./gradlew build

# Build one target
./gradlew :1.20.4:build

# Launch a specific development environment
./gradlew :1.20.4:runClient
./gradlew :1.20.4:runServer
./gradlew :1.20.4:runDatagen

# Existing tests and cleanup
./gradlew :1.20.4:test
./gradlew clean
```

In Windows PowerShell, replace `./gradlew` with `.\gradlew.bat`:

```powershell
.\gradlew.bat :1.20.4:build
```

### Build artifacts

Stonecutter places each target's output under `versions/<Minecraft-version>/build/libs/`. Filenames follow `mod-name-loader-Minecraft-version-mod-version`:

```text
versions/1.20.4/build/libs/areas-hint-fabric-1.20.4-4.6.0.jar
versions/1.21.4/build/libs/areas-hint-fabric-1.21.4-4.6.0.jar
```

Install the regular JAR, not the `-sources.jar` or `-dev.jar`. The `mod_loader` property only affects artifact naming; changing it does not convert the Fabric mod into a Forge or NeoForge mod.

For manual checks, use `/areahint debug on`, `/areahint boundviz`, and `/areahint reload`. Check both client and server logs when inspecting hierarchy, altitude, synchronization, or permission behavior. Description data is in the current world's `database/` directory.

<a id="docs"></a>

## Further documentation

The following detailed guides and development notes are currently in **Simplified Chinese**:

| Document | Topic |
|---|---|
| [COMMAND_USAGE.md](./COMMAND_USAGE.md) | Editing commands, approvals, urbanization, and game modes |
| [LOG_FEATURE.md](./LOG_FEATURE.md) | Logging |
| [TITLESIZE_GUIDE.md](./TITLESIZE_GUIDE.md) | Title sizes |
| [TITLESTYLE_INTERACTIVE_GUIDE.md](./TITLESTYLE_INTERACTIVE_GUIDE.md) | Interactive title styles |
| [TRANSLATION_GUIDE.md](./TRANSLATION_GUIDE.md) | Translation maintenance |
| [TRANSLATION_REPORT.md](./TRANSLATION_REPORT.md) | Translation notes |
| [RECOLOR_REFACTOR_SUMMARY.md](./RECOLOR_REFACTOR_SUMMARY.md) | Color editing implementation |
| [REPLACEBUTTON_IMPLEMENTATION_SUMMARY.md](./REPLACEBUTTON_IMPLEMENTATION_SUMMARY.md) | Shared action key implementation |
| [REPLACEBUTTON_EXPANDAREA_SHRINKAREA_INTEGRATION.md](./REPLACEBUTTON_EXPANDAREA_SHRINKAREA_INTEGRATION.md) | Shared key and geometry editing |
| [SHRINKAREA_REFACTOR.md](./SHRINKAREA_REFACTOR.md) | Area shrinking implementation |
| [SHRINKAREA_CHANGES.md](./SHRINKAREA_CHANGES.md) | Area shrinking change notes |
| [CLAUDE.md](./CLAUDE.md) | Development and project structure notes |
| [Multi-version planning](./docs/plans/multiversion/README.md) | Porting plans and historical build records |

Historical design notes may describe planned work; use the current build configuration and source code for implemented behavior.

<a id="limitations"></a>

## Limitations

- Vulkan rendering is experimental and falls back to OpenGL without VulkanMod.
- Command availability depends on server settings, permission nodes, and area editing rules.
- Description commands modify description files independently of the area geometry.
- Safe teleportation may fail when no suitable landing position exists in the area's terrain.
- Manual JSON edits must preserve field names, valid geometry, and parent / level relationships.
- Use the artifact for your Minecraft version; a configured build target alone does not establish in-game compatibility with a particular modpack.

<a id="license"></a>

## License

This project is licensed under the [MIT License](./LICENSE).

[Back to top](#top) · [简体中文](./README.zh-CN.md)
