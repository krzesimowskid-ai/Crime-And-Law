# Weryfikacja 0.1.53 — tekstura i budowa aurelii

Minecraft 1.21.1 / NeoForge 21.1.248 / Java 21 / protokół 28.
Wersja do testów na kopii świata. Wcześniejszy nieregularny problem zapisu
stanowiska pozostaje otwarty; ta wersja nie zmienia kodu zapisu.

## Zmiana

Nowa zielona tekstura 64×64 zastępuje poprzednie złote pasy. Ząbkowane
listki wychodzą ze wspólnej nasady, z osobnym ogonkiem między blaszką
a główną łodygą. Materiał ma osiem zielonych kolorów, binarną alfę i filtr
nearest. Oryginał imagegen i sposób próbkowania opisuje
[pochodzenie zasobów](AURELIA_ASSETS.md).

Dwie dolne pary wyrastają naprzeciw siebie, a wyższe liście zmieniają
kierunek wokół łodygi i są stopniowo mniejsze. Dolne blaszki lekko opadają,
środkowe są bliższe poziomym, a górne delikatnie uniesione. Krzywizna
wzdłużna i poprzeczna, mały skręt oraz opuszczenie końca dają przestrzenną
powierzchnię zamiast obróconego płaskiego prostokąta. Ogonek składa się
z dwóch połączonych odcinków. To stylizowana geometria gry, bez symulacji
tkanek, sprężystości lub reakcji na wiatr.

Botaniczne odniesienia do budowy i układu liści:

- [UF/IFAS — Cannabis sativa](https://ask.ifas.ufl.edu/publication/AG476).
- [Canadian Food Inspection Agency — biology of Cannabis sativa](https://inspection.canada.ca/en/plant-varieties/plants-novel-traits/applicants/directive-94-08/biology-documents/cannabis-sativa).

Opisane źródła wspierają budowę złożonego liścia z ogonkiem oraz przejście
od dolnych par do górnych liści naprzemianległych. Wymiary, kąty, liczba
możliwych liści i rozkład krzywizny są decyzją graficzną tej fikcyjnej rośliny.

## Geometria i sterowanie

UUID nadal ustala cały wariant rośliny. Cięcie nie przestawia pozostałych
liści i nie losuje nagród. Konserwatywne pochylone graniastosłupy obejmują
skręt, krzywiznę i grubość; SAT zachowuje margines 0,006 bloku między
blaszkami. Ogonki celowo dochodzą do wspólnej łodygi.

Renderer i wybieranie używają tej samej siatki 8×4; test alfy kursora
uwzględnia 64×64 zamiast poprzednich 32×32. Odcięty liść zachowuje kształt.
Konserwatywna obwiednia lotu obejmuje cały wygięty model, aby nie znikał
przed opuszczeniem dolnej krawędzi widoku. Lot nadal jest sztywną animacją
z grawitacją, bez nowych kolizji z otoczeniem.

## Wyniki

Pełny zestaw: **269 testów / 43 zestawy; 0 błędów, awarii i pominięć**.
Oddzielne 13 testów geometrii i lotu również przeszło. Dla 2000 ziaren
sprawdzono wszystkie pary blaszek, dolne przeciwległe pary, różne kierunki
i położenie nad rantem doniczki oraz pod lampą. Niezależna kontrola
przycina poziome rzuty obwiedni i porównuje ich płaszczyzny, zamiast
powtarzać implementację SAT. Próbki zakrzywionej powierzchni i zgodność
generatora z rendererem mają osobne kontrole.

`organic-4`: dwa rzeczywiste klienty TCP i osobny serwer. PASS:

- Ten sam trwały wariant widzą obaj gracze i okno zbioru.
- Osiem rzeczywistych cięć w dowolnej kolejności, co najmniej trzy kliknięcia
  w nieprzezroczystą blaszkę poza znacznikiem; osiem nagród i osiem punktów
  zużycia. Ponowione zakończenia cięcia nie dodają nagród.
- Pierwszy opadający model śledzony od pierwszej klatki; usunięcie dopiero
  po opuszczeniu dolnej krawędzi przez całą obwiednię.
- Obejrzano zbiór, roślinę pod UV, lot oraz galerię czterech różnych układów.
- Oba stdout i oba `logs/latest.log` bez błędów OpenGL.
- Pierwszy serwer zakończył się kodem 0; rzeczywisty restart z dysku zachował
  sprzęt, zużytą ziemię oraz UUID, liczbę i maskę drugiej rośliny.

`organic-1` przerwano po czterech cięciach, kiedy automat nie dostał
piątej nagrody. Nie zaliczamy tej próby jako PASS i nie przypisujemy jej
jednoznacznej przyczyny bez danych. Automat sprawdza teraz natychmiast,
czy LPM wybrał oczekiwany liść, celuje w środki pikseli z dokładniejszym
skalowaniem wejścia i po 10 sekundach wypisuje odpowiedź o brakującym
cięciu. `organic-2` potwierdził poprawny wybór blaszki, ale serwer odrzucił
puszczenie jako zbyt późne. Automat sprawdzał zwolnienie co trzecią klatkę;
teraz sprawdza je w każdej klatce, celuje w 850 ms i zapisuje rzeczywisty czas.
Zasady produkcyjne 12–29 ticków pozostają bez zmian. Nie dodano
ponawiania cięć ani przyznawania nagród. `organic-3` nie uruchomił klientów:
nowy świat nie osiągnął gotowości serwera w ciągu 100 sekund. Zwiększono
wyłącznie limit rozruchu i restartu środowiska do 200 sekund; warunki
wyboru, cięcia i nagród pozostają te same.

Ta próba nie obejmuje ponownej integracyjnej kontroli worków, sadzenia,
sklepu i finansów. Dotychczasowe testy jednostkowe tych części należą do
269 testów. Udany restart nie zamyka wcześniejszego problemu zapisu.
Nie zmieniano UV, protokołu 28, zapisu, kosztów, plonu ani zasad nagród.

## JAR

- `crimeandlaw-0.1.53.jar`, 4347410 bajtów.
- SHA-256 `419a553dc07a36f45ce3254ad639c5d9286501ebdb9a5e55d9d129969679d9d3`.
- ZIP CRC poprawne, bez powtórzonych wpisów i klas Smoke.
- 266 klas produkcyjnych oraz 34 zasoby zgodne bajtowo
  z końcową kompilacją i źródłami; końcowy JAR zbudowano bez fixture.

Dane: [validation-0.1.53.json](validation-0.1.53.json).
