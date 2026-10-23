# Weryfikacja 0.1.23

Minecraft 1.21.1, NeoForge 21.1.248, Java 21. Protokół 13 bez zmian.

## Zmiana

Baner zapisuje się w maks. 2048×512, obecny pas 2048×475. Logo nadal
2048×2048. Starsze obrazy 4K/8K są zmniejszane przy odczycie świata,
z zachowaniem proporcji, kadru, przezroczystych marginesów, koloru i krycia.
Migracja oznacza dane jako zmienione, więc zapis świata utrwala mniejszy PNG.
Klient pomniejsza również teksturę 8K odebraną od starszego serwera.
Dawne limity odczytu i transmisji zachowano dla zgodności z 0.1.22.

## Wyniki

- `build --offline`: BUILD SUCCESSFUL.
- 99 testów JUnit: 0 niepowodzeń, 0 błędów i 0 pominięć.
- Import zachowuje szczegóły większego źródła do kadrowania; wynik jest 2K.
  Przybliżony kadr 1:1 porównano z pikselami oryginału.
- Migracja zapisanego banera 8192×1901 daje 2048×475, zachowuje marginesy,
  kolor i krycie. Drugi odczyt nie wymaga kolejnej migracji.
- Zapis, odczyt i kodeki stanu zawierają szczegółowe logo i mniejszy baner.
- Prawdziwy Minecraft: `free23a`, 1920×1080, GUI 2 i 4, 13 klatek, PASS.
  Surowy starszy PNG 8192×1901 zostaje przed rejestracją tekstury zmniejszony
  do 2048×475. Sprawdzono faktyczne wymiary GPU, pełne mipmapy i stabilny
  szary obraz po pomniejszeniu jednopikselowej szachownicy.
- Ta sama próba potwierdza zachowanie swobodnej mapy: puszczenie myszy,
  ponowny gest, siatka, przybliżenie, zmiana GUI, spóźnione dane i wybieranie
  częściowo widocznych chunków, z porównaniem pikseli. Klient zakończył kodem 0.

Protokołu nie zmieniono; pełny test dwóch klientów i restartu z poprzedniego
wydania jest w [raporcie 0.1.22](VALIDATION_0.1.22.md). W 0.1.23 nie
uruchamiano ponownie próby dwóch klientów TCP.

## JAR

`crimeandlaw-0.1.23.jar`: 1841707 bajtów. Bez błędów CRC, powtórzonych wpisów
i klas automatyzacji. 97 klas produkcyjnych zgodnych bajt w bajt z kompilacją,
15 bibliotek JarJar, metadane wersji 0.1.23.

SHA-256: `d3f1a8529bb52bfc88818611848c59c328388dd03b8bb0e713fdb670a4a4b3a9`.
