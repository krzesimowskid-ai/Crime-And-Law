# Walidacja 0.1.34 — 24 października 2026

Minecraft 1.21.1, NeoForge 21.1.248, Java 21. Protokół 19, schemat danych 4.

## Zmiana

Przy małym oknie wyśrodkowany pasek captu przecinał kolumnę wiadomości.
Układ rezerwuje teraz całą szerokość czatu wraz z tłem i przewijaniem.
Panel przesuwa się w prawo i zwęża do dostępnej przestrzeni. Nagłówek
i status dopasowują skalę, więc długi tekst nie jest urywany.

Otwarty czat zachowuje widoczność i pozycję captu. Ukryty czat nie rezerwuje
przestrzeni. Dotychczasowe reguły F1 i pozostałych menu pozostają.

## Wyniki

- `build --offline -x createMinecraftArtifacts -x downloadAssets`: BUILD SUCCESSFUL.
  Wykorzystano niezmienione oficjalne artefakty Minecrafta i NeoForge.
- 162 testy JUnit: zero niepowodzeń, błędów i pominięć.
- Nowe testy obejmują 144 kombinacje rozmiaru GUI, otwartego/zamkniętego
  czatu, jego szerokości i skali, regresję z okna użytkownika i ukryty czat.
- Rzeczywisty renderer: siedem przypadków przy 855×481, 640×480
  i 1920×1080, GUI 2/4, otwarty/zamknięty/ukryty czat, trzy fazy captu
  i długi status wyeliminowanego uczestnika. Każdy przypadek: PASS.
- Sprawdzono piksele całego górnego obramowania captu i obu ikon sald.
  Obejrzano zrzuty małych okien oraz GUI 4: panele są rozdzielone, mapa,
  salda i zegar są widoczne podczas pisania.
- CRC JAR-u i 15 zagnieżdżonych bibliotek: poprawne. Brak klas sterowników
  testów w wydaniu. Oba fonty, licencja i pięć miksinów klienta obecne.

Sterownik `src/hudTest` uruchamia prawdziwe okna klienta połączonego przez
TCP z lokalnym serwerem. Dane do wyświetlania captu są lokalną próbką;
ten test dotyczy renderowania, nie powtarza sieciowej rozgrywki captu.
Pierwsze powiadomienia samouczka są czyszczone wyłącznie przez sterownik,
aby nie zasłaniały badanych pikseli. Nie zmienia to zachowania wydania.

Wyniki: `validation-0.1.34.json`. Zrzuty i logi lokalne:
`validation-034/final-render` w katalogu roboczym rozmowy.

`crimeandlaw-0.1.34.jar`: 2 122 623 bajty.

SHA-256: `268e47b34bc2c1abbeff715ea0efdb3f8e32c618d2b0f7034f1147aa511f9aea`.
