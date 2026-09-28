# CdrMoonProgression

Community dimension progression plugin untuk Moonsign. Target: Paper 1.21.11 / Java 21.

## Gameplay flow

1. Player mengumpulkan resource secara normal.
2. Player membuka `/progress`.
3. Pilih stage aktif lalu **Setor Kontribusi**.
4. Pilih resource yang tersedia di inventory.
5. Pilih jumlah setoran: `1`, `16`, `64`, atau `Semua`.
6. Item diambil dari inventory dan dikonversi menjadi progression point.
7. Setoran berhasil dicatat ke `history.yml` dan personal contribution bertambah.
8. Personal contribution dapat mencapai Personal Goals. Reward personal diberikan manual oleh admin.
9. Saat progress global melewati milestone, reward milestone global dapat dijalankan otomatis satu kali.
10. Overworld mencapai `50,000` -> The Nether terbuka.
11. Nether mencapai `70,000` -> The End terbuka.

Mulai v0.3.0, menghancurkan block tidak lagi langsung menambah progression.

## Fitur v0.6.0

- Full inventory GUI progression melalui `/progress`.
- Contribution Deposit GUI: setor `1 / 16 / 64 / semua`.
- Global progression dan personal contribution.
- Leaderboard contributor.
- Milestone Rewards global configurable.
- Contribution History persisten di `history.yml`.
- Personal Contribution Goals per stage.
- Default personal goal: `500 / 1,000 / 2,500 / 5,000 / 10,000` poin.
- Personal goal status: `LOCKED / PENDING REWARD / REWARDED`.
- Reward personal tidak pernah diberikan otomatis oleh plugin.
- Admin dapat melihat antrean reward personal yang pending.
- Admin menandai REWARDED setelah hadiah benar-benar diberikan manual.
- Reward state personal goal persisten di `data.yml`.
- Reset stage ikut mereset personal contribution dan personal reward state stage tersebut.
- Hanya stage aktif yang menerima setoran.
- Deposit otomatis dibatasi ketika target hampir selesai.
- Named/custom-model/PDC items tidak dianggap resource deposit vanilla.
- Auto unlock Nether dan End.
- Dimension lock untuk portal dan teleport.
- Optional PlaceholderAPI hook.

## Personal Contribution Goals

Player membuka GUI:

```text
/progress goals
/progress goals overworld
/progress goals nether
```

Status goal:

```text
LOCKED
  Belum mencapai target personal.

PENDING REWARD
  Target sudah tercapai, reward belum dikonfirmasi admin.

REWARDED
  Admin sudah memberikan reward manual dan menandai selesai.
```

Default config:

```yaml
personal-goals:
  enabled: true
  overworld:
    '500':
      name: '&aContributor I'
      icon: IRON_NUGGET
      lore:
        - '&7Kontribusi awal expedition.'
    '1000':
      name: '&eContributor II'
      icon: IRON_INGOT
  nether:
    '500':
      name: '&aNether Contributor I'
      icon: NETHER_BRICK
```

Tidak ada field `commands` pada personal goals karena reward sengaja dikelola manual oleh admin.

### Admin reward flow

Lihat semua reward yang belum diberikan:

```text
/progressadmin goals pending all
/progressadmin goals pending overworld
/progressadmin goals pending nether
```

Setelah admin memberikan hadiah manual:

```text
/progressadmin goals reward <player> <overworld|nether> <goal>
```

Contoh:

```text
/progressadmin goals reward Cadera overworld 2500
```

Jika salah menandai atau reward perlu dibuka kembali:

```text
/progressadmin goals unreward Cadera overworld 2500
```

Plugin tidak memberi item, money, key, permission, atau reward lain ketika personal goal tercapai. Plugin hanya melacak eligibility dan status administrasinya.

## Contribution History

Buka GUI history:

```text
/progress history
/progress history all
/progress history overworld
/progress history nether
/progress history me
```

Contoh entry:

```text
Cadera • Raw Iron
Stage: Overworld
Resource: 32x Raw Iron
Progress: +96 poin
28 Sep 2026 • 21:30
```

Konfigurasi:

```yaml
history:
  enabled: true
  max-entries: 1000
  timezone: 'Asia/Jakarta'
```

## Milestone Rewards

Milestone global tetap merupakan sistem terpisah dari personal goal. Command milestone dapat dijalankan otomatis sebagai console dan hanya satu kali.

Placeholder milestone tersedia: `{stage}`, `{stage_key}`, `{percent}`, `{current}`, `{target}`.

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
- `/progressadmin goals pending [overworld|nether|all]`
- `/progressadmin goals reward <player> <stage> <goal>`
- `/progressadmin goals unreward <player> <stage> <goal>`
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
target/CdrMoonProgression-0.6.0-SNAPSHOT.jar
```
