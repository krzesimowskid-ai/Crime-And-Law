# Weryfikacja 0.1.48 — ręczny obrót i worek ziemi

Minecraft 1.21.1, NeoForge 21.1.248, Java 21. Protokół 26 na wszystkich
czterech rejestratorach; ten sam JAR na serwerze i u graczy.

## Zmiana

- LPM chwyta, mysz przesuwa worek/konewkę. A/D zmienia przechył boczny,
  W/S wzdłużny. Brak automatycznego odwracania i powrotu po ponownym chwycie.
- Obie osie ±180°, z ograniczoną szybkością i kontrolą zakresów serwera.
  Sam W lub S wystarcza do odwrócenia worka. Puszczenie LPM zatrzymuje emisję.
- Piętnaście źródeł ziemi w siatce 5×3 obejmuje rzeczywisty otwór modelu.
  Transformacja położenia i obrotu jest wspólna z serwerem. Tylko trafiająca
  część strumienia daje postęp; koszt pozostaje jednym użyciem worka.
- Usunięto wystającą obróconą wstęgę i odstające płaskie etykiety. Etykieta
  podąża za załamaniami worka; UV materiału jest ciągłe na kolejnych pasach.
  Własna drobna faktura zamiast powtarzających się szerokich pasków.
- Zaktualizowano instrukcję okna i dwa komunikaty serwera o sterowaniu.

## Wyniki

241 testów / 39 zestawów, 0 błędów, 0 pominięć. Nowe przypadki obejmują
obie osie obrotu, ograniczenia, brak ruchu bez klawiszy, piętnaście źródeł,
transformację otworu, pełne/częściowe/chybione trafienie i wstrzymanie emisji.

Próba `manual-1`: dwa rzeczywiste klienty TCP i serwer. PASS:

- Rzeczywiste zdarzenia WASD obracają przedmiot bez zmiany jego pozycji.
  Samo trzymanie worka i konewki przez 450 ms nie zmienia kąta.
- Rozcięcie, ręczny obrót, wsypywanie, puszczenie i ponowny chwyt bez
  samoczynnego obrotu, napełnienie doniczki i zużycie jednego użycia worka.
- Nasiono z woreczka, nietrafienie, odzyskanie, opadanie, zasypanie,
  ręczny przechył konewki i strużki poruszające się z sitkiem.
- Pełny cykl do ośmiu liści, osiem nagród i osiem punktów zużycia nożyc.
- Replay, blokada drugiego gracza, podmiana źródła, przedwczesne ukończenie,
  zamknięte/stosowane worki, wszystkie trzy użycia, przepełnienie ekwipunku,
  zakupy, rozdzielenie gotówki/banku i zachowanie ilości materiałów.
- Cztery rozmiary/skale okna; oglądnięto zrzuty otwartego i odwróconego
  worka, wsypywania, pełnej doniczki oraz podlewania.
- Dotychczasowe kontrole UV: dwie lampy, ruch kamery Fancy/Fabulous,
  filtracja GL_LINEAR, brak emisji nad lampą, usunięcie i przeładowanie zasobów.
- Pierwszy serwer exit 0. Restart z dysku PASS: sprzęt, zużyta ziemia,
  maska/UUID drugiej rośliny, przedmioty, woda, narzędzia i gotówka zachowane.

Po próbie zmieniono wyłącznie dwa stare komunikaty tekstowe serwera o Q/E;
pozostały kod wykonawczy odpowiada sprawdzonej próbie. JAR zbudowano ponownie
bez włączonych źródeł automatyzacji.

## Granice

**Wersja testowa, używaj kopii świata.** Przejście restartu w tym przebiegu
nie rozwiązuje wcześniejszego nieregularnego problemu zapisu stanowiska,
odtworzonego m.in. w 0.1.45 i 0.1.47. Ta zmiana nie naprawia zapisu.
Nie jest to potwierdzenie braku wszystkich błędów całego moda.

## Artefakt

- Plik: `crimeandlaw-0.1.48.jar`, 3958161 bajtów.
- SHA-256: `c1d5b5190f7e821454f4957daa7525de9306063c6efa9cea9ed6ad6f1849fc94`.
- CRC ZIP poprawne, brak duplikatów i klas Smoke.
- 252 klas produkcyjnych i 34 zasoby porównane bajtowo z wynikiem kompilacji/źródłami.
- Pierwsze przygotowanie testów wymagało odtworzenia pliku ścieżki zasobów.
  Pierwsze pakowanie przerwał brak tymczasowego pliku w wygenerowanym katalogu
  zasobów; katalog odtworzono ze źródeł i ponowne pakowanie przeszło.

Dane: [validation-0.1.48.json](validation-0.1.48.json).
