# Walidacja 0.1.37 — 25 października 2026

Minecraft 1.21.1, NeoForge 21.1.248, Java 21. Protokół 19, schemat danych 4.

## Zmiana

Dodano zakładki „Ulepszenia” (ID 11) i „Kontrakty” (ID 12), dostępne
liderowi oraz pozostałym członkom gangu. Każda pokazuje własny tytuł
i panel „W przygotowaniu”. Mechaniki czekają na dalsze informacje.
Odstępy nawigacji dopasowują się do jej liczby pozycji, aby wszystkie
11 zakładek mieściło się nad ustawieniami i opuszczeniem gangu.

## Weryfikacja

- Końcowe `build --offline -x createMinecraftArtifacts -x downloadAssets`:
  BUILD SUCCESSFUL. Wykorzystano niezmienione oficjalne artefakty Minecrafta
  i NeoForge.
- 168 istniejących testów JUnit: zero niepowodzeń, błędów i pominięć.
  Zaktualizowano oczekiwaną listę widocznych kategorii.
- Prawdziwy renderer Minecrafta: osiem przypadków, lider i zwykły członek,
  1920×1080 przy GUI 2/4, 855×481 i 640×480 przy GUI 2.
  Sprawdzono faktyczny rozmiar framebufferu i skalę GUI po zmianie okna.
- Wszystkie 11 przycisków nawigacji jest widocznych, aktywnych, ma pełne
  etykiety i rozłączne pola kliknięcia. Ostatni przycisk pozostaje nad stopką.
- Obie nowe zakładki otwierano rzeczywistymi współrzędnymi myszy.
  Potwierdzono piksele komunikatu w obu panelach, klikanie „Ustawień”
  i powrót do zakładki „Gang”. Obejrzano zrzuty dużego oraz małego okna.
- CRC JAR-u i 15 dołączonych bibliotek: poprawne. Bez powtórzonych wpisów
  i bez klas sterowników graficznych. Metadane wydania: 0.1.37.

Kontrola renderera używa lokalnego modelu gangu, bez serwera i połączeń
rozgrywki. Nie stanowi ponownej weryfikacji finansów, captów ani czatu;
ich kod nie był zmieniany. Zrzuty oraz tymczasowy sterownik znajdują się
w roboczym katalogu `validation-037`, poza repozytorium.

`crimeandlaw-0.1.37.jar`: 2 124 612 bajtów.

SHA-256: `393287b8575336c76d0bfb55aea9fc0cf8471c6c2a3c840f6092ea2b00fcf79c`.
