# CdrMoonProgression

Community dimension progression plugin untuk Moonsign. Target: Paper 1.21.11 / Java 21.

## Gameplay flow

1. Player mengumpulkan resource secara normal.
2. Player membuka `/progress`.
3. Pilih stage aktif lalu **Setor Kontribusi**.
4. Pilih resource yang tersedia di inventory.
5. Pilih jumlah setoran: `1`, `16`, `64`, atau `Semua`.
6. Item diambil dari inventory dan dikonversi menjadi progression point.
7. Setoran berhasil dicatat ke `history.yml`.
8. Saat progress melewati milestone global, reward milestone dijalankan otomatis satu kali.
9. Overworld mencapai `50,000` -> The Nether terbuka.
10. Nether mencapai `70,000` -> The End terbuka.

Mulai v0.3.0, menghancurkan block tidak lagi langsung menambah progression.

## Fitur v0.5.0

- Full inventory GUI progression melalui `/progress`.
- Contribution Deposit GUI: setor `1 / 16 / 64 / semua`.
- Global progression dan personal contribution.
- Leaderboard contributor.
- Milestone Rewards `10% / 25% / 50% / 75% / 100%` yang configurable.
- Milestone reward command hanya dieksekusi satu kali dan persisten di `data.yml`.
- Contribution History tersimpan terpisah di `history.yml`.
- Setiap history menyimpan player, UUID, stage, resource, jumlah item, poin aktual, dan timestamp.
- GUI history paginated, 45 log per halaman.
- Filter history: `Semua / Overworld / Nether / Punyaku`.
- History retention configurable supaya file tidak tumbuh tanpa batas.
- Hanya setoran berhasil yang masuk history; command admin `add/set` tidak dipalsukan menjadi deposit player.
- Hanya stage aktif yang menerima setoran.
- Deposit otomatis dibatasi ketika target hampir selesai.
- Named/custom-model/PDC items tidak dianggap resource deposit vanilla.
- Auto unlock Nether dan End.
- Dimension lock untuk portal dan teleport.
- Optional PlaceholderAPI hook.

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

Entry terbaru disimpan paling atas. Ketika jumlah log melebihi `max-entries`, log paling lama dibuang otomatis.

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

Command milestone dijalankan sebagai console. Placeholder tersedia: `{stage}`, `{stage_key}`, `{percent}`, `{current}`, `{target}`.

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
- `/progress history [all|overworld|nether|me]`
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
target/CdrMoonProgression-0.5.0-SNAPSHOT.jar
```
