# Changelog

## 1.1.5 - 2026-05-11

### Fixed
- Fixed pending regenerating blocks being left as air after server restart or plugin reload by flushing selected regen blocks before shutdown.

## 1.1.4 - 2026-05-11

### Changed
- Improved regeneration countdown text with fade-in, upward movement, see-through support, and display interpolation/teleport duration options.

## 1.1.3 - 2026-05-11

### Added
- Added a targeted countdown `TextDisplay` for regenerating blocks that appears only to players looking at that block.

## 1.1.2 - 2026-05-11

### Fixed
- Fixed editor GUI crashes on Leaf/Paper builds where XSeries custom skull texture support is incompatible with the server authlib.

## 1.1.1 - 2026-05-11

### Fixed
- Fixed duplicate pending regeneration tasks that could make the preview show one ore while the final block became another ore.
- Fixed `/oregen edit` crashing on server builds that do not expose `ItemFlag.HIDE_POTION_CONTENTS`.

## 1.1 - 2026-05-11

### Added
- Added optional per-level upgrade costs with `use_level_costs` and `level_costs`.
- Added configurable per-level costs for `tier` and `linhmach` in `config.yml`.
- Added regeneration preview animation using `BlockDisplay` while ore blocks are waiting to respawn.
- Added `global.generators.regeneration-preview` options for enabling, scale, tick interval, and brightness.

### Changed
- Delayed block regeneration now chooses the next ore before the delay starts, so the preview matches the final generated block.
- Regeneration preview animation duration follows the final Linh Mạch-adjusted regen delay.
