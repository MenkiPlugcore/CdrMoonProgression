# CdrMoonProgression

Community dimension progression plugin untuk Moonsign. Target awal: Paper 1.21.11 / Java 21.

## Gameplay flow

1. Player mengumpulkan progression dari block natural di Overworld.
2. Saat global Overworld mencapai `50,000`, The Nether otomatis terbuka.
3. Progress berikutnya hanya dihitung dari resource Nether.
4. Saat global Nether mencapai `70,000`, The End otomatis terbuka.

Target dan nilai setiap block dapat diubah dari `config.yml`.

## Fitur v0.1.0

- Global progression per stage.
- Personal contribution per player.
- Configurable block point values.
- Auto unlock Nether dan End.
- Dimension lock untuk portal dan teleport.
- Anti exploit player-placed block yang persisten setelah restart.
- Tracking perpindahan placed block oleh piston.
- Cleanup marker saat block terbakar/meledak.
- `/progress` dan leaderboard `/progress top`.
- Admin control `/progressadmin`.
- Optional PlaceholderAPI hook.
- Auto-save progress + compact binary placement database.

## Commands

- `/progress`
- `/progress top [overworld|nether]`
- `/progressadmin status`
- `/progressadmin add <overworld|nether> <amount> [player]`
- `/progressadmin set <overworld|nether> <amount>`
- `/progressadmin reset <overworld|nether|all>`
- `/progressadmin unlock <nether|end>`
- `/progressadmin lock <nether|end>`
- `/progressadmin reload`
- `/progressadmin save`

Admin permission: `cdrmoonprogression.admin`

Dimension bypass permission: `cdrmoonprogression.bypass.dimension`

## PlaceholderAPI

Identifier: `cdrmoonprogression`

- `%cdrmoonprogression_stage%`
- `%cdrmoonprogression_current_progress%`
- `%cdrmoonprogression_current_target%`
- `%cdrmoonprogression_current_percent%`
- `%cdrmoonprogression_overworld_progress%`
- `%cdrmoonprogression_overworld_target%`
- `%cdrmoonprogression_overworld_percent%`
- `%cdrmoonprogression_nether_progress%`
- `%cdrmoonprogression_nether_target%`
- `%cdrmoonprogression_nether_percent%`
- `%cdrmoonprogression_personal_current%`
- `%cdrmoonprogression_personal_overworld%`
- `%cdrmoonprogression_personal_nether%`
- `%cdrmoonprogression_nether_unlocked%`
- `%cdrmoonprogression_end_unlocked%`

## Build

```bash
mvn clean package
```

Output:

```text
target/CdrMoonProgression-0.1.0-SNAPSHOT.jar
```
