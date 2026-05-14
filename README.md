# MarisHomes

MarisHomes is a Folia-safe homes plugin with GUI support and admin tools.

## What It Handles

- Player home creation and teleport
- Home deletion
- GUI-driven home browsing
- Admin maintenance command path

## Requirements

- Paper / Folia 1.21+
- Java 21

## Installation

1. Place the plugin jar in `plugins`.
2. Start the server once.
3. Edit `config.yml`, `gui.yml`, and `message.yml`.
4. Restart the server.

## Quick Setup

1. Set a few homes with a test player.
2. Open `/home` and verify the GUI layout.
3. Test deletion flow with `/delhome <name>`.
4. Review any home limits or restrictions in `config.yml`.

## Player Commands

- `/home` - Open home GUI or teleport flow.
- `/sethome <name>` - Save a home.
- `/delhome <name>` - Delete a home.

## Admin Command

- `/homeadmin` - Admin and reload path.

## Command Examples

```text
/sethome base
/home
/delhome base
```

## Files

- `config.yml` - Main settings.
- `gui.yml` - GUI layout and text.
- `message.yml` - Player-facing messages.

## Common Mistakes

- Renaming a home in expectations even though the plugin stores homes by the name you originally create.
- Testing GUI only and not direct command flow.
- Forgetting to restart after major GUI edits.

## Notes

- This plugin is marked as Folia supported.
- Home limits and teleport conditions should be reviewed before production rollout.