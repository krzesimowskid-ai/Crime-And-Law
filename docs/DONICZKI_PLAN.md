# Doniczki i jednorazowe uprawy — projekt następnego etapu

Plan z 25 października 2026 został wdrożony w **0.1.43**.
Poniżej zachowano ustalenia projektowe. Aktualną instrukcję, ceny, czas,
warunek UV i szczegóły wykonania opisuje [AURELIA.md](AURELIA.md).
Walidacja jest w [VALIDATION_0.1.43.md](VALIDATION_0.1.43.md).

## Ustalony cykl i przedmioty

- Jedno nasiono lub sadzonka daje jedną roślinę i jeden cykl zbioru.
  Po pełnym zbiorze roślina się zużywa. Kolejny cykl wymaga nowych nasion
  lub sadzonki z zakupu, bez samoczynnego rozmnażania farmy.
- Doniczka jest własnym przedmiotem/blokiem z pikselowym modelem 3D.
  Zastępuje dotychczasowe sadzenie na zaoranej ziemi. Pozostaje po zbiorze.
- Podłoże zużywa się po cyklu; kolejne sadzenie wymaga nowego nasypania.
- Jedno użycie worka ziemi wystarcza na pełne przygotowanie jednej doniczki.
  Worki zawierają odpowiednio 1, 2 lub 3 pełne użycia. Worek jednorazowy
  znika po nasypaniu; większy pozostaje z pomniejszonym zapasem i znika
  dopiero po ostatnim użyciu. Przykładowo duży worek przechodzi 3 -> 2 -> 1
  -> zużyty, obsługując trzy kolejne napełnienia tej samej lub różnych doniczek.
- Napełnienie jednej doniczki nie wymaga trzech porcji. Wcześniejszy
  przykład takiego przelicznika zastępuje powyższe doprecyzowanie użytkownika.
- Istnieją zarówno woreczki zawierające kilka nasion, jak i luźne nasiona.
  Interakcja sadzenia rozpoznaje oba warianty i pozwala rozpakować woreczek.
- Podlewanie odbywa się konewką ze skończonym zapasem wody i możliwością
  ponownego napełnienia.
- Zachowujemy własny pikselowy styl 3D rośliny, liści, przedmiotów i narzędzi.

## Stalowy stelaż i lampa UV

Stelaż ma wysokość **dwóch bloków** i obsługuje **jedną doniczkę**:

| Część stelaża | Montowany przedmiot |
|---|---|
| Górny blok | Lampa UV skierowana na roślinę poniżej |
| Dolny blok | Wkładana doniczka z jedną rośliną |

Doniczkę i lampę wkłada się do odpowiednich części stelaża. Rama, doniczka
oraz lampa pozostają po cyklu. Premia obejmuje wyłącznie doniczkę należącą
do tego stanowiska, zamiast wszystkich pobliskich roślin. Zwykłe światło
pozwala uprawiać roślinę bez stelaża; możliwość wzrostu w ciemnym wnętrzu
wiążemy z działającym oświetleniem stanowiska.

Nazwa podstawowego przedmiotu w grze: **lampa UV**. Odniesieniem są
mechaniki symulatorów, zwłaszcza Drug Dealer Simulator 2, gdzie stelaż
ma montowaną doniczkę i lampę UV. Dodatkowe modele LED i inne ulepszenia
pozostają propozycjami do późniejszego bilansu. Ceny, zasilanie i zużycie
lampy nie są jeszcze ustalone.

## Przebieg minigry i sadzenia

1. Gracz ustawia doniczkę samodzielnie lub wkłada ją w dolną część stelaża.
2. Otwiera przygotowanie doniczki. Okno sprawdza dostępność ziemi i nasion.
3. W minigrze otwiera worek, przechyla go i kieruje strumień do doniczki.
   Widać wysypywanie, opadanie i osiadanie ziemi oraz zmianę poziomu podłoża.
   Ruchy i ewentualne przerwy na osiadanie należą do jednego pełnego użycia
   worka; nie pobierają osobnych porcji przy każdym przechyleniu.
4. Po pełnym napełnieniu ubywa jedno użycie worka. Doniczka zachowuje ziemię,
   a pozostała zawartość worka służy do następnych napełnień.
5. W tym samym przebiegu gracz wybiera nasiono lub sadzonkę. Woreczek nasion
   może otworzyć właśnie w tym kroku, zużywając jedną sztukę do sadzenia.
6. Sadzi roślinę i podlewa konewką. Stan podłoża i nawodnienia jest widoczny
   w modelu i podpowiedzi. Opcjonalnie montuje UV w górnej części stelaża.
7. Po dojrzeniu zbiera widoczne liście w dowolnej kolejności, korzystając
   z minigry nożyc. Jeden odcięty liść daje jedną nagrodę.
8. Po ostatnim liściu cykl się kończy, a podłoże jest zużyte. Sprzęt zostaje;
   gracz przygotowuje doniczkę ponownie, korzystając z nowego worka albo
   pozostałych użyć większego worka, i sadzi kolejne kupione nasiono.

Użytkownik rozważył nacinanie worka w celu zrobienia otworu do wysypywania,
ale pozostawił szczegóły otwarte. To opcjonalny gest/animacja w minigrze;
nie ustalono obowiązku posiadania specjalnych nożyc do otwierania worków.
Samo otwarcie worka nie zużywa ziemi i zachowuje jego pozostałe użycia.

## Opakowania nasion i sprzedawca

Zamknięty woreczek pokazuje typ oraz liczbę nasion. Rozpakowanie zamienia
go na dokładnie tę liczbę luźnych sztuk. Okno sadzenia obsługuje luźne
nasiona, zamknięte woreczki i obsługiwane sadzonki. Propozycja wykonania:
domyślnie podpowiada luźne nasiono, z możliwością wybrania innego źródła.
Przy wyborze woreczka pokazuje rozpakowanie i pozostawia resztę nasion
w ekwipunku. Przykładowo z woreczka 5 nasion po jednym sadzeniu zostają 4;
pojemność 5 jest przykładem interfejsu, nie ostatecznym bilansem.
Brak miejsca blokuje rozpakowanie przed zmianą przedmiotów.

**Sprzedawca NPC jest wstępnym wyborem użytkownika**, który zaznaczył,
że rozgrywka nie ma być RP. NPC ma pełnić zwykłą funkcję sklepu z nasionami,
ziemią i wyposażeniem. Ten wybór nie oznacza dodawania zawodów RP, odgrywania
ról ani dodatkowych wymogów fabularnych. Miejsce, ceny i rodzaj używanego
salda pozostają do ustalenia przy wdrożeniu sklepu.

## Losowy plon i efekt UV w grze

Robocze zakresy według propozycji użytkownika, włącznie z granicami:

- Zwykłe światło: **3–6 liści** na roślinę.
- Działająca lampa UV: **4–8 liści** na roślinę.

Przy równych szansach każdej liczby średnia wynosi odpowiednio 4,5 i 6
liści, czyli UV zwiększa oczekiwany plon o około 33,3%. Ewentualne
przyspieszenie wzrostu trzeba bilansować osobno. Rozkład prawdopodobieństwa
i premia do czasu nie są jeszcze ustalone.

Model i wybieranie muszą obsługiwać do ośmiu liści. Liczba widocznych,
możliwych do odcięcia liści odpowiada wynikowi losowania, nie stałemu
zestawowi sześciu z JAR 0.1.42. Serwer losuje wynik raz przy dojrzewaniu
i zapisuje go z tożsamością rośliny oraz pozostałymi liśćmi. Bonus UV zależy
od działania lampy podczas wzrostu, a nie tylko jej włożenia przed zbiorem.
Dokładny warunek naliczenia premii wymaga jeszcze doboru balansu.

## Ekonomia i zabezpieczenia do wdrożenia

- Wyłączyć odrost po pełnym zbiorze (`HarvestController` -> `regrow()`).
- Usunąć recepturę nasion z pszenicy i świecących jagód oraz jej odblokowanie.
- Usunąć zwrot i dodatkowy drop nasion z niszczonej rośliny; dotyczy to też
  młodej rośliny, Fortune, Silk Touch, eksplozji i przyszłych zbieraczy.
- Przestawienie doniczki lub stelaża nie może odnowić rośliny, zwrócić
  nasiona ani wylosować nowego plonu. Zniszczenie stanowiska nie duplikuje
  wkładanej doniczki i lampy przez osobne dropy dwóch części ramy.
- Obie części dwublokowego stelaża odwołują się do jednego stanu stanowiska.
  Jedna doniczka zajmuje jedno stanowisko, jedna lampa daje jedną premię.
- Zakup pobiera pieniądze i przyznaje przedmiot jako jedną operację serwera,
  z kontrolą miejsca w ekwipunku i ochroną przed powtórzeniem.
- Jedno sadzenie zużywa jedno nasiono, nie nasiono i woreczek jednocześnie.
- Jedno pełne napełnienie zużywa jedno użycie worka i napełnia jedną doniczkę.
  Pozostałe użycia zapisują się przy przedmiocie; otwarcie okna, przełożenie
  worka i restart nie mogą resetować licznika.
- Rozpakowanie, napełnienie i sadzenie mają jednorazowe zatwierdzenia.
  Dwie osoby nie mogą jednocześnie zmieniać tej samej doniczki/stelaża.
- Zatwierdzona ziemia zostaje w doniczce po zamknięciu minigry. Rozłączenie
  nie cofa zużycia worka ani postępu. Zapis podłoża obejmuje stan jego zużycia.
- Ponowne wejście do zbioru, relog i restart nie losują plonu ponownie.
- Premie kilku lamp nie sumują się bez ograniczeń na jednej roślinie.

## Materiały odniesienia

Wzorzec rozgrywki stanowią symulatory, a wartości dla fikcyjnej aurelii
są bilansem naszego moda. W Drug Dealer Simulator 2 UV występuje jako
element stelaża, a LED jako ulepszenie; nie przenosimy automatycznie jego
parametrów na własny system:

- [DDS2 — poradnik społeczności na Steam, część Weed Production 101](https://steamcommunity.com/sharedfiles/filedetails/?id=3273716171).
- [DDS2 — katalog przedmiotów, UV Bulb i LED Bulb](https://drugdealersim.com/wiki/dds2/all-items-catalog).

Wcześniej sprawdzono też ogólną rolę światła i UV w botanice. Źródła
wyjaśniają różnicę pojęć; nie wyznaczają premii ani instrukcji w grze:

- [University of Minnesota — Lighting for indoor plants and starting seeds](https://extension.umn.edu/garden-and-home/yard-and-garden/gardening-in-minnesota/lighting-for-indoor-plants).
- [University of Missouri — Understanding Grow Lights](https://extension.missouri.edu/publications/g6987).
- [Qian et al. 2021 — Downsizing in plants](https://academic.oup.com/plphys/article/187/1/378/6294311).
- [Rodriguez-Morrison et al. 2021 — badanie UV-B](https://www.frontiersin.org/journals/plant-science/articles/10.3389/fpls.2021.725078/full).
- [Llewellyn et al. 2022 — badanie światła i UV](https://www.frontiersin.org/journals/plant-science/articles/10.3389/fpls.2022.974018/full).
- [Huebner et al. 2024 — badanie różnych zakresów UV](https://www.frontiersin.org/journals/plant-science/articles/10.3389/fpls.2024.1480876/full).

## Kolejność wykonania

1. Jednorazowy cykl, kupowane materiały i usunięcie dróg odzyskiwania nasion.
2. Doniczka, zużywane podłoże, worki na 1/2/3 napełnienia, nasiona i konewka.
3. Minigra nasypywania, rozpakowanie i sadzenie w jednym przebiegu okna.
4. Dwublokowy stelaż z doniczką na dole, UV u góry i zapisanym plonem 3–6/4–8.
5. Sprzedawca oraz próby dwóch graczy, anulowania, pełnego ekwipunku,
   zużywania większych worków, montażu części ramy, relogu i restartu.
