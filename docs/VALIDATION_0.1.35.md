# Walidacja 0.1.35 — 24 października 2026

Minecraft 1.21.1, NeoForge 21.1.248, Java 21. Protokół 19, schemat danych 4.

## Zmiana

Wiadomości przestawały być widoczne po około dziesięciu sekundach.
Zakończenie pisania dodatkowo zmniejszało liczbę widocznych wierszy
i przesuwało tekst przez ukrycie nagłówka.

Wiadomości nie wygasają z upływem czasu. Oba stany mają wspólną wysokość
i współrzędne historii. Cały panel przechodzi płynnie w ciągu 200 ms do
58% ustawionej nieprzezroczystości. Zębatka otwiera natywne ustawienia
z jednym suwakiem wysokości. Pusty zamknięty czat nie rysuje panelu.

Zachowano limity historii, natywne usuwanie i czyszczenie wiadomości,
podpisy, transport i komendy oraz poprawkę odstępu od captu z 0.1.34.

## Wyniki

- `build --offline -x createMinecraftArtifacts -x downloadAssets`: BUILD SUCCESSFUL.
  Użyto niezmienionych oficjalnych artefaktów Minecrafta i NeoForge.
- 166 testów JUnit: zero niepowodzeń, błędów i pominięć. Nowe testy sprawdzają
  długi stan nieaktywny, płynne odwrócenie przejścia, zachowanie kolorów
  i identyczny układ w 24 kombinacjach rozmiaru, wysokości i odstępów.
- Siedem rzeczywistych przypadków okna: 855×481, 640×480 i 1920×1080,
  GUI 2/4, otwarty/zamknięty/ukryty czat i trzy fazy captu: PASS.
- Po Escape wszystkie 11 wierszy pozostało w tych samych miejscach.
  Po co najmniej 12 rzeczywistych sekundach i 240 tickach GUI liczba
  zabarwionych pikseli tekstu w każdym wierszu była identyczna jak tuż
  po przejściu w stan nieaktywny. Kontrast próby spadł ze 168 do 108
  po zamknięciu pisania i pozostał na 108 po oczekiwaniu.
- Przeszły: zachowanie oryginalnej historii podczas oczekiwania, natywne
  usunięcie wiadomości, czyszczenie historii i brak pustego panelu,
  klikany komponent, Tab do `/help`, powrót z ustawień ze szkicem,
  wspólna wysokość i brak interakcji tekstu podczas zwykłej rozgrywki.
- Obejrzano zrzuty otwartego czatu, historii po oczekiwaniu, powrotu
  z ustawień i pustej historii. Capt, mapa, salda i zegar pozostają widoczne.
- CRC JAR-u i 15 zagnieżdżonych bibliotek: poprawne. Brak sterowników
  graficznych w wydaniu. Oba fonty, licencja i pięć miksinów obecne.

Test używa prawdziwego renderera i klienta połączonego przez TCP z lokalnym
serwerem. Wiadomości oraz stan captu do kontroli wyglądu są lokalnymi
próbkami. Sprawdzenie usuwania używa lokalnej sygnatury i natywnej metody
ChatComponent, nie pakietu usuwania wysłanego z serwera. Nie jest to nowy
pełny test rozgrywki captu ani ekonomii.

Wyniki i pomiary: `validation-0.1.35.json`. Zrzuty i logi lokalne:
`validation-035/final-render` w katalogu roboczym rozmowy.

`crimeandlaw-0.1.35.jar`: 2 124 311 bajtów.

SHA-256: `e96e0fc3d6fdbe81b381517fdb259c894f3e2539d3a5fdb3279705a31fa13a74`.
