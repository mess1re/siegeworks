# Changelog

## 0.1.0-beta.8

### Notes

- Requires Axiomata 0.1.0-beta.7.
- Siege ladders are no longer drawn: craft a ladder base and build it up with the construction hammer. Drawn ladder blueprints from earlier versions can no longer be used.

### Added

- Incendiary pots: craft a clay pot, fill it by hand, seal it with a wick and light it before the throw; nearby fire and explosions set pots off. See the [Ammunition Guide](https://github.com/mess1re/siegeworks/wiki/Ammunition-Guide#incendiary-pot).
- Siege ladders can be carried in both hands, and a ladder build can be finished after any section. See [Siege Ladder](https://github.com/mess1re/siegeworks/wiki/Siege-Ladder).

### Changed

- Engines are built and repaired with any logs and planks; dismantling gives back oak.
- Engines roll to a stop when nothing drives them.
- Any player riding one of a team's mounts holds the reins.
- Siege engines no longer freeze, drown or take potion effects; cannon balls and grapeshot are mined with a pickaxe.
- Datapacks override Siegeworks files on every loader. Pot ingredients are set by a datapack profile; see the [Data Pack Reference](https://github.com/mess1re/siegeworks/wiki/Data-Pack-Reference).

### Fixed

- Hammer blows count only on the highlighted section of an engine being built.
- Ammunition blocks are shown properly in hands and item frames.

## 0.1.0-beta.7

### Added

- Added a “Return to tower” order to the Recruits command screen.

### Changed

- Unavailable siege commands are disabled and show the reason in both the Recruits screen and the RTS map.
- Taking control of a draft mount now requires standing near the animal.
- Requires Axiomata 0.1.0-beta.6 or newer.
- Optional Recruits RTS Command integration now requires 0.1.0-beta.2 or newer.

### Fixed

- Bolts appear aligned with their flight direction instead of rotating into place after firing.
- Fixed migration of old singijeon item IDs in Forge saves.
- RTS fire-zone orders now reach distant siege crews.
- The Recruits “Leave” order now also applies to siege tower passengers.
- Recruits ordered across a tower’s bridge stay aboard if there is no surface to disembark onto.
- Fixed startup crashes with OptiFine.

## 0.1.0-beta.6

### Added

- Siege engine ownership, team and alliance access, capture of unmanned enemy machines, and release of abandoned machines.
- Datapack rules for block material strength, penetration resistance and fracture energy.
- A clickable update notice, shown once per game launch. Disable it with `updates.showNotice` in the client config.

### Changed

- Projectile flight now uses gravity, air resistance and rocket thrust. Aiming calculations use the same flight model.
- Penetration and crater damage use projectile mass, diameter, speed and the material struck. Block cracks are saved with the world and remain until the block is removed or its state changes.
- Tower Crossbow bolts launch at 120 m/s; Arcballista bolts at 140 m/s.
- Cannon launch speeds are 300 m/s for Culverin, 330 m/s for Serpentine and 315 m/s for Mons Meg.
- Renamed Hwacha ammunition to So-singijeon and Jung-singijeon. Existing stacks of `singijeon` and `explosive_singijeon` load as `so_singijeon` and `jung_singijeon`, keeping their count and item data.
- Tower Crossbow bolts stack to 16.
- Projectile profiles are synchronized from the server on joining and after reload. Invalid overrides report their source and do not replace a working catalog during reload.
- Requires Axiomata 0.1.0-beta.5 or newer.

### Configuration and datapacks

- Client and server configs now include `configVersion = 1`. Missing settings are added while valid existing values are kept; deleting the configs is not needed.
- New `rules.ownership` settings control access, capture conditions and duration, and release of abandoned machines. `abandonAfterDays` defaults to 14; set it to 0 to keep ownership indefinitely.
- `rules.terrain.blockDamage` selects `EVERYWHERE` (default), `RESPECT_PROTECTION` or `NEVER`. Fire and falling debris also respect this setting.
- `rules.terrain.stoneFractureEnergy` adjusts the energy needed to fracture stone (default: 50,000 J/m³). It affects craters and accumulated cracks, not projectile flight, penetration resistance or entity damage. Material profiles can override it for specific blocks.
- Engine, projectile and block material JSON profiles use `formatVersion: 2`. Engine `projectileSpeed` is replaced by `muzzleVelocity` in m/s. Projectile profiles replace `drag` with `dragCoefficient` and `diameter`, and group impact effects under `entity`, `shock`, `blast` and `fire`; rocket thrust and burn time are under `motor`.
- Bundled profiles update with the mod jar. Custom datapacks using beta.5 fields need manual conversion; old and unknown fields are rejected. See the [datapack reference](https://github.com/mess1re/siegeworks/wiki/Data-Pack-Reference).

### Fixed

- Recruits return to siege towers along open drawbridges. The bridge no longer raises automatically while recruits are still on it.
- Siege engineers keep their firing target and turn the machine to aim. Move, attack and hold orders no longer compete with stale driving targets; recruits resume following after a siege task.
- Recruit operators can be selected through the machine to open their inventory.
- Fixed a crash when opening siege commands with no selected engine type.
- Rams follow attack and stand orders instead of attacking by default.
- Supply containers and dismantling check access permissions. Commanders can recall their crew from foreign machines, and carried ladders can be set up by the owner's side.
- Enemy siege engines only appear on the RTS map when scouted.
- Smoother aim and driving synchronization, including the local operator's predicted controls.
- Draft mounts collide with terrain during towing and follow ground height at their hitch position.
- Mangonel release timing follows the throwing arm. Incendiary pots render correctly when loaded and in flight.
- Restored staged construction for Mangonel and Trebuchet.
- Corrected operator hand positions and upper-body leaning poses.
- Shots continue through loaded chunks outside simulation distance instead of hanging in the air. Flight visuals follow the server's unclipped launch velocity.

## 0.1.0-beta.5

### Added

- Siege controls can now be issued by machine type from the Recruits command menu without looking at a machine.

### Fixed

- The crew command no longer sends every recruit in the selected groups to the same machine.
- Fixed a client crash with mods that replace living entity renderers.

## 0.1.0-beta.4

### Fixed

- Siege projectiles can now hit multipart targets such as the Ender Dragon.
- Bolts no longer stop in mid-air after killing a target they can penetrate.
- Bolts embedded in living targets now stay attached to the part they hit.

## 0.1.0-beta.3

### Changed

- Updated the required Axiomata version to 0.1.0-beta.2.

## 0.1.0-beta.2

### Changed

- Replaced the procedural maintenance panel with a Minecraft-style GUI.
- Returned Mons Meg to one centered draft mount and restored its previous towing distance.

## 0.1.0-beta.1

### Added

- Initial public beta.
