# CdrMoonProgression

Stable dimension progression plugin untuk Moonsign.
Target: Paper 1.21.11 / Java 21.

## Core v1.0.0

Fokus utama plugin:

1. **Dimension Lock** — Nether dan The End diblokir sampai stage progression selesai.
2. **Global Progression** — player mengumpulkan resource lalu setor lewat `/progress`.
3. **Personal Contribution** — kontribusi setiap player tetap dicatat secara terpisah.
4. **Leaderboard** — ranking contributor per stage.

Flow utama:

```text
Kumpulkan resource
        ↓
/progress → Setor Kontribusi
        ↓
Global progression naik
        ↓
Personal contribution tercatat
        ↓
Leaderboard diperbarui
        ↓
Target stage selesai
        ↓
Dimension unlock
```

Default progression:

```text
Overworld → 50,000 point → unlock The Nether
Nether    → 70,000 point → unlock The End
```

Block break tidak langsung menambah progress. Semua kontribusi masuk melalui GUI deposit.

## `/progress`

Player cukup memakai:

```text
/progress
```

GUI menyediakan:

- status Overworld / Nether
- global progress
- deposit resource
- personal contribution
- leaderboard
- status dimension lock

Fitur tambahan yang sudah tersedia tetap dapat dipakai bila dibutuhkan staff:

- Resource Requirements
- Global Milestones
- Personal Contribution Goals (reward manual admin)
- Contribution History
- PlaceholderAPI

## Deposit

Player dapat memilih:

```text
Setor 1
Setor 16
Setor 64
Setor Semua
```

Named item, CustomModelData, dan item dengan PDC tidak dianggap resource vanilla deposit.
Plugin juga membatasi jumlah deposit agar item tidak terbuang ketika target hampir selesai.

## Resource Requirements

Resource Requirements adalah layer tambahan dan dapat dimatikan:

```yaml
resource-requirements:
  enabled: false
```

Jika aktif, dimension baru terbuka setelah target point DAN seluruh requirement selesai.

## Personal Contribution & Leaderboard

Setiap deposit mencatat kontribusi player sendiri meskipun progression global bersifat satu server.
Leaderboard tersedia per stage melalui GUI atau:

```text
/progress top overworld
/progress top nether
```

## Dimension Lock

Selama belum unlocked, akses ke Nether / End melalui portal atau teleport akan ditolak.

Permission bypass admin:

```text
cdrmoonprogression.bypass.dimension
```

## Admin Essentials

```text
/progressadmin status
/progressadmin add <overworld|nether> <amount> [player]
/progressadmin set <overworld|nether> <amount>
/progressadmin reset <overworld|nether|all>
/progressadmin unlock <nether|end>
/progressadmin lock <nether|end>
/progressadmin reload
/progressadmin save
```

Permission admin:

```text
cdrmoonprogression.admin
```

Command tambahan untuk Resource Requirements dan Personal Goals tetap tersedia bila fitur tersebut dipakai.

## Data

Progression state disimpan persisten sehingga restart server tidak mereset progress, contribution, leaderboard, unlock state, requirement state, milestone state, atau manual reward state.

Contribution History disimpan terpisah pada `history.yml`.

## PlaceholderAPI

PlaceholderAPI bersifat optional. Identifier:

```text
cdrmoonprogression
```

Placeholder tersedia untuk global point, percent, personal contribution, dimension unlock, dan Resource Requirements.

## Build

```bash
mvn clean package
```

Output stable:

```text
target/CdrMoonProgression-1.0.0.jar
```
