# Crime And Law — ustalenia projektu

Stan projektu: 21 października 2026.

## Status i sposób dalszej pracy

To zapis pomysłów i decyzji użytkownika, a nie opis gotowego moda. Powstaje prototyp panelu gangów, obecnie wersja 0.1.13. 19 października użytkownik odłożył DarkPerazera i wrócił do Crime And Law. Zaakceptował rozpoczęcie od podstaw panelu gangów; obecnie doprecyzowuje jego wygląd. Dla pierwszego prototypu przyjęto Minecraft 1.21.1 i NeoForge 21.1.248: użytkownik polecił rozpoczęcie bez wskazania innej platformy. Jest to jawne założenie wykonawcze, nie wcześniejsza deklaracja użytkownika.

Crime And Law jest nowym, osobnym modem. McArk pozostaje odłożony, zachowując cel odwzorowania ASA 1:1. Nie zmieniać projektu McArk w Crime And Law ani nie usuwać jego materiałów w ramach tej pracy.

Użytkownik może przypominać sobie pomysły stopniowo. Nie traktować opisu jako zamkniętej specyfikacji. Zachowywać pomysły, nie zmieniać ich bez pytania i zadawać najwyżej jedno konkretne pytanie naraz. Propozycje asystenta nie stają się ustaleniami bez akceptacji użytkownika.

## Kierunek i atmosfera

- Autorski mod do Minecrafta w gangsterskim klimacie.
- Własne AI dla NPC; jego szczegółowy zakres pozostaje do określenia.
- Ważne są obsługa urządzeń, animacje, modele, dźwięki i atmosfera prowadzenia laboratorium.
- Użytkownik chce złożonego procesu angażującego gracza, a nie pojedynczej wymiany składników na gotowy produkt w jednym oknie craftingu.
- Wydajność jest ważna od początku.
- Pierwszy prototyp jest przygotowywany dla Minecraft 1.21.1 / NeoForge 21.1.248, zgodnie z jawnym założeniem wykonawczym po poleceniu rozpoczęcia.

## Materiały, produkty i maszyny

Nowsze podsumowanie przekazane przez użytkownika zastępuje wcześniejsze rozważania o rzeczywistych recepturach:

- Produkty i receptury mają być fikcyjne, niezwiązane z prawdziwymi narkotykami ani rzeczywistą chemią.
- **Furonatamina** jest przykładową fikcyjną nazwą, nie zatwierdzoną nazwą pierwszego produktu.
- Użytkownik chce przedmiotów reprezentujących cały układ okresowy. Mogą mieć rzeczywiste nazwy i symbole, np. węgiel, sód, azot i tlen.
- Węgiel jako pierwiastek ma być osobnym przedmiotem względem minecraftowego węgla.
- Worek, pudełko lub pojemnik jest umowną jednostką gry i nie oznacza rzeczywistej ilości.
- Temperatura może być wyrażana w **termach ™**, a ciśnienie w **barytach (Bs)**. Są to abstrakcyjne parametry bez przelicznika na rzeczywiste jednostki.
- Duże maszyny multiblokowe mają mieć wejście na surowce i wyjście na produkt.
- Obsługa może obejmować minigry i regulację parametrów jako fikcyjne mechaniki gry.
- Użytkownik chce także odprowadzania dymu/oparów. Dokładne urządzenia, połączenia i reguły symulacji nie są jeszcze ustalone.
- Użytkownik chciał efektów działania produktów na postać. Konkretne profile, czasy, wartości, tolerancja i uzależnienie nie zostały zatwierdzone.

## Handel

- Użytkownik rozważa sprzedaż we wioskach: wieśniakom albo specjalnym handlarzom/dilerom NPC.
- Nie zapadła ostateczna decyzja, komu dokładnie i w jaki sposób się sprzedaje.
- Osobni handlarze z budżetem, popytem, reputacją, pamięcią transakcji i oceną jakości to propozycja asystenta.
- Waluta, ceny i wartości ekonomiczne pozostają do ustalenia.

## Panel gangów

Pomysły użytkownika:

- Jeden klawisz otwiera panel zależnie od członkostwa w gangu. Konkretny klawisz nie został wybrany.
- Gracz bez gangu widzi opcje/zakładki **„Stwórz swój gang”** oraz **„Znajdź gang”**.
- Wyszukiwanie umożliwia znalezienie i dołączenie do gangów innych graczy w danym świecie/na serwerze. Nie ustalono listy między różnymi serwerami.
- Przy tworzeniu gangu ustala się nazwę i własne logo; opis jest rozważaną opcją.
- Logo ma umożliwiać własny obrazek, nie jedynie wybór z gotowych wzorów.
- Import obrazu z komputera i z linku to pomysły do technicznego sprawdzenia, a nie potwierdzone działające funkcje.
- Po utworzeniu lub dołączeniu ten sam klawisz otwiera menu aktualnego gangu.
- Jedną z zakładek menu ma być mapa świata pokazująca m.in. kontrolowane obszary.
- Rangi, uprawnienia, zaproszenia, akceptowanie zgłoszeń, limity członków i koszty utworzenia nie zostały określone.

## Wygląd menu gangu — doprecyzowanie 19 października

- Na początek jeden spójny, jednolity motyw bazowy.
- Zaakceptowano makietę: logo i nazwa gangu w lewym górnym nagłówku, zakładki po lewej, treść po prawej, zamknięcie w prawym górnym rogu.
- Użytkownik następnie zażądał kompaktowych proporcji: mniejsze logo i nazwa, niższy nagłówek i przyciski, węższy panel nawigacji, więcej miejsca na treść. Makieta nie określa docelowych wielkości elementów.
- Zakładki pierwszego panelu: Gang, Członkowie, Rangi, Mapa, Ustawienia. Mapa w pierwszym prototypie wyraźnie zapowiada dalszy etap; nie udaje działającego systemu terytoriów.
- Wspólny motyw ustawiany dla gangu dotyczy jego menu, otwieranego zarówno przez założyciela, jak i członków.
- Lider może ustawić domyślny wygląd menu gangu, w tym kolor tła, obramówek oraz przezroczystość.
- Członek może korzystać z wyglądu ustalonego dla gangu albo dostosować go lokalnie tylko dla siebie.
- Osobiste zmiany nie zmieniają ustawień gangu ani wyglądu u innych graczy.
- Nie utożsamiać motywu menu z kolorem własności na mapie. Powiązanie tych kolorów nie zostało zatwierdzone.
- Szczegóły kontrolek edytora, dokładne kolory startowe, zakres przezroczystości i zapisywanie osobistych motywów osobno dla każdego gangu pozostają do ustalenia.

## Nowszy szkic ekranu bez gangu — 19 października, wersja 0.1.1

Użytkownik przekazał szkic z dwiema równymi zakładkami w jednym górnym rzędzie:
„Stwórz gang” po lewej i „Znajdź gang” po prawej. Pod nimi jeden szeroki obszar
treści. Ten układ dotyczy ekranu przed utworzeniem/dołączeniem do gangu;
zastępuje wcześniejszy boczny wybór tworzenia/wyszukiwania. Szkic określa układ,
nie zmianę motywu na biały. Dla członka/lidera pozostaje zaakceptowany panel gangu.

## Gangi NPC — pomysł na później

- Użytkownik rozważa gangi złożone z NPC i mające własne AI.
- Do takich gangów gracz nie może dołączać; miałyby być przeciwnikami.
- Gangi i ich budowle mają być zróżnicowane.
- Rozważane są gangi na powierzchni i ukryte pod ziemią.
- To kierunek do późniejszego rozwinięcia, nie zadanie implementacyjne na teraz.
- Sposób walki, skład gangów, zachowania AI, generacja baz i system ich odnajdywania pozostają otwarte.

## Terytoria

- Podstawową jednostką jest chunk o powierzchni 16 × 16 bloków.
- Własność obejmuje całą wysokość chunka: od najniższego do najwyższego poziomu świata. Użytkownik nie chce osobnej własności powierzchni i podziemia w tej samej kolumnie.
- Jeżeli choć fragment wioski leży w chunku, cały chunk zalicza się do jej obszaru. Sposób technicznego rozpoznawania granic wioski nie został wybrany.
- Neutralny, niezajęty teren można przejąć za opłatą. Wartość i waluta nie są ustalone.
- Wioski i zajęte tereny wymagają innego sposobu przejęcia niż zakup neutralnej ziemi.
- Walka z mieszkańcami wioski jest luźnym pomysłem użytkownika; nie zatwierdzono jeszcze warunków przejmowania.
- Nie ustalono odległości między działkami, wymogu sąsiedztwa, limitu terytorium, utrzymania ani zasad ochrony przed utratą terenu.

## Widoczność i odkrywanie ukrytych terytoriów

- Gangi na powierzchni mają widoczne budynki, a przynależność ich terenu jest normalnie widoczna.
- Teren ukrytego podziemnego gangu początkowo wygląda na mapie jak neutralny.
- Próba zakupu takiego chunka zostaje odrzucona. Komunikat informuje, że teren jest zajęty przez gang.
- Odmowa nie ujawnia automatycznie położenia bazy ani wejścia do niej.
- Gra **nie obrysowuje automatycznie** ukrytego terytorium ani nie zaznacza go za gracza. Użytkownik wyraźnie doprecyzował ręczne oznaczanie.
- Gracz może sam wyciągać wnioski ze sprawdzania kolejnych chunków.
- Dokładna treść komunikatu i ujawnianie nazwy ukrytego gangu nie zostały doprecyzowane.

## Ręczne oznaczenia na mapie

Potwierdzone przez użytkownika:

- Zakładka/opcja **„Zaznacz”** pozwala graczowi samodzielnie oznaczyć teren, np. podejrzewaną lokalizację ukrytego gangu.
- Kolor wybiera się płynnie z pełnego **koła barw/palety**, nie z kilku gotowych próbek.
- Są **oba zakresy widoczności**: **„Tylko ja”** oraz **„Mój gang”**.
- Oznaczenie jest notatką na mapie, nie zmianą własności.

Do rozważenia, bez zatwierdzenia:

- Wyświetlanie autora wspólnego oznaczenia.
- Uprawnienia do edycji/usuwania według rang.
- Nazwy i opisy znaczników, przezroczystość, dokładny sposób zaznaczania i zakres ujawnionej mapy.

Użytkownik lubi rozważanie obu wariantów przy dylematach. Nie oznacza to automatycznego zatwierdzania wszystkich przedstawianych opcji; konkretne decyzje nadal należy uzgadniać.

## Propozycja pierwszego prototypu — jeszcze bez realizacji

Asystent zaproponował: podstawę moda, jeden multiblok, jeden fikcyjny proces, prosty układ oparów, jednego handlarza, pieniądze, zapis oraz JAR do testów. To propozycja kolejności pracy, nie zatwierdzenie wyglądu, receptury, wymiarów maszyny ani parametrów.

Aktualny pierwszy etap: panel tworzenia i wyszukiwania gangów, własne logo, członkostwo oraz trwały zapis, docelowo JAR do sprawdzenia z drugim graczem. Użytkownik zaakceptował rozpoczęcie i następnie doprecyzował motywy menu. Po ponownym poleceniu „to rób” asystent rozpoczął prototyp na Minecraft 1.21.1 / NeoForge 21.1.248, jawnie podając to założenie.


## Zakres prototypu 0.1.0

Szczegóły wykonawcze pierwszej wersji, możliwe do zmiany po testach:

- Tworzenie, wyszukiwanie i zgłoszenia do gangów; zgłoszenie przyjmuje osoba z uprawnieniem.
- Jeden gang na gracza; zapis gangów w danych świata po stronie serwera.
- Domyślne rangi Lider i Członek; własne rangi z uprawnieniami do członkostwa, rang i wyglądu.
- Przekazanie przywództwa, opuszczenie gangu i usuwanie członków; lider nie może opuścić pozostałych bez przekazania przywództwa.
- Własne logo PNG z lokalnej ścieżki pliku, dopasowane do 64×64. Pobieranie obrazów z URL nie jest jeszcze zaimplementowane.
- Kolory tła i obramówki oraz krycie tła; osobiste nadpisanie motywu nie zmienia ustawień innych graczy.
- Robocze limity zasobów prototypu: 64 członków, 12 rang, 512 gangów; nie są ostateczną decyzją o balansie moda.
- NPC, produkcja, handel, mapa i własność chunków pozostają dalszymi etapami.



## Doprecyzowanie 19 października — wersja 0.1.2

- Logo: przeciąganie pliku z folderu na otwarty panel gry, kadrowanie i dopasowanie,
  przy zachowaniu możliwości podania ścieżki. Dodano też systemowe okno wyboru pliku.
- Usunąć zbędny napis o Esc pod „Opuść gang”; zostaje zamknięcie X i Esc.
- Dopracować wygląd nagłówka z nazwą gangu, tło i ramkę oraz czytelność przycisków.
- Rangi można dowolnie przestawiać przeciąganiem, także umieszczać nową rangę bez
  uprawnień nad Liderem. Kolejność wizualna jest niezależna od uprawnień i właściciela.
- Ustawienia podzielić na schludne kategorie „Wygląd” i „Logo”.
- Kolory wybiera się przyciskiem „Zmień…”: koło barw oraz suwaki jasności,
  nasycenia i krycia, z dodatkowymi polami HEX/RGB. Krycie dotyczy tła.
- Zachowane rozróżnienie wspólnego wyglądu gangu i wyglądu osobistego.

## Doprecyzowanie 20 października — wersja 0.1.3

Poniższe decyzje zastępują wcześniejsze ustalenia o rangach i limicie członków.

- Usunąć system rang; do zarządzania pozostaje lider gangu.
- Nie ustalać limitu członków. Licznik ma pokazywać tylko „Członkowie: liczba”.
- W głównej zakładce wyśrodkowana, ozdobna ramka z logo, nazwą gangu i pod nią
  liczbą członków. Orientacyjna szerokość: jedna trzecia obszaru treści.
  „Czarne Wilki” jest wyłącznie przykładem używanym w podglądach.
- Ustawienia umieścić na dole nawigacji, nad „Opuść gang”.
- Zgłoszenia, odświeżanie i czynności lidera przenieść do kategorii zarządzania
  w ustawieniach. Usunąć techniczne opisy zapisu/serwera z głównej zakładki.
- Suwaki kolorów mają być pionowe, z rzeczywistymi gradientami. Jasność:
  czerń przy 0% na dole, wybrany kolor przy 100% u góry. Nasycenie i krycie
  również pokazują wizualnie, jaki efekt daje przesuwanie.
- Zachować koło barw, HEX/RGB, osobisty i wspólny wygląd oraz import i kadrowanie logo.
- Wykonawczo długie listy są stronicowane; nie stanowi to limitu członkostwa.

## Nowy szkic 20 października — wersja 0.1.4

- Szkic użytkownika `image(20261020-111515).png` zastępuje poprzednią centralną
  kartę o szerokości jednej trzeciej obszaru treści.
- Duże logo na środku u góry części treści; nazwa gangu pod nim.
- Niżej pozioma linia przez całą szerokość tej części panelu.
- Pod linią nagłówek „Informacje o gangu” i schludnie rozmieszczone informacje
  od lewej do prawej, zamiast skupiania ich w lewej kolumnie.
- Aktualny układ ma trzy równe kolumny: członkowie, lider i miejsce na poziom.
  Poziom gangu został wspomniany jako przykład przyszłej informacji; nie ustalono
  zasad awansowania, XP, kosztów ani korzyści. Na teraz widnieje „Wkrótce”.
- Użytkownik zaakceptował edytor kolorów i gradienty; zachować ich wygląd i działanie.
- Użytkownik zapowiedział inspiracje przycisków. Poczekać na materiał przed
  zmianą ich wyglądu; zachowana dotychczasowa nawigacja i miejsce na dalszą treść.

## Kategorie i obrazy — 20 października, wersja 0.1.5

- Użytkownik potwierdził widoczne kategorie: Gang, Członkowie, Rejestr,
  Mapa/Kontrola terytorialna oraz Ustawienia. Nie dodawać wszystkich ze zdjęcia.
- Wyłącznie Transport ma być nieaktywnym fundamentem w kodzie, niewidocznym
  w menu. Wcześniejsza interpretacja asystenta wyłączająca inne kategorie była
  błędna i została przez użytkownika skorygowana.
- Rejestr zapisuje publiczne wydarzenia członkostwa, przywództwa i wyglądu.
  Od 0.1.7 przechowywane jest ostatnie 10 000 wpisów, z serwerowym stronicowaniem.
- Mapa pozostaje dotychczasową podstawą kontroli terytorialnej.
- Logo ma przyjmować także 256×256 i obrazy o innych proporcjach/rozdzielczościach,
  z kadrowaniem lub dopasowaniem całości. Obsługiwane PNG, JPG/JPEG, GIF i BMP;
  GIF jest statycznym logo z pierwszej klatki. Końcowa miniatura nadal ma 64×64.

## Rejestr — przewijanie i strony, wersja 0.1.6

Użytkownik potwierdził: wpisy od góry do dołu, najnowsze na górze; pionowe
przewijanie kółkiem i pasek po prawej; strony i ich przyciski na dole.
Nagłówek oraz sterowanie stronami pozostają na miejscu. Scroll obejmuje tylko
wpisy bieżącej strony. Po zmianie strony lista zaczyna od góry. Zachowano
protokół serwera. Od 0.1.7: 100 wpisów na stronę i limit 10 000 wydarzeń.

## Rejestr — ustalenia 0.1.7
Strzałki poprzedniej/następnej strony są obok siebie w nagłówku po prawej, tuż przed faktycznym numerem strony/liczbą stron. Dolna część jest przeznaczona na listę. Limit 10 000 wpisów (100 stron po 100), najstarsze usuwane po przekroczeniu limitu.
Docelowo Rejestr obejmie magazyn: kto, kiedy, co i ile włożył/wyjął; członkostwo oraz awanse/degradacje z opcjonalnym powodem. Historia wpłat i wypłat (kto, kiedy, kwota) należy osobno do Zarządzania saldem. Magazyn, saldo i system rang nie są dodawane w tej zmianie.

## Kategorie 0.1.8
Dodane widoczne zakładki Premie, Zarządzanie saldem, Czarna lista i Cele. Na razie panele informacyjne bez mechaniki; tylko Transport pozostaje ukryty. Historia finansowa docelowo w Zarządzaniu saldem.

## Naprawa importu 0.1.9
21 października po powrocie środowiska odtworzono 0.1.8 z GitHuba. Rozszerzony import: format rozpoznawany po zawartości, WebP/SVG/TIFF/ICO/ICNS/PSD/TGA/PCX/PNM obok PNG/JPEG/GIF/BMP. Podgląd do 1024 px, kadrowanie i wyjściowe PNG 64×64 zachowane. Limity źródła 64 MB, raster do 100 000 px na bok i miliarda pikseli; SVG 8 MB, bez zewnętrznych zasobów. Oryginalny plik logo użytkownika nie został dostarczony, więc jego konkretny przypadek pozostaje do sprawdzenia na pliku.

## Logo — doprecyzowanie 21 października, wersja 0.1.10
Użytkownik chce importu niezależnego od tego, co przedstawia obraz. Nie dodawać
filtrów nazw, flag ani symboliki; wszystkie obrazy mają ten sam proces importu.
Nie oznacza to obsługi uszkodzonych danych ani każdego istniejącego formatu.
Dodano próbę kolejnych czytników, gdy jeden zawiedzie, i testy sześciu modeli
kolorów PNG, nietypowych nazw oraz izolacji strumieni po błędzie czytnika.

## Szczegółowy obraz logo — 21 października, wersja 0.1.11
Użytkownik chce wiernego wyświetlania przesłanego rysunku z drobnymi detalami,
bez grubej pikselizacji wprowadzanej przez miniaturę 64×64. Narysowane przez
autora kwadratowe elementy są częścią oryginału. Nie dodawać przełącznika
„pixel art / gładkie” ani automatycznego przerysowywania obrazu.

Po doprecyzowaniu podniesiono wynik do 512×512, z bezstratnym PNG do 1,1 MB,
filtrem liniowym w podglądzie i całym menu oraz zgodnością zapisu ze starymi
logo. Poprzednich detali usuniętych podczas importu 64×64 nie da się odzyskać
bez ponownego wczytania oryginału. Protokół 4; ten sam nowy mod na kliencie
i serwerze. Nie zmieniać rozmiaru okien ani układu zaakceptowanego menu.

## Przywrócenie rang i uproszczenie logo — 21 października, wersja 0.1.12
Użytkownik wyjaśnił, że rangi mają być dostępne. Przywrócono tworzenie, nazwy,
uprawnienia, przypisywanie członkom oraz przeciąganie kolejności, także nad
Lidera. Pozycja nadal jest niezależna od uprawnień i faktycznego właściciela.
To aktualna decyzja zastępująca wcześniejsze usunięcie systemu rang.
Przywrócony zakres uprawnień i roboczy limit 12 rang pochodzą z poprzedniej
wersji tego systemu. Nie przywracać dawnych limitów liczby członków/zgłoszeń.

Na podstawie zrzutu image(20261021-152133).png usunięto pole ścieżki do obrazu,
„Wczytaj i dopasuj”, tekst z formatami/przeciąganiem oraz informację o
opcjonalnym logo i późniejszej zmianie. Pozostają wybór z plików i sam
mechanizm przeciągania obrazu. To założenie zostało podane użytkownikowi
przed wykonaniem. Wczytanie pliku nadal otwiera kadrowanie.

Zapisy bez rang otrzymują domyślne rangi i przypisania. Jeśli stare dane rang
są obecne w pliku świata, są zachowywane. Zdarzenia tworzenia/zmiany rangi,
przypisania i kolejności są rejestrowane. Protokół 5; nowy JAR na kliencie
i serwerze. Logo nadal 512×512 z wygładzaniem.


## Formularz rang, rejestr i zamykanie — 21 października, wersja 0.1.13
- „+ Nowa ranga” otwiera osobne okno „Nowa ranga”: nazwa, uprawnienia, zapis.
  Anuluj/X/Esc zamykają je bez utworzenia rangi; skalowanie GUI zachowuje szkic.
- Usunięto informacje o przeciąganiu/kolejności rang oraz polecenie podglądu
  i wyboru odbiorcy zapisu wyglądu. Opis wspólnej zmiany logo pozostaje.
- Kwadratowy przycisk zamykania ma symetryczny, wyśrodkowany X zarówno
  w panelu gangu, jak i w oknach koloru, kadrowania i nowej rangi.
- Rejestr nie dopisuje zmian kolorów, logo, tworzenia/edycji definicji rang
  ani ich kolejności. Zachowano wcześniej ustalone zdarzenia członkostwa,
  przywództwa i przypisania rang członkom. Istniejące wpisy nie są kasowane.
- Pełna polska data z rokiem i czasem jest nad opisem, np.
  „21 października 2026 o godzinie 18:15”, w strefie czasowej komputera gracza.
- W przyszłości rejestr obejmie operacje magazynu oraz użycie gangowych/
  frakcyjnych pojazdów. To źródła zdarzeń do dodania razem z ich systemami;
  transport nadal ma wyłącznie ukrytą definicję. Historia salda jest osobna.

## Policja — pomysł na później, bez implementacji
Użytkownik chce różnych jednostek, na razie omawianych wspólnie jako policja.
Gracze z policji obecni na serwerze sami planują akcje i mogą zbierać ekipę
na najazd na gang. Gdy nie ma aktywnych graczy z policji lub nikt do niej
nie należy, ich rolę przejmują NPC z AI. Dowody/ryzyko mają prowadzić do
najazdów na bazy. Szczegóły zasad i zachowań pozostają do uzgodnienia.
Wspólna dokumentacja śledztw i płynne przejmowanie rozpoczętych akcji są
propozycjami asystenta, nie zatwierdzonymi jeszcze mechanikami.
