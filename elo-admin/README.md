# EloAdmin — addon untuk Neptune

Plugin tambahan (bukan modifikasi Neptune.jar) yang menambahkan command:

```
/elo add <player> <jumlah>     -> menambah Elo
/elo remove <player> <jumlah>  -> mengurangi Elo (minimal 0)
/elo set <player> <jumlah>     -> mengatur Elo langsung
```

Permission: `neptune.elo.admin` (default: OP)

Catatan: pemain target harus **online**, karena NeptuneAPI mengambil data
profile lewat cache pemain yang sedang bermain.

## Kenapa ini file terpisah, bukan langsung ditempel ke Neptune.jar?

Neptune.jar adalah plugin closed-source yang sudah dikompilasi (bytecode),
dan menyediakan API publik resmi (`dev.lrxh.api.*`) untuk hal-hal seperti
ini. Menulis addon lewat API resmi jauh lebih aman dan tidak akan rusak
saat Neptune update, dibanding membongkar/menyuntik ulang jar aslinya.

## Cara build

Butuh **JDK 17+** dan **Maven** terpasang di komputermu.

1. Pastikan file `lib/Neptune-2_3-1_21_11-d4be367.jar` ada (sudah disertakan,
   itu salinan dari jar yang kamu upload — hanya dipakai saat compile, tidak
   ikut ke dalam jar hasil build).
2. Buka terminal di folder ini, lalu jalankan:
   ```
   mvn clean package
   ```
3. Hasil jar ada di `target/elo-admin-1.0.jar`.

## Cara pasang di server

1. Copy `Neptune-2_3-1_21_11-d4be367.jar` (asli) dan `elo-admin-1.0.jar`
   ke folder `plugins/` server Paper kamu.
2. Restart server.
3. Coba: `/elo add NamaPlayer 50`

## Kalau tidak punya JDK/Maven

- Pakai IDE seperti IntelliJ IDEA (Community, gratis) — buka folder ini
  sebagai Maven project, lalu klik "Package" di menu Maven.
- Atau minta developer/temanmu yang biasa bikin plugin Spigot/Paper untuk
  build-kan sekali saja — filenya sudah lengkap tinggal `mvn package`.
