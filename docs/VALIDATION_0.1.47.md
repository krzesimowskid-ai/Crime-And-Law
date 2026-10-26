# Weryfikacja 0.1.47 — UV skierowane w dół

2026-10-26. Minecraft 1.21.1, NeoForge 21.1.248, Java 21, protokół 25.
**Poprawka światła przeszła kontrolę. Wersja pozostaje testowa: zapis stanowiska po restarcie ponownie nie przeszedł kontroli. Używaj kopii świata.**

## Zmiana

Po nagraniu użytkownika zmieniono emisję we wszystkich kierunkach na bezpośrednią, miękką wiązkę spod oprawy. Źródło odpowiada spodowi świetlówek na wysokości 1,545 bloku nad podstawą stelaża. Światło pada na roślinę i podłogę, słabnie z odległością i ku brzegom; nad oprawą nie ma emisji. Pełne nieprzezroczyste bloki zasłaniają wiązkę. Nie stosuje się filtra gotowego obrazu ani zwykłej białej emisji lampy.

Znaleziono błąd filtrowania: `NativeImage.upload` resetuje filtr tekstury do `NEAREST`, mimo wcześniejszego ustawienia `LINEAR`. Przywrócono interpolację po każdym przesłaniu danych. Wybrane lampy mają stałą kolejność przy chodzeniu między nimi; niezmienne pola są ponownie wykorzystywane.

## Wyniki

- **235 testów w 39 zestawach**, zero niepowodzeń, błędów i pominięć. Obejmują kierunek, miękką krawędź, tłumienie, cień nieprzezroczystej przeszkody, determinizm pola i próbkowanie powyżej oprawy.
- Próba `directional-2`: rzeczywisty dedykowany serwer i dwa klienty TCP. Kontrola światła, usuwanie/montaż lampy oraz przeładowanie zasobów — **PASS**.
- Dwie lampy i ruch kamery w Fancy, następnie ruch w Fabulous: po **119 pomiarów** tego samego fragmentu białej podłogi na tryb. Zakres zmian RGB wyniósł odpowiednio **0,306 / 0,289 / 0,372 na 255** w obu trybach. Sprawdzano rzeczywisty filtr GPU: `LINEAR` dla obu kierunków.
- Sufit nad lampą: maksymalna różnica pikseli centralnej próbki 11×11 między włączonym a wyłączonym UV wyniosła **0 na 255**. Obejrzano zapisane obrazy podłogi, sufitu i ruchu kamery.
- Pełny cykl przygotowania, źródło wody, zużycie materiałów, zbiór ośmiu liści, rezerwacja drugiego gracza, kontrole ochrony i powtórzeń pakietów — **PASS**. Kontrole skal interfejsu i WASD — **PASS**.
- Pierwszy serwer zakończył pracę kodem 0. **Restart — FAIL:** `Reusable equipment and exhausted soil persist`. Stan stanowiska nie został zachowany zgodnie z oczekiwaniem. To ponowne odtworzenie otwartej obserwacji z [0.1.45](VALIDATION_0.1.45.md), bez ustalonej przyczyny. Nie uznano poprawki UV za naprawę zapisu.
- Pierwsza próba uruchomienia zatrzymała się przed kontrolą gry na ładowaniu biblioteki natywnej LWJGL. Rozdzielono katalogi ekstrakcji dla obu klientów; druga próba uruchomiła oba poprawnie. Ta zmiana dotyczy wyłącznie skryptu testowego.

Integralność JAR-a, zgodność klas i zasobów oraz wyniki pomiarów są zapisane w `validation-0.1.47.json`. Klasy automatyzacji nie są pakowane do wydania.

## Granice

Nie testowano zamienników renderera, zewnętrznych paczek shaderów ani długotrwałego obciążenia dużą farmą. Cienie częściowych kształtów są przybliżone. UV nie zwiększa serwerowego kanału zwykłej jasności blokowej; roślina wykrywa zamontowaną lampę bezpośrednio. Nie ma dodatkowej instalacji shaderów wymaganej od gracza.
