# CdrMoonProgression

Community dimension progression plugin untuk Moonsign. Target: Paper 1.21.11 / Java 21.

## Gameplay flow

1. Player mengumpulkan resource secara normal.
2. Player membuka `/progress`.
3. Pilih stage aktif lalu **Setor Kontribusi**.
4. Item deposit menambah personal contribution, global point, dan resource requirement terkait.
5. Stage hanya selesai jika target point global DAN seluruh resource requirement selesai.
6. Overworld complete -> The Nether terbuka.
7. Nether complete -> The End terbuka.

Mulai v0.3.0, block break tidak langsung menambah progression. Semua kontribusi dilakukan melalui GUI deposit.

## Fitur v0.7.0

- Global point progression: Overworld `50,000`, Nether `70,000` secara default.
- Full GUI melalui `/progress`.
- Contribution Deposit GUI: setor `1 / 16 / 64 / semua yang masih dibutuhkan`.
- Resource Requirements per stage.
- Requirement group dapat menerima beberapa material sekaligus, misalnya semua jenis log ke satu counter.
- Dimension unlock membutuhkan point target + seluruh resource requirements.
- Deposit tetap dapat dilakukan setelah point mencapai 100% jika resource requirement belum selesai.
- Deposit yang tidak lagi berguna otomatis ditolak agar item tidak terbuang.
- Requirement progress persisten di `data.yml`.
- First-run v0.7 migration dapat membangun requirement progress dari `history.yml`.
- Admin dapat melihat, mengoreksi, menambah, atau mereset requirement progress.
- Milestone 100% menunggu point + resource requirements selesai.
- Contribution History persisten di `history.yml`.
- Personal Contribution Goals dengan reward manual admin.
- Leaderboard contributor.
- Dimension lock untuk portal dan teleport.
- Optional PlaceholderAPI hook, termasuk requirement placeholders.

## Resource Requirements

Default Overworld:

```text
Iron Supply        3,000 Raw Iron
Gold Supply        1,000 Raw Gold
Diamond Reserve      100 Diamond
Timber Stockpile   5,000 Logs (gabungan semua jenis log)
```

Default Nether:

```text
Netherrack Stockpile  15,000
Soul Materials         3,000  (Soul Sand + Soul Soil)
Quartz Supply          3,000
Dark Stone Supply      3,000  (Blackstone + Basalt)
Bone Stockpile           500
Ancient Material          50 Ancient Debris
```

Semua target dapat diubah di `config.yml`.

Contoh requirement group:

```yaml
resource-requirements:
  enabled: true
  initialize-from-history: true

  overworld:
    timber_stockpile:
      name: '&a&lTimber Stockpile'
      icon: OAK_LOG
      target: 5000
      materials:
        - OAK_LOG
        - SPRUCE_LOG
        - BIRCH_LOG
        - JUNGLE_LOG
```

Satu item yang disetor dapat mengisi requirement group yang menerima material tersebut. Counter requirement berhenti di target; item tambahan masih dapat dipakai jika global point masih membutuhkan kontribusi.

### Migration dari versi lama

Saat pertama upgrade ke v0.7.0:

```yaml
resource-requirements:
  initialize-from-history: true
```

Plugin membaca `history.yml` dan menghitung kembali resource deposit yang tercatat sejak Contribution History tersedia.

Untuk deposit lama yang tidak tercatat di history, admin dapat mengoreksi manual:

```text
/progressadmin requirements status all
/progressadmin requirements set overworld iron_supply 2500
/progressadmin requirements add overworld timber_stockpile 500
/progressadmin requirements reset nether
```

## Personal Contribution Goals

Reward personal tetap manual oleh admin. Status goal:

```text
LOCKED -> PENDING REWARD -> REWARDED
```

Admin flow:

```text
/progressadmin goals pending all
/progressadmin goals reward <player> <stage> <goal>
/progressadmin goals unreward <player> <stage> <goal>
```

Plugin tidak memberikan item, money, key, permission, atau reward personal otomatis.

## Contribution History

```text
/progress history
/progress history all
/progress history overworld
/progress history nether
/progress history me
```

History menyimpan player, UUID, stage, material, jumlah item, contribution points, dan timestamp.

## Commands

Player:

- `/progress`
- `/progress overworld`
- `/progress nether`
- `/progress top [overworld|nether]`
- `/progress me`
- `/progress goals [overworld|nether]`
- `/progress milestones [overworld|nether]`
- `/progress history [all|overworld|nether|me]`

Admin:

- `/progressadmin status`
- `/progressadmin add <overworld|nether> <amount> [player]`
- `/progressadmin set <overworld|nether> <amount>`
- `/progressadmin reset <overworld|nether|all>`
- `/progressadmin unlock <nether|end>`
- `/progressadmin lock <nether|end>`
- `/progressadmin requirements status [overworld|nether|all]`
- `/progressadmin requirements set <stage> <requirement-id> <amount>`
- `/progressadmin requirements add <stage> <requirement-id> <amount>`
- `/progressadmin requirements reset <overworld|nether|all>`
- `/progressadmin goals pending [overworld|nether|all]`
- `/progressadmin goals reward <player> <stage> <goal>`
- `/progressadmin goals unreward <player> <stage> <goal>`
- `/progressadmin reload`
- `/progressadmin save`

Admin permission: `cdrmoonprogression.admin`

Dimension bypass permission: `cdrmoonprogression.bypass.dimension`

## PlaceholderAPI

Identifier: `cdrmoonprogression`

Existing point/personal placeholders tetap tersedia, ditambah:

```text
%cdrmoonprogression_overworld_requirements_complete%
%cdrmoonprogression_overworld_requirements_total%
%cdrmoonprogression_overworld_requirements_satisfied%
%cdrmoonprogression_nether_requirements_complete%
%cdrmoonprogression_nether_requirements_total%
%cdrmoonprogression_nether_requirements_satisfied%
%cdrmoonprogression_current_requirements_complete%
%cdrmoonprogression_current_requirements_total%
%cdrmoonprogression_current_requirements_satisfied%
```

## Build

```bash
mvn clean package
```

Output:

```text
target/CdrMoonProgression-0.7.0-SNAPSHOT.jar
```
