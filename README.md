
# Kwitansi Digital

Berguna untuk pembuatan Kwitansi melalui device berbasis Android dan dapat dicetak menggunakan Printer Thermal.


## FAQ

#### Dapat Menggunakan Printer Apa Saja?

Kita baru test menggunakan printer Iware dengan ID RPP02N, seharusnya printer thermal berbasis bluetooth lainnya dapat digunakan dengan baik.

#### Bagaimana Cara Print?

Untuk saat ini aplikasi ini masih menggunakan metode selectFirstPaired() dari library [DantSu | ESCPOS-ThermalPrinter-Android](https://github.com/DantSu/ESCPOS-ThermalPrinter-Android) yang mana aplikasi ini akan mengkoneksikan dengan device bluetooth yang dipairing pertama kali pada smartphone. Jika pada smartphone anda sudah terpairing banyak bluetooth device, unpair semua devicenya lalu setelah itu pair printer thermal anda.

#### Dimana Saya Dapat Menemukan File PDF Kwitansi?

Pada direktori Downloads di penyimpanan internal.

#### Dimana Saya Dapat Menemukan File Backup Database?

Pada direktori Downloads di penyimpanan internal, file backup berupa file CSV.

#### Bagaimana Cara Restore Database?

Pastikan anda menyimpan file database berupa CSV dengan format nama "RoomDB_Backup.csv" pada direktori Downloads, setelah itu tinggal tekan tombol Restore Database.


## Fitur

- Room Database
- Save Kwitansi
- Print Kwitansi
- Save As PDF
- Backup dan Restore Database


## To Add

- Update Kwitansi
- Share Kwitansi ke Social Media
- Generate Random ID
- Icon dan Poster
