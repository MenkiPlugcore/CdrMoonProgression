# CdrMoonProgression

Community dimension progression plugin untuk Moonsign. Target: Paper 1.21.11 / Java 21.

## Gameplay flow

1. Player mengumpulkan resource secara normal.
2. Player membuka `/progress`.
3. Pilih stage aktif lalu **Setor Kontribusi**.
4. Pilih resource yang tersedia di inventory.
5. Pilih jumlah setoran: `1`, `16`, `64`, atau `Semua`.
6. Item diambil dari inventory dan dikonversi menjadi progression point.
7. Saat progress melewati milestone global, reward milestone dijalankan otomatis satu kali.
8. Overworld mencapai `50,000` -> The Nether terbuka.
9. Nether mencapai `70,000` -> The End terbuka.

Mulai v0.3.0, menghancurkan block tidak lagi langsung menambah progression.

## Fitur v0.4.0

- Full inventory GUI melalui `/progress`.
- Contribution Deposit GUI.
- Resource selector per stage.
- Setor 1 / 16 / 64 / semua.
- Global progression per stage.
- Personal contribution per player.
- Leaderboard contributor.
- Deposit value configurable dari `config.yml`.
- Milestone Rewards configurable per stage.
- Default milestone `10% / 25% / 50% / 75% / 100%`.
- Reward milestone menjalankan console commands tepat satu kali.
- Broadcast/title/subtitle milestone configurable.
- Milestone state persisten di `data.yml`, sehingga restart tidak menggandakan reward.
- GUI status milestone: `PENDING / REACHED / REWARDED`.
- `/progress milestones [overworld|nether]` untuk membuka Milestone GUI.
- Reset stage juga mereset milestone claim untuk stage tersebut.
- Migration aman dari v0.3: milestone yang sudah terlewati tidak otomatis membagikan reward lama secara default.
- Hanya stage aktif yang dapat menerima setoran.
- Deposit otomatis dibatasi agar tidak mengambil item berlebihan saat target hampir selesai.
- Named/custom-model/PDC items tidak dianggap resource deposit vanilla.
- Auto unlock Nether dan End.
- Dimension lock untuk portal dan teleport.
- Optional PlaceholderAPI hook.
- Data progression tetap persisten pada `data.yml`.

## Milestone Rewards

Konfigurasi milestone berada di:

```yaml
milestones:
  reward-existing-progress-on-first-load: false
  overworld:
    '25':
      name: '&eSupply II'
      icon: IRON_INGOT
      lore:
        - '&7Seperempat expedition selesai.'
      broadcast: '&eMilestone Overworld 25% tercapai!'
      title: ''
      subtitle: ''
      commands:
        - 'give @a minecraft:diamond 1'
```

Command dijalankan sebagai console. Placeholder yang dapat digunakan:

- `{stage}`
- `{stage_key}`
- `{percent}`
- `{current}`
- `{target}`

`reward-existing-progress-on-first-load: false` direkomendasikan saat upgrade server live. Progress lama yang sudah melewati milestone akan ditandai sebagai rewarded tanpa menjalankan reward secara mendadak.

## Default deposit resources

### Overworld

- Coal
- Raw Copper
- Raw Iron
- Raw Gold
- Redstone
- Lapis Lazuli
- Emerald
- Diamond
- Overworld logs

### Nether

- Netherrack
- Soul Sand
- Soul Soil
- Quartz
- Gold Nugget
- Blackstone
- Basalt
- Bone Block
- Glowstone Dust
- Ancient Debris

Semua nilai poin dapat diubah pada `progression.<stage>.deposit-items` di `config.yml`.

## Commands

- `/progress`
- `/progress overworld`
- `/progress nether`
- `/progress top [overworld|nether]`
- `/progress me`
- `/progress milestones [overworld|nether]`
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
target/CdrMoonProgression-0.4.0-SNAPSHOT.jar
```
