# Walidacja 0.1.36 — 24 października 2026

Minecraft 1.21.1, NeoForge 21.1.248, Java 21. Protokół 19, schemat danych 4.

## Zmiana

Przy małym oknie zegar i salda odrywały się od minimapy. Zbyt szeroki
margines hotbara uruchamiał dawny układ, który podnosił kolumnę nad
waniliowe statystyki, pozostawiając ją po prawej stronie i daleko od mapy.

Zwykłe kwoty mieszczą się teraz obok mapy przy 855×481. Przy braku miejsca
cała kolumna przechodzi bezpośrednio nad mapę przy jej lewej krawędzi,
z odstępem czterech jednostek. Granicę wyznacza rzeczywisty hotbar oraz
jedna jednostka zapasu na cień. Pomiar kwot odpowiada faktycznej skali
rysowanego tekstu, także dla długich liczb.

## Wyniki

- `build --offline -x createMinecraftArtifacts -x downloadAssets`: BUILD SUCCESSFUL.
  Użyto niezmienionych oficjalnych artefaktów Minecrafta i NeoForge.
- 168 testów JUnit: zero niepowodzeń, błędów i pominięć. Nowe testy obejmują
  zgłoszony rozmiar okna, szerokie kwoty, wąskie układy, przyleganie do mapy
  i pozostawanie po lewej stronie waniliowego hotbara.
- Dziewięć rzeczywistych przypadków renderera przy 855×481, 640×480
  i 1920×1080, GUI 2/4: PASS. Faktyczna skala GUI jest sprawdzana po zmianie
  rozmiaru okna; wyniki zawierają także rzeczywiste współrzędne HUD-u.
- Dla 148 840 $ gotówki i 0 $ banku przy 855×481: kolumna obok mapy,
  dolne krawędzie wyrównane. Dla maksymalnych 1 000 000 000 000 $ na obu
  kontach oraz przy braku miejsca: kolumna bezpośrednio nad mapą po lewej.
- Sprawdzono piksele obu ikon sald i paska captu, otwarty/zamknięty/ukryty
  czat oraz trzy fazy captu. Obejrzano małe okno, układ 640×480 i obie
  próby z maksymalnymi kwotami. Pełne kwoty pozostają czytelne.
- Powtórzony test trwałego czatu: 11 wierszy pozostaje po Escape
  i co najmniej 12 rzeczywistych sekundach / 240 tickach GUI. Przeszły
  natywne usuwanie i czyszczenie, ustawienia ze szkicem, klikany komponent
  i podpowiadanie komendy przez Tab.
- CRC JAR-u i 15 zagnieżdżonych bibliotek: poprawne. Brak klas sterowników
  graficznych w wydaniu. Oba fonty, licencja i pięć miksinów obecne.

Test używa prawdziwego renderera i klienta połączonego przez TCP z lokalnym
serwerem. Salda, wiadomości i stan captu do kontroli wyglądu są lokalnymi
próbkami; test nie jest nowym pełnym sprawdzeniem ekonomii ani rozgrywki
captu. Poprawki czatu z 0.1.35 są zachowane.

Wyniki: `validation-0.1.36.json`. Zrzuty i logi lokalne:
`validation-036/final-render` w katalogu roboczym rozmowy.

`crimeandlaw-0.1.36.jar`: 2 124 253 bajty.

SHA-256: `ff2745c7818824dc7994aa5cec594c0a337469b06d89ffb39a3fe597a7d3d3c3`.
