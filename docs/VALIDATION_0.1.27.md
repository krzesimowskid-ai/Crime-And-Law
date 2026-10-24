# Walidacja 0.1.27 — 24 października 2026

Wynik: **PASS**. Poprawka układu komunikatów w dopasowaniu logo i banera.

Na zgłoszonym zrzucie podgląd logo kończył się przy wysokości 272,
licząc od górnej krawędzi dialogu. Instrukcje były w wierszach 281 i 293,
a status zapisu/błędu w wierszu 291, przez co teksty się przecinały.

Status przeniesiono do prawej kolumny pod narzędziami kadrowania,
z zawijaniem i ograniczeniem wysokości przed dolnymi przyciskami.
Instrukcje pozostają pod podglądem.

Sprawdzono sześć widoków w rzeczywistym renderze Minecrafta:
logo bez komunikatu, przygotowanie logo, dłuższy błąd logo, przygotowanie
banera, przygotowanie logo przy skali GUI 4 i dłuższy błąd przy GUI 4.
Pozostałe widoki korzystały ze skali GUI 2. Stany komunikatów wymuszono
na potrzeby inspekcji wizualnej; nie jest to próba wywoływania błędów kodera.
Środowisko: Linux/Mesa/Xvfb, 1600×900. Tymczasowy sterownik podglądu
nie jest częścią repozytorium ani wydania.

`build`: sukces; 128 istniejących testów przechodzi. CRC archiwum i jego
15 bibliotek poprawne. Brak klas automatyzacji i podglądu w JAR-ze.
Protokół sieciowy pozostaje 16.

Plik: `crimeandlaw-0.1.27.jar`, 1917232 bajtów.

SHA-256: `390b92588c4cbae2b9c7b4c6faaced14c464eb296ac512b9af8f74e2e9e609a6`.
