# Walidacja 0.1.38 — 25 października 2026

Minecraft 1.21.1, NeoForge 21.1.248, Java 21. Protokół 19, schemat danych 4.

## Zmiana

Użytkownik poprosił o przewijanie zakładek zamiast zmniejszania odstępów.
Nawigacja ponownie ma przyciski wysokości 19 jednostek w odstępach 24
jednostek. Ma własny stan przewijania oraz pasek po prawej. „Ustawienia”
i „Opuść gang” pozostają poza przewijaną listą. Pozycja jest zachowana
po przełączeniu zakładki, odświeżeniu danych i przebudowie GUI.

## Weryfikacja

- Końcowe `build -x createMinecraftArtifacts -x downloadAssets`: BUILD
  SUCCESSFUL. Użyto niezmienionych oficjalnych artefaktów Minecrafta
  i NeoForge.
- 168 istniejących testów JUnit: zero błędów, niepowodzeń i pominięć.
- Prawdziwy renderer Minecrafta, osiem przypadków: lider oraz zwykły
  członek, 1920×1080 przy GUI 2/4, 855×481 i 640×480 przy GUI 2.
  Rzeczywisty rozmiar framebufferu oraz skalę GUI sprawdzano po zmianie okna.
- Sprawdzono przewijanie kółkiem i jego ograniczenia, kliknięcie toru,
  przeciąganie paska, Home/End i strzałkę po zaznaczeniu paska.
- Przełączono „Ulepszenia” i „Kontrakty” przez rzeczywiste kliknięcia.
  Przełączenie, nowy snapshot i przebudowa GUI zachowują pozycję listy.
- Ukryte zakładki nie przechwytują kliknięć stopki. Ustawienia są dostępne
  przy początku i końcu listy, a obie akcje stopki mają stałą pozycję.
- Kółko nad listą rang nie przesuwa nawigacji, a kółko nad nawigacją
  nie przesuwa listy rang. Klawisze zaznaczonego paska nawigacji nie
  przesuwają kamery mapy.
- Obejrzano zrzuty początku listy przy 1920×1080 i końca przy 640×480.
  Etykiety mieszczą się w przyciskach, widoczne wiersze są nad stopką.
- CRC JAR-u oraz 15 dołączonych bibliotek: poprawne. Metadane: 0.1.38.
  Brak powtórzonych wpisów oraz klas sterownika graficznego w wydaniu.

Kontrola renderera używa lokalnego modelu gangu, bez serwera rozgrywki.
Nie jest ponowną weryfikacją sieci, finansów, captów ani czatu, których
kod nie był zmieniany. Tymczasowy sterownik, zrzuty i końcowy raport
znajdują się w roboczym katalogu `validation-038`, poza repozytorium.

`crimeandlaw-0.1.38.jar`: 2 125 005 bajtów.

SHA-256: `fcc826b5234322661899d93e1fe54b8760b467d1c293cd94d4c8e2e92255253b`.
