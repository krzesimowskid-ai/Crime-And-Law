# Crime And Law — symulacja balansu terytoriów v1

Status: **propozycja projektowa, nieaktywna w JAR-ze 0.1.15**.
Wyniki poniżej pochodzą z modelu; nie są testem gry ani gospodarki serwera Majestic.

## Punkt dochodowy — przykład referencyjny

Baza: 100,00 $ za godzinę kwalifikowanej aktywności. Nie jest to wypłata za sam upływ czasu.
Koszt poziomu 1 w tabeli wynosi 0,00 $, ponieważ zdobycie terenu jest osobną czynnością.
Kwoty skalują się z bazą danego źródła; nie są jedną ceną dla wszystkich obiektów.

| Poziom | Przychód / godzinę aktywności | Koszt tego ulepszenia | Min. kontroli łącznie | Zwrot z dodatkowego przychodu |
|---|---:|---:|---:|---:|
| 1 | 100,00 $ | 0,00 $ | 0 h | — |
| 2 | 115,00 $ | 60,00 $ | 2 h | 4,00 h |
| 3 | 130,00 $ | 120,00 $ | 6 h | 8,00 h |
| 4 | 145,00 $ | 240,00 $ | 14 h | 16,00 h |
| 5 | 160,00 $ | 360,00 $ | 30 h | 24,00 h |

Zwrot uwzględnia tylko różnicę przychodu po ulepszeniu, nie cały przychód obiektu.
Przy połowie aktywności czasy zwrotu wynoszą 8 / 16 / 32 / 48 godzin,
a przy podwójnej aktywności 2 / 4 / 8 / 12 godzin. Brak aktywności oznacza brak zwrotu.
Cena źródła, pełne wydatki operacyjne i popyt wymagają osobnego modelu gospodarki.

## Scenariusze obliczeniowe

- PASS — 1, 12 i 100 zwykłych lub bezczynnych chunków: 0,00 $ dochodu.
- PASS — 1–100 nakładających się źródeł jednego efektu: najwyżej mnożnik 1,60.
- PASS — Ten sam obrót rozdzielony na 1, 12 lub 100 terenów: identyczna wypłata.
- PASS — 350 105 małych naliczeń: brak strat lub nadpłat przez zaokrąglenia.
- PASS — Obniżki pozostawiają dodatni koszt/czas; +15% tempa nie oznacza −15% czasu.
- PASS — Zwrot kosztu kolejnego ulepszenia: 4 / 8 / 16 / 24 godziny przy ruchu referencyjnym.

To sprawdza model matematyczny. Zapobieganie powtórnym transakcjom, zapis ułamków,
autoryzacja, ocena terenu i wypłaty serwera nadal wymagają kodu oraz testów integracyjnych.
Nie należy traktować tej symulacji jako potwierdzenia ich działania w Minecrafcie.
