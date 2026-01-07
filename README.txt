TempestBoss v1.4.0 (Multi-Arena)

MAIN IDEA
- You can run multiple boss fights at the same time in different arenas.
- Each arena has its own state machine (WAITING -> COUNTDOWN -> INTRO -> FIGHTING -> COOLDOWN).
- Every arena can be configured fully from /bossgui with categories.

QUICK START (GUI)
1) /bossgui
2) Arenas -> Create Arena
3) Select arena -> set Region (Sphere/Cuboid) -> set Spawn -> set Boss Type -> set Min Players
4) Set Trigger = ENTER
5) Enable arena

Now when enough players are inside, it starts countdown and spawns.

FILES
- plugins/TempestBoss/arenas.yml (arenas settings)
- plugins/TempestBoss/loot.yml (custom loot items by boss type)

COMMANDS
- /bossgui (admin only)
- /boss reload
- /boss arena create <name>

Notes
- iPhone Files app may hide .github; prefer PC for source uploading.
