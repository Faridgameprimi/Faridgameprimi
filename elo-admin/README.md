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
- Atau build otomatis lewat GitHub Actions (lihat bagian di bawah) — tidak
  perlu install apa pun di komputer sendiri.

## Build otomatis lewat GitHub (tanpa install JDK/Maven)

Folder ini sudah menyertakan `.github/workflows/build.yml` yang akan
otomatis meng-compile jar-nya setiap kali kamu push ke GitHub.

1. Buat repo baru di GitHub — **pilih Private**, karena folder `lib/`
   berisi salinan `Neptune-2_3-1_21_11-d4be367.jar` (plugin berbayar/privat
   milik developer lain, dipakai hanya sebagai dependency compile). Jangan
   sebarkan file itu di repo publik.
2. Upload/push semua isi folder `elo-admin/` (termasuk `.github/`, `lib/`,
   `pom.xml`, `src/`) ke repo tersebut.
3. Buka tab **Actions** di repo GitHub-mu — workflow "Build EloAdmin" akan
   otomatis jalan (atau klik "Run workflow" kalau belum jalan sendiri).
4. Setelah selesai (biasanya 1-2 menit), buka hasil run tersebut, lalu di
   bagian **Artifacts** unduh `elo-admin-jar.zip`. Di dalamnya ada file
   `elo-admin-1.0.jar` — itu yang ditaruh ke folder `plugins/` server,
   bersama `Neptune-2_3-1_21_11-d4be367.jar` yang asli.
