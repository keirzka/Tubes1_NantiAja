# README — Tubes1_NantiAja
## IF2211 Strategi Algoritma — Pemanfaatan Algoritma Greedy dalam Pembuatan Bot Permainan Battlecode 2025

---

## Deskripsi Singkat

Repositori ini berisi implementasi tiga bot permainan Battlecode 2025 menggunakan algoritma **Greedy** berbasis bahasa pemrograman Java. Setiap bot dirancang dengan strategi greedy yang berbeda untuk memenangkan permainan dengan cara mewarnai peta dan menghancurkan robot serta menara lawan.

---

## Penjelasan Algoritma Greedy per Bot

### Bot Utama — Fokus: Penghancuran Tower Musuh
Bot utama mengimplementasikan strategi greedy yang agresif dengan memprioritaskan penghancuran tower musuh secara langsung. Tower hanya memproduksi dua jenis robot: **Soldier** dan **Mopper** dengan rasio 2:1.
- **Soldier**: Secara greedy memilih tower musuh terdekat sebagai target utama. Jika tidak ada tower musuh yang terdeteksi, robot menuju ruin terdekat untuk dibangun menjadi tower baru. Jika tidak ada keduanya, robot bergerak acak sambil mewarnai petak.
- **Mopper**: Secara greedy memprioritaskan penghapusan cat musuh yang menghalangi pembangunan tower pada ruin, kemudian menyerang robot musuh untuk mencuri cat mereka.
- Tower hanya membangun **Money Tower** dan **Paint Tower** dengan rasio 3:2, serta secara otomatis menyerang robot musuh dalam jangkauan.

### Bot Alternatif 1 — Fokus: Penguasaan Wilayah
Bot ini mengimplementasikan greedy berbasis sistem **scoring** untuk memaksimalkan luas wilayah yang diwarnai. Tower memproduksi robot dengan komposisi Splasher 70%, Soldier 20%, Mopper 10%.
- **Splasher**: Mengevaluasi setiap tile yang dapat diserang dengan skor berdasarkan kondisi (tile bertanda untuk tower = +6, wilayah musuh = +5, wilayah kosong = +3, wilayah sekutu = +1), lalu memilih target dengan skor tertinggi. Pergerakan juga ditentukan berdasarkan skor arah terbaik.
- **Soldier**: Menggunakan fungsi evaluateRuin() untuk memilih ruin terbaik berdasarkan jumlah wilayah sekutu, kosong, dan musuh di sekitarnya. Membangun tower dan menandai pola SRP.
- **Mopper**: Membersihkan cat musuh terdekat, mentransfer cat ke robot sekutu yang kekurangan, dan melakukan mop swing terhadap robot musuh.

### Bot Alternatif 2 — Fokus: Pengelolaan Sumber Daya
Bot ini mengimplementasikan greedy untuk menjaga ketersediaan sumber daya sekaligus membangun tower dan SRP secara efisien.
- **Tower**: Mengatur produksi robot berdasarkan tiga fase. Mengirim broadcast lokasi ruin yang tersedia ke robot sekutu. Melakukan upgrade jika chips > 2000.
- **Soldier**: Membangun tower sebagai prioritas utama, mengerjakan SRP, membersihkan cat musuh di area penting, dan bereksplorasi sambil mengecat map.
- **Mopper**: Menjaga SRP dan ruin aktif dari ancaman cat musuh, mendukung pengisian cat robot sekutu.
- **Splasher**: Mencari target splash strategis (dekat ruin/SRP atau banyak cat musuh), melakukan coverage splash jika cat cukup, dan bereksplorasi jika tidak ada target.

---

## Requirements

- **Java Development Kit (JDK)**: Versi 8 atau lebih baru
- **Gradle**: Sudah disertakan dalam proyek melalui Gradle Wrapper (`gradlew`)

---

## Instalasi

1. Clone repositori ini:
   ```bash
   git clone https://github.com/keirzka/Tubes1_NantiAja.git
   cd Tubes1_NantiAja
   ```

2. Pastikan JDK sudah terinstal dan dapat diakses melalui terminal:
   ```bash
   java -version
   ```

---

## Cara Compile dan Menjalankan Program

### Compile (Build)

```bash
# Windows
.\gradlew build

# macOS / Linux
./gradlew build
```

### Menjalankan Pertandingan via Client
1. Buka **Battlecode Client**.
2. Pilih menu **Game** → **Queue**.
3. Pilih bot yang ingin dipertandingkan (misal: `mainRobot` vs `alternativebot1`).
4. Pilih peta yang diinginkan (misal: `DefaultSmall` atau `DefaultLarge`).
5. Klik **Run** untuk memulai simulasi pertandingan.

### Menjalankan Pertandingan via Command Line
```bash
# Windows
.\gradlew run

# macOS / Linux
./gradlew run
```
---

## Author

| NIM | Nama |
|---|---|
| 13524073 | Keisha Rizka Syofyani |
| 13524087 | Muhammad Fakhry Zaki |
| 13524109 | Helena Kristela Sarhawa |
