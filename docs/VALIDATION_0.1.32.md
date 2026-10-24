# Walidacja 0.1.32 — 24 października 2026

Minecraft 1.21.1, NeoForge 21.1.248, Java 21. Protokół 19, schemat danych 4.

## Zakres zmiany

Mapa zachowuje wszystkie oryginalne kanały RGB; zmienia się wyłącznie alfa
na 176/255. Zachowano filtrowanie dwuliniowe, wskaźnik północy przy krawędzi
i strzałkę gracza. Usunięto szarą paletę wprowadzoną w 0.1.31.

Ikony sald zmniejszono z 22 do 14 jednostek, odstęp wierszy z 24 do 14,
a zwykły tekst z 1,08 do 0,70. Pozostają pełne kwoty. W banknocie dodano
wewnętrzną ramkę, a w karcie podziały chipa i górny detal. Układ uwzględnia
rzeczywistą szerokość tekstów oraz hotbar, statystyki i nieaktywny czat.

Nad kwotami: HH:mm, dd.MM.rrrr, polski dzień tygodnia. Czas i strefa pochodzą
z serwera. Osobny pakiet ma mniej niż 128 bajtów dla zwykłych stref; jest
wysyłany co około 30 sekund, po zmianie strefy i przy loginie/respawnie/zmianie
wymiaru. Monotoniczny licznik klienta podtrzymuje zegar między pakietami.
Formatowanie jest buforowane do zmiany minuty lub nowej synchronizacji.
Podzbiór DejaVu Sans Bold obejmuje polskie nazwy dni; licencja dołączona.

Temperatura i pogoda nie zostały wdrożone; użytkownik wskazał je na później.

## Sprawdzenia

- 157 testów JUnit: granice układu, zachowane barwy i alfa, północ na obrzeżu,
  północ kalendarzowa w strefie Warszawy, polska data i dzień tygodnia,
  przejście na czas zimowy 25 października 2026, resynchronizacja i reset.
- Kodek zegara zachowuje datę/strefę i odrzuca nieprawidłowe wartości.
- Dotychczasowe testy finansów i zapisu, w tym 10 000 losowych operacji,
  pozostają częścią pełnego zestawu.

Wyniki sieciowe, kontrola rzeczywistego renderera oraz suma kontrolna JAR-u
są zapisane w `validation-0.1.32.json`. Sterowniki testów z `src/financeTest`
pozostają wyłączone z normalnego wydania.

## Rzeczywisty renderer i plik wydania

Dwa klienty TCP i osobny serwer: PASS. Obejrzano zrzuty 1920×1080, GUI 2/4,
zwykłych i maksymalnych sald oraz czterech kierunków. Mapa zachowuje kolory
i przezroczystość, a małe liczniki oraz polska data/dzień są czytelne.
Test pikseli potwierdza widoczność banknotu i karty przy mniejszym rozmiarze.
Zmiana strefy Europe/Warsaw → Pacific/Auckland → Europe/Warsaw dotarła do
działającego HUD-u bez otwierania menu. Test serwera i restartu zachował
salda, historie i członkostwo. Wartości podglądu klienta nie zasilały kont.

Końcowe `build --offline -x createMinecraftArtifacts -x downloadAssets`:
BUILD SUCCESSFUL. Wykorzystano gotowe, niezmienione artefakty Minecrafta /
NeoForge 21.1.248 z poprzedniego wydania i lokalne zasoby renderera.
157 testów: zero niepowodzeń, błędów i pominięć. CRC JAR-u oraz 15 bibliotek
w środku poprawne. Brak klas automatyzacji; czcionka HUD i licencja dołączone.

`crimeandlaw-0.1.32.jar`: 2 020 038 bajtów.

SHA-256: `f40011013f84e61dbfc70fab1cc3a45311dee4b1f7b90c3ba6cba076a99eb800`.
