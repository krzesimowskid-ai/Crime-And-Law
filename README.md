# Crime And Law

Autorski mod gangsterski: panel gangów, członkostwo i własny wygląd menu.
Wersja **0.1.32**.

Pierwsza propozycja własnej ekonomii w dolarach i rozwoju terenów:
[Balans terytoriów](docs/BALANS_TERYTORIOW.md) oraz
[symulacja](docs/SYMULACJA_BALANSU.md). To projekt do dalszej implementacji;
Wersja 0.1.29 dodaje konta graczy, saldo gangu, wpłaty, wypłaty i premie.
Dochody z terytoriów i handlu nadal wymagają implementacji; roboczy balans
nie jest jeszcze odczytywany przez JAR. Zasady użycia: [Finanse](docs/FINANSE.md).
Przejmowanie terenów, zegar świata oraz oznaczenia mapy już działają:
[zasady i komendy captów](docs/CAPTY.md).

## Uruchomienie

- Minecraft **1.21.1**, Java **21**, NeoForge **21.1.248**.
- Włóż `crimeandlaw-0.1.32.jar` do `mods`, zastępując poprzedni JAR tego moda.
- W rozgrywce sieciowej mod musi być **u graczy i na serwerze**.
  **0.1.32 korzysta z protokołu 19** — wymaga tej samej wersji na obu stronach.
  Starsze wersje od 0.1.26 do 0.1.28 używają protokołu 16.
- W świecie jednoosobowym część serwerowa działa razem ze światem.
- **G** otwiera panel; skrót można zmienić w sterowaniu. Esc lub X zamyka okno.

## Minimapka i pieniądze

Podczas gry, w lewym dolnym rogu, widać minimapkę rzeczywistego załadowanego
terenu, strzałkę gracza i północ. Obok są autorskie ikony banknotu i karty
oraz osobne kwoty **Gotówka** i **Bank**. Mapa podąża za graczem i kierunkiem
patrzenia. Mapa zachowuje naturalne kolory terenu, przezroczystość i płynną filtrację;
północ pozostaje przy jej krawędzi. Kwoty są w dwóch małych wierszach
z gładką czcionką i szczegółowymi ikonami. Nad nimi są godzina, data i polski
dzień tygodnia, według czasu i strefy serwera. Serwer synchronizuje zegar
co około 30 sekund oraz po zmianie strefy. Temperatura i pogoda są planowane
na później. Nie wymaga otwierania panelu ani członkostwa w gangu. HUD jest
ukryty przy F1 i w otwartych ekranach. W grze nieaktywne wiadomości czatu
są przesunięte ponad nowy HUD; otwarty czat zachowuje zwykłe położenie.
Rozmiar minimapy jest niezależny od skali menu, a pozycja pieniędzy uwzględnia
faktyczną skalę waniliowego hotbara i statystyk.

Istniejące pieniądze osobiste pozostają w banku, nowa gotówka wynosi 0 $.
Śmierć i odrodzenie nie odbierają ani nie kopiują żadnego z sald.
Nie ma jeszcze przelewów między gotówką i bankiem. Komendy administratora:

```
/crimeandlaw pieniadze dodaj NazwaGracza gotowka 1000
/crimeandlaw pieniadze dodaj NazwaGracza bank 1000
```

Poprzednia postać komendy bez nazwy konta nadal dodaje środki do banku.
Wpłaty i wypłaty gangu oraz premie korzystają z banku. Szczegóły nowych
paneli: [HUD, czarna lista i cele](docs/HUD_CZARNA_LISTA_CELE.md).

## Panel gangu

Bez gangu: dwie równe zakładki u góry — „Stwórz gang” i „Znajdź gang”.
Możesz ustawić nazwę i opcjonalne logo albo wysłać zgłoszenie do istniejącego gangu.
Enter zatwierdza poprawną nazwę lub wyszukiwanie. Na każdej stronie wyników
można przewinąć wszystkie 12 gangów; wyszukiwanie i przełączanie stron pozostają na miejscu.
Zgłoszenie przyjmuje lider lub osoba z uprawnieniem do zgłoszeń.

Po dołączeniu: kompaktowy nagłówek, zakładki „Gang”, „Członkowie”, „Rangi”, „Rejestr” i „Mapa”.
Główna zakładka ma wyśrodkowane logo i nazwę nad linią obejmującą całą szerokość
obszaru treści. Poniżej „Informacje o gangu” i trzy równomierne kolumny:
liczba członków, lider oraz miejsce na poziom gangu. Członkowie nie mają limitu.
Pole poziomu pokazuje „— / Wkrótce”; system zdobywania poziomów jest do ustalenia.
„Ustawienia” znajdują się na dole nawigacji, nad „Opuść gang”.
Mapa pokazuje rozpoznane tereny, pozwala przesuwać zapisany obraz, ręcznie oznaczać
chunki i rozpoczynać lub kontestować capty. Kontrolę terytoriów aktywuje lider.
Rejestr pokazuje rzeczywiste wydarzenia: utworzenie gangu, dołączenia, odejścia,
usunięcia członków, przekazanie przywództwa i przypisywanie rang członkom.
Zmiany kolorów, logo, definicji i kolejności rang nie tworzą nowych wpisów.
Historyczne wpisy ze starszych światów są zachowane. Data jest pełna,
np. „21 października 2026 o godzinie 18:15”, według czasu komputera gracza,
w osobnym wierszu nad opisem. Magazyn i pojazdy będą źródłami zdarzeń po
zaimplementowaniu tych systemów. Historia finansowa pozostaje osobną częścią salda.
Wpisy powstają na serwerze i zapisują się ze światem; ostatnie 10 000 wydarzeń,
po 100 na stronę, od najnowszego. Wpisy na bieżącej stronie przewija się
pionowo kółkiem lub paskiem po prawej; nagłówek i numer/przyciski stron pozostają
nieruchome. Pasek obsługuje strzałki, Page Up/Down i Home/End po zaznaczeniu.
Zmiana strony ustawia listę na początku. Krótka lub pusta strona nie przewija się.
Długie wpisy zawijają się, a zmiana skali GUI zachowuje pozycję w granicach listy. Nie ujawnia nazw osób z prywatnych zgłoszeń.
Starsze zapisy zaczynają z pustym rejestrem; wydarzenia nie są odtwarzane wstecz.

Transport ma wyłącznie ukrytą definicję kategorii w kodzie. Nie ma przycisku,
widoku, zadań działających w tle ani mechanik transportu. Próba wybrania jego
nieaktywnego identyfikatora wraca do głównej zakładki. Saldo gangu i premie
mają działające formularze oraz wyszukiwaną, stronicowaną historię. Czarna lista
blokuje zgłoszenia i przyjęcie po UUID gracza oraz ma własną historię. Cele
pokazują tygodniowy postęp istniejących mechanik, na razie bez nagród. Tworzenie i edycję zestawów ubrań
usunięto z katalogu uprawnień; starsze zapisy są migrowane automatycznie.

Własne rangi tworzy się w **Rangi → + Nowa ranga**. Osobne okno „Nowa ranga”
ma nazwę, uprawnienia i „Zapisz rangę”. Anuluj, X i Esc odrzucają nowy szkic.
Zmiana skali GUI zachowuje wpisaną nazwę i wybrane uprawnienia. Można
ustawić nazwę i uprawnienia: zgłoszenia, usuwanie członków, zarządzanie rangami
oraz wspólny wygląd/logo/baner. Dostępna jest osobna zmiana rang członków.
Pełny [katalog uprawnień](docs/UPRAWNIENIA.md) ma wyszukiwanie, grupy i przewijanie.
Prawa przyszłych systemów są widoczne jako nieaktywne. Osoby bez uprawnień mogą przeglądać rangi.
Lista rang przewija się niezależnie od formularza. Nową rangę można zapisać
Enterem z pola nazwy. Przeciąganie przy krawędzi przewija listę.
Przewijanie trwa także po zatrzymaniu przeciąganego wiersza przy krawędzi.
Przeciąganie ustawia kolejność; także ranga bez uprawnień może być wyżej od
Lidera. Pozycja na liście nie daje uprawnień ani własności gangu. Uprawnienia
Lidera są stałe. Zachowano roboczy limit dawnego systemu: 12 rang łącznie.

Lista członków pokazuje nazwę, rangę, status online/offline oraz licznik online / wszystkich.
Kliknięcie otwiera kartę obok wiersza: portret górnej części skina, nazwa,
ranga, status i data dołączenia. Kliknięcie poza kartą, jej X lub pierwszy Esc
zamyka kartę. Skórka pochodzi z bieżących danych Minecrafta albo zapisanego
profilu; w czasie ładowania/braku danych używana jest domyślna skórka gry.
Brakujące starsze daty są odzyskiwane z pasujących wpisów rejestru, z oznaczeniem
„według rejestru”. Jeśli nie ma takiego zapisu, data pozostaje nieznana.
Lista członków służy przeglądaniu. Czynności dostępne dla danej osoby znajdują
się w **Ustawienia → Zarządzanie**. „Zmień rangę” przełącza wybranego członka
na kolejną dostępną rangę. Przekazanie przywództwa wymaga obecnego lidera.
Serwer sprawdza każde uprawnienie niezależnie od stanu przycisków. Prywatne
zgłoszenia otrzymują tylko lider i osoby z uprawnieniem do zgłoszeń.

Lider przed odejściem przekazuje przywództwo, jeśli ma innych członków.
Odejście ostatniego członka usuwa pusty gang po potwierdzeniu; najpierw musi
on wypłacić pozostałe środki gangu. Pieniądze osobiste pozostają na koncie po odejściu.

## Kolory i wygląd

Ustawienia mają sekcję „Wygląd” z podsekcjami „Kolory menu”, „Logo”, „Baner”
oraz, dla uprawnionych osób, osobne „Zarządzanie”.
„Zmień kolor…” otwiera koło barw, pola HEX/RGB i pionowe suwaki z gradientami:

- **Jasność:** wybrany kolor u góry, czerń na dole.
- **Nasycenie:** nasycony kolor u góry, odcień neutralny na dole.
- **Krycie tła:** pełne krycie u góry, przezroczystość na szachownicy na dole.

Suwaki obsługują kliknięcie, przeciąganie i klawisze strzałek; Home ustawia 100%,
End 0%. Po „Zastosuj” widzisz podgląd menu. Każdy członek może zapisać wygląd
osobisty albo używać wyglądu gangu. Wspólny motyw i logo zapisuje lider lub osoba z uprawnieniem do wyglądu.
Osobisty wygląd nie zmienia ustawień u pozostałych graczy.

## Własne logo

Importer nie analizuje symboliki, treści ani nazw obrazów i nie ma listy
zakazanych flag. Własne grafiki i flagi przechodzą tę samą ścieżkę odczytu,
kadrowania oraz zapisu. Sprawdzane są poprawność pliku i limity zasobów.

Wybierz obraz z systemowego okna albo przeciągnij jeden plik z folderu
na otwarty panel gry. Pole ścieżki i przycisk ręcznego wczytywania usunięto.
 PNG/JPG/JPEG/GIF/BMP oraz WebP, SVG, TIFF, ICO, ICNS, PSD, TGA, PCX i PNM. Plik rastrowy do 80 MB; SVG do 8 MB. W razie potrzeby użyj F11,
aby widzieć grę i folder jednocześnie.

Obrazy 256×256, poziome, pionowe i inne proporcje są przyjmowane. GIF używa
pierwszej klatki. Limit wymiarów rastra: 100 000 px na bok i miliard pikseli;
roboczy obraz zachowuje do 8192 px na dłuższym boku i 16 777 216 pikseli łącznie.
Sam mały podgląd edytora ma do 2048 px; zapis korzysta z większego obrazu roboczego.

W oknie „Dopasuj logo” przesuwaj obraz, przybliżaj kółkiem lub suwakiem.
Wybierz kwadratowy kadr albo cały obraz z zachowaniem proporcji i przezroczystymi
marginesami. „Anuluj”/X/Esc zachowuje poprzednie logo. „Zastosuj logo” przygotowuje
podgląd; zapisz go przez „Utwórz gang” lub Ustawienia → Wygląd → Logo → „Zapisz logo gangu”.

Logo jest zapisywane jako bezstratny PNG **2048×2048**, do 17 MB. Nagłówek,
podgląd i główna zakładka wyświetlają cały obraz z wygładzaniem skalowania. Przy pomniejszaniu gra korzysta
z wcześniej przygotowanych poziomów szczegółowości, ograniczając ząbkowanie
i migotanie drobnych wzorów. Poziomy powstają raz po zmianie grafiki, nie co klatkę.
Dla wyraźnego logo przygotuj źródło 2048×2048 lub większe; przy mocnym kadrowaniu
warto mieć większy oryginał. Mniejszy obraz jest przyjmowany, ale powiększanie
nie odzyska brakujących szczegółów. Nie ma wyboru stylu ani przerabiania rysunku
na inny rodzaj grafiki.

Dotychczasowe logo 64×64 i 512×512 nadal się wyświetla. Aby odzyskać detale utracone
podczas wcześniejszego importu, wczytaj ponownie oryginalny plik i zapisz logo.
Pozostali gracze nie potrzebują pliku źródłowego. Import z linku nie jest jeszcze
obsługiwany.


## Baner gangu

Opcjonalny baner jest tłem szerokiego obszaru z logo i nazwą w zakładce Gang.
Nazwa ma osobną ramkę zapewniającą czytelność. Logo, ramka nazwy i baner
mają wspólną oś w środku pełnego pasa, również po oddaleniu obrazu. Baner ustawia się po utworzeniu
gangu w **Ustawienia → Wygląd → Baner**. Formularz tworzenia nadal dotyczy nazwy i logo.

Wybierz jednolity kolor albo obraz. Edytor koloru ma koło barw, jasność,
nasycenie, krycie i pola HEX/RGB. Przy obrazie można zmienić krycie.
„Usuń baner” przygotowuje usunięcie; „Zapisz baner gangu” zatwierdza zmiany
na serwerze. Zamknięcie panelu bez zapisania odrzuca szkic.

Obraz można wybrać z plików lub przeciągnąć, mając otwartą zakładkę Baner.
Importer korzysta z tych samych formatów i limitów źródła co logo.
Osobne kadrowanie odpowiada proporcjom pasa. Można przesuwać obraz, przybliżać,
oddalać poniżej 1× albo zmieścić całość z przezroczystymi marginesami.
Kolor baneru wypełnia marginesy także przy wybranym obrazie. Wynik ma do
**2048×512**, obecny pas daje PNG **2048×475**;
wyświetlanie zachowuje proporcje i dopasowuje kadr do szerokości pasa.
Logo pozostaje osobnym PNG 2048×2048.
Starsze banery 4K/8K są przy wczytywaniu świata automatycznie zmniejszane
do 2K z zachowaniem proporcji, marginesów, koloru i krycia. Zmiana zapisuje się
ze światem. Klient potrafi również wyświetlić starszą teksturę po zmniejszeniu jej do 2K.
Odczyt zachowuje dawny limit 8192×2048 / 68 MB dla zgodności; nowe zapisy
i tekstury są ograniczone do 2K. Protokół 14 wymaga aktualizacji serwera i klientów do 0.1.24.
Przygotowanie końcowego PNG działa w tle; anulowanie nie zapisuje porzuconego kadru.

Baner zapisuje lider lub osoba z dotychczasowym uprawnieniem do wyglądu/logo.
Serwer ponownie sprawdza uprawnienia, obraz i limity. Baner jest wspólny
oraz zapisywany ze światem. Osobisty motyw menu nadal działa niezależnie.

## Zapis i aktualizacja

Gangi: `data/crimeandlaw_gangs.dat` w danych świata Overworld.
Osobisty motyw: `config/crimeandlaw-appearance.json`.
Starsze zapisy 0.1.0–0.1.2 zachowują gangi, właściciela, członków, zgłoszenia,
logo i wspólny motyw. Zapisane rangi i uprawnienia są odczytywane. W zapisach
bez rang powstają domyślne „Lider” i „Członek”, z odpowiednimi przypisaniami.

Nie ma limitu członków ani zgłoszeń na gang. Listy są wysyłane i wyświetlane
stronami po sześć pozycji; licznik pokazuje pełną liczbę członków. Duży gang
nie powoduje przesłania całej listy w jednym pakiecie. Jeden gang na gracza,
nazwy 3–32 znaki, roboczy limit 512 gangów na świat.

## Budowanie i sprawdzanie

Z JDK 21:

```text
Windows: gradlew.bat build
Linux/macOS: ./gradlew build
```

Wynik: `build/libs/crimeandlaw-0.1.32.jar`.
Gradle Wrapper 9.2.1, ModDevGradle 2.0.148, NeoForge 21.1.248.
`./gradlew test` sprawdza model, migrację zapisów, duże listy i prywatność
zgłoszeń, walidację/edycję logo, kolory oraz skalowanie menu.

`./gradlew -PuiSmoke runClient` uruchamia prawdziwy klient z przykładowymi danymi,
sprawdza menu i edytory oraz zapisuje obrazy i raport w `build/ui-smoke-current`.
Własny katalog próby wybiera `-PuiSmokeId=nazwa`; rozdzielczość:
`-PuiWidth=1280 -PuiHeight=720`.
Wymaga ekranu/OpenGL. Kod z `src/uiTest` jest wyłączony z wydania JAR.
Ten test graficzny korzysta z przygotowanych danych oraz mapy prawdziwego świata
z `build/server-smoke-terrain16a/view.json`. Przed pierwszą próbą graficzną
uruchom test serwera `-PserverSmoke -PserverSmokeId=terrain16a runServer`
w jego osobnym świecie testowym. Osobny test sieciowy
`-PnetworkSmoke -PnetworkSmokeId=nazwa` udostępnia zadania `runNetworkServer`,
`runNetworkOwner` i `runNetworkMember`: dwa klienty łączą się z własnym serwerem
na 127.0.0.1:25597, obsługują menu i sprawdzają synchronizację/uprawnienia.
Raporty trafiają do `build/network-smoke-nazwa`; kod automatyzacji jest wyłączony
z wydania. Szczegóły wykonanych prób: [VALIDATION.md](VALIDATION.md).

## Dalszy zakres

Własność chunków w rozgrywce, oznaczenia, NPC, produkcja, handel i rozbudowane
animacje pozostają dalszymi etapami. Mapa ma już zapis i podgląd stałych cech terenu.

Definicje kategorii: `GangCategory.java`. Tylko `TRANSPORT` ma umiejscowienie `HIDDEN`; aktywację i mechaniki pozostawiono do dalszej pracy.

Rejestr: numer strony i dwie sąsiadujące strzałki w prawym narożniku nagłówka; powiększona lista. Maksymalnie 100 stron po sto wydarzeń.

W 0.1.8 dodano przyciski Premie, Zarządzanie saldem, Czarna lista i Cele. Otwierają panele informacyjne; funkcje tych systemów są do rozwijania. Transport nadal jest ukryty.

## Import logo 0.1.9
Format jest rozpoznawany po danych, a nie rozszerzeniu. Czytniki są dołączone do JAR-a. Przykładowo WebP lub SVG zapisany pod nazwą PNG może zostać odczytany bez ręcznej konwersji. Obraz roboczy zachowuje do 8192 px na dłuższym boku i 16 777 216 pikseli łącznie; mały podgląd ma do 2048 px. Po kadrowaniu do serwera trafia logo PNG 2048×2048 albo osobny szeroki baner do 2048×512. SVG renderuje lokalne kształty wektorowe; odwołania do obrazów i plików zewnętrznych są pomijane. Formatów HEIC/AVIF, PDF i animowanego logo nie obsługuje to wydanie.

## Odczyt logo 0.1.10
Jeśli pierwszy czytnik rozpoznaje format, lecz nie potrafi odczytać danego wariantu,
importer próbuje kolejnych dostępnych czytników. Każda próba zaczyna od osobnego
strumienia danych, a nie od pozycji pozostawionej przez poprzedni czytnik.
Błędy odczytu mają komunikat po polsku; szczegóły czytnika trafiają na poziom DEBUG.
Obsługiwane formaty oraz limity plików pozostają takie jak w 0.1.9.

## Szczegółowość logo 0.1.11
Zapis zwiększono z 64×64 do 512×512. Podniesiono limity walidacji, zapisu świata
i obu kierunków przesyłania; protokół sieciowy 4 wymaga aktualizacji klienta
i serwera. Tekstura jest odczytywana ze strumienia i ładowana tylko po zmianie
logo. Każde miejsce wyświetlania używa pełnego zakresu obrazu oraz filtra liniowego.
Wygładzany jest również podgląd kadrowania. Zachowana obsługa dawnych logo 64×64.

## Rangi i uproszczony formularz logo 0.1.12
Przywrócono własne rangi, edytor uprawnień, przypisywanie i przeciąganie
kolejności. Zdarzenia rang trafiają do Rejestru. Transport nadal jest ukryty.
Formularze tworzenia gangu i ustawień logo mają przycisk wyboru obrazu oraz
obsługę przeciągania pliku. Usunięto pole ścieżki, „Wczytaj i dopasuj”, opis
formatów i informację o opcjonalnym logo/późniejszej zmianie. Import 512×512,
kadrowanie i wyświetlanie pozostają dostępne.


## Porządkowanie panelu 0.1.13
Nową rangę tworzy się w osobnym oknie. Usunięto wskazane instrukcje z rang
oraz wyglądu; opis logo pozostaje. Przycisk zamykania jest kwadratowy,
a X ma równe przekątne i jest wyśrodkowany. Rejestr nie dopisuje konfiguracji
wyglądu ani definicji/kolejności rang; pełna data z rokiem jest nad opisem.


## Mapa świata i stałe cechy terenu
Mapa pokazuje ciągły obraz prawdziwej powierzchni: woda zachowuje przebieg
rzek i brzegów, widoczne są lasy, piasek, skały, śnieg oraz dachy i drogi
wiosek. Każdy piksel danych odpowiada jednej kolumnie bloków; kolory pochodzą
z palety map Minecrafta, a nachylenie i głębokość wody wpływają na cieniowanie.
To mapa barwna z cieniowaniem rzeźby, nie zdjęcie tekstur z góry.

Domyślnie widoczny jest obszar 11×11 chunków (176×176 bloków) wokół gracza.
Przyciski +/− oraz kółko nad mapą zmieniają przybliżenie.
Przeciąganie lewym przyciskiem przesuwa widok już przy wciśniętej myszy.
Kamera zachowuje dowolne położenie między chunkami po puszczeniu myszy,
kolejnym naciśnięciu, przełączeniu siatki, zmianie przybliżenia i odbiorze danych.
Kliknięcia na częściowo widocznych chunkach uwzględniają to przesunięcie.
Zaokrąglenie dotyczy wyłącznie obszaru pobieranych danych. Ruch wewnątrz
tego samego obszaru nie przebudowuje listy opisów ani tekstury mapy.
Klient pobiera obszar 21×21 chunków, z zapasem wokół widocznych 11×11,
i rysuje również ten zapas. Przy przesuwaniu środek pobierania wyprzedza
kierunek ruchu o maksymalnie trzy chunki. Odbiór danych nie przerywa trzymanego przeciągnięcia;
spóźnione odpowiedzi uzupełniają pamięć kafli, zachowując bieżące położenie kamery.
Przesuwanie nie zmienia osobno wybranego chunka;
ikona postaci u góry („Wróć do gracza”) lub Home natychmiast przełącza
na zapamiętaną okolicę postaci i przywraca śledzenie gracza. Działają też strzałki klawiatury
poza zaznaczonym paskiem przewijania. Przycisk „Siatka” włącza granice chunków.
Siatka domyślnie jest wyłączona. Rzeczywista pozycja i kierunek gracza,
północ, podziałka i zaznaczenie chunka są nakładką na krajobraz.
Kliknięcie wybiera chunk; opis po prawej przewija się niezależnie.
Zmiana skali GUI zachowuje wybór i przybliżenie.

Rysunek mapy powstaje z załadowanych bloków przy oglądanym miejscu oraz
z zapisanych na dysku, w pełni wygenerowanych chunków. Odczyt dysku obejmuje
palety bloków i wysokość powierzchni; nie tworzy żywych chunków, encji ani biletów
utrzymujących teren w pamięci. Nie generuje nowych terenów ani pustych plików
regionów. Niewygenerowane lub nieczytelne obszary pozostają zasłonięte. Odczytany obraz zapisuje się ze światem osobno dla
każdego wymiaru i chunka, przeżywa restart i odświeża się po przebudowie
wczytanego terenu. Dane są wspólne dla serwera; nie dodano jeszcze osobistej
mgły odkrycia ani przeglądu całego świata naraz. Można przeglądać zapisane
wcześniej fragmenty i istniejące pełne chunki ze świata po odsunięciu widoku
od gracza. Odczyt palet z pliku jest niezależny od utrzymywania chunków w świecie.
Nie analizuje się przez to automatycznie cech naturalnych odległych chunków.
Mapa jest odświeżana tylko podczas jej przeglądania, a nie w tle dla wszystkich
chunków. Dane powierzchni przychodzą stopniowo, niezależnie od analizy cech.

Obraz jest osobny od **stałych cech naturalnego terenu**. Zmiana budynku
zmienia rysunek, ale nie pierwotne cechy. Własny budynek widoczny na mapie
nie przyznaje cechy wygenerowanego budynku ani premii.

Rozpoznawane są cechy podłoża, obecność wody, leśny charakter i różnice
wysokości. Mogą występować razem z obiektami wygenerowanej wioski: farmą,
warsztatem, zagrodą, domem rybaka, biblioteką lub garbarnią. Wioska liczy się
w chunku, jeśli przecina go choć fragment wygenerowanej części struktury.
Identyfikacja obiektów korzysta z danych struktur i nazw szablonów wiosek
vanilla. Inne mody z własnymi szablonami mogą wymagać rozszerzenia reguł.

**Cechy zapisują się na stałe**, osobno dla wymiaru i współrzędnych chunka.
Wycinka, zburzenie warsztatu i przebudowa nie zmieniają zapisanego profilu.
Postawienie własnego stołu kowalskiego lub farmy nie dodaje cechy wygenerowanego
budynku. Pełna kolumna chunka ma jeden profil.

Nowo generowane chunki zapisują pomiary w chwili pierwszego wczytania:
16 próbek podłoża/wysokości i dane biomów. Zapis źródłowy jest przechowywany
w danych chunka. Dane budynków są dołączane przy analizie mapy na podstawie
wygenerowanych struktur, z zachowaniem oryginalnych pomiarów.

**Starsze chunki:** jeśli powstały przed instalacją tego wydania, nie da się
odzyskać z nich dokładnego stanu sprzed przebudowy. Wtedy używany jest
bazowy generator terenu i jego biomy, a źródło pokazuje „rekonstrukcja
generatora”. Pokrycie powierzchni jest wtedy przybliżane z biomu, bez
odczytywania budowli graczy; nie jest to dokładna rekonstrukcja wszystkich
dekoracji i zmian generatora z dawnych wersji. Taki profil także zostaje stały.

Analiza korzysta z dostępnych chunków. Nie generuje odległych obszarów.
Brak wczytanego początku struktury wioski wstrzymuje zapis danego profilu,
aby nie utrwalić niepełnej listy budynków. Należy podejść bliżej wioski.
Praca jest porcjowana: maksymalnie dwa kroki na tick serwera i miękki budżet
2 ms; pojedyncza operacja generatora może ten budżet przekroczyć. Kolejka
ma najwyżej 128 zadań; nieodświeżane zadania wygasają po 200 tickach.

Trwałe profile i przygotowana własność: `data/crimeandlaw_terrain.dat` w
Overworld. API własności i opisu posiadanego terenu działa po stronie serwera;
klient nie może przesłać właściciela ani cech. Przyszły kontroler zakupu/walki
będzie z niego korzystał. Sam interfejs jeszcze nie pozwala zajmować ziemi.
Nie ma aktywnych mnożników wzrostu, nowych receptur ani kamizelek.
Zwykły teren bez premii jest normalnym przypadkiem. Cechy nie są automatycznymi
bonusami: sekcja „Potencjał do rozwinięcia” została usunięta. Konkretne rzadkie
premie będą wymagały własnych, łącznych kryteriów krytycznych oraz wystarczających
danych. Ich lista, progi i wartości nie zostały jeszcze ustalone. Próbkowanie
16 punktów nie jest pełną oceną kwalifikacji. Nie przyznano mnożników za same
biomy, wodę, fragment wioski ani samą liczbę posiadanych chunków.

Dodatkowy test integracyjny: `./gradlew -PserverSmoke runServer` uruchamia
jednorazowy serwer testowy w `run-server-smoke-current` (wymaga zaakceptowania EULA
w tym katalogu). Drugi start tego samego świata sprawdza ponowny odczyt danych.
Kod testów serwera i klienta jest wyłączony z JAR-a wydania.
Opcja `-PserverSmokeId=nazwa` wybiera osobny świat i katalog raportu dla danego testu.

Rysowanie powierzchni ma osobną kolejkę do 1024 zadań, najwyżej 2048 kolumn na
tick i miękki limit 2 ms; pojedynczy krok może przekroczyć ten budżet. Brakujące
fragmenty widocznej okolicy mają pierwszeństwo przed pobieraniem zapasu i odświeżaniem.
Kolejka śledzi aktualne obszary najwyżej 64 oglądających; starsze pozycje tej samej
osoby zastępuje najnowsza. Nieaktywne żądania wygasają po 100 tickach. Odczyt
plików: najwyżej cztery nowe operacje na tick i osiem w toku. Porzucenie starego
widoku nie zwalnia miejsca I/O przed rzeczywistym zakończeniem jego odczytu.
Dekodowanie palet działa w tle. Tylko
gotowe kafle są zapisywane. Ponowny odczyt tego samego wczytanego chunka
następuje najwcześniej po 100 tickach. Klient utrzymuje jedną teksturę mapy,
aktualizowaną przy zmianie pikseli, a nie przebudowywaną w każdej klatce.

Test interfejsu 0.1.16 korzysta także z danych prawdziwej mapy: przed
`./gradlew -PuiSmoke runClient` wykonaj dwa starty testu serwera z
`-PserverSmokeId=terrain16a`. Jego raport `build/server-smoke-terrain16a/view.json`
jest wejściem do sprawdzenia tekstury oraz ostatnich zrzutów klienta.


## Dopracowanie panelu — 0.1.16

Wyniki wyszukiwania i lista rang mają niezależne przewijanie; przyciski oraz
formularze nie przesuwają się wraz z listą. Pusty szkic nazwy rangi zachowuje
się przy zmianie rozmiaru, a przeciąganie do krawędzi przewija listę.
Niepotrzebne paski przewijania są ukrywane. Zbyt długie napisy przycisków
mają wielokropek i podpowiedź z pełnym tekstem. Puste kategorie przyszłych
systemów mają wspólny wygląd, bez udawanych danych. X działa także przed
utworzeniem gangu. Na mapie dodano przesuwanie oraz szybki powrót do gracza.


## Poprawki interfejsu — 0.1.17

Zniknęły strzałki pod mapą; powrót do gracza ma własną ikonę w nagłówku.
Plus, minus i Odśwież mają wyśrodkowane napisy, a aktywna Siatka pełną,
pogrubioną ramkę. Przesuwanie mapy nie wybiera innego chunka; kliknięcie
wybiera teren, a opis wybranego obszaru pozostaje podczas odsuwania widoku.

Strzałki stron i przyciski czynności mają wyśrodkowane napisy/symbole.
Nawigacja, nazwy rang i etykiety uprawnień pozostają po lewej.
Uprawnienia mają przełączniki z opisem stanu dla czytnika ekranu.
Enter zatwierdza formularz z pola tekstowego i zachowuje działanie zaznaczonego
przycisku. Tworzenie gangu ma uporządkowaną kartę i wyróżniony przycisk główny.
Nie dodano magazynów, pojazdów, finansów ani innych przyszłych systemów.


## Korekty po 0.1.17 — 0.1.19

Baner wypełnia całą strefę tożsamości gangu. Większe logo i baner wymagają
ponownego importu oryginału, aby odzyskać szczegóły utracone w starszym zapisie.
Kadrowanie pozwala pokazać cały obraz, oddalać i ustawić położenie na kolorowym tle.
Logo i baner znajdują się wewnątrz Wyglądu.

Pierwsze otwarcie czeka na dane serwera; kolejne zachowuje znany stan gangu.
Mapa kończy przeciąganie przy puszczeniu myszy i używa ograniczonej pamięci
1024 kafli, aby szybki powrót nie usuwał rozpoznanego obrazu. Nie wczytuje
ani nie generuje odległego świata. Ikona gracza jest symetryczna.
Portret obsługuje widoczne warstwy głowy, tułowia i obu rąk.

Dawne zarządzanie rangami migruje do edycji rang i osobnej zmiany rangi członka.
Nowy katalog liczy 25 praw: pięć działających i 20 zablokowanych definicji
przyszłych systemów. Blokada jest sprawdzana także na serwerze.

Duże obrazy są przesyłane w małych częściach i składane w całość przed użyciem,
aby nie przekraczać granic pojedynczej wiadomości Minecrafta. Serwer sprawdza
kompletność, kolejność, rozmiar oraz czas oczekiwania. Zapisane PNG pozostają całe.


## Przeciąganie mapy — 0.1.20

Poprawiono cały gest, w tym ruch w wielu kierunkach z trzymaną myszą.
Kamera jest liczona w przestrzeni świata, zamiast przesuwać ograniczony stary
prostokąt obrazu. Zapas obrazu i znane sąsiednie kafle wypełniają brzegi podczas
ruchu. Odpowiedzi aktualizują istniejący panel; nie niszczą fokusu ani gestu.
Puszczenie myszy kończy podgląd bez ponownego przesunięcia i bez wyboru chunka.

Serwer zbiera dane przy oglądanym miejscu i czyta istniejący zapis pełnych
chunków również poza załadowaną okolicą gracza. Brakujące, dostępne fragmenty
są dopytywane do czterech razy na sekundę; mapa nie przesyła ponownie logo/baneru.
Nieczytelne starsze formaty zapisu nie są zgadywane ani automatycznie generowane.


## Szybsza mapa — 0.1.24

Klient przechowuje do 8192 fragmentów mapy. Zapamiętana okolica gracza ma
pierwszeństwo przy usuwaniu starych danych, dzięki czemu Home może pokazać ją
bez oczekiwania na nową odpowiedź. Rozkodowane kolory mają osobną pamięć
do 1024 różnych obrazów; nie są odczytywane ponownie przy każdym przesunięciu.

Jedno aktualne żądanie mapy czeka na odpowiedź, a kolejne ruchy aktualizują
jego następny cel. Minimalny odstęp wysyłania to 150 ms; po dwóch sekundach
bez odpowiedzi można ponowić pobieranie. Numery żądań zapobiegają odblokowaniu
nowego pobierania przez starą odpowiedź. Kamera porusza się niezależnie od sieci.
Spóźnione dane nadal mogą uzupełnić pamięć obrazów.

Pełna kolejka pomija nadmiar pracy; kamera zachowuje swój cel i następne
żądania mogą uzupełnić brakujące dane. Obrazy spoza pamięci są pobierane
ponownie. Nie obiecuje to natychmiastowego pierwszego odczytu każdego terenu:
niewygenerowane obszary pozostają zasłonięte, a mapa nie generuje świata.


## Sąsiedzi neutralnego captu — 0.1.26

Nowy rywal może dołączyć do walki o neutralny chunk, jeśli jego gang posiada
co najmniej jeden z ośmiu przylegających chunków, również po przekątnej.
Lider wybiera pole na mapie i potwierdza dołączenie; sama ranga z uprawnieniem
udziału nie pozwala rozpocząć udziału całego gangu bez decyzji lidera.
Gdy gang już uczestniczy, jego uprawnieni członkowie dołączają do własnej drużyny.
Menu podaje powód odmowy, a serwer ponownie sprawdza te same warunki przy akcji.
Zasady obecności i zakończenia zapisów pozostają obowiązujące. Protokół 16.


## Komunikaty przy kadrowaniu — 0.1.27

W oknie dopasowania logo i banera instrukcje przesuwania obrazu i kółka
myszy pozostają pod podglądem. Status przygotowania i błędy wyświetlane
są w osobnym obszarze prawej kolumny, pod narzędziami kadrowania.
Dłuższy błąd zawija się w obrębie tej kolumny, nad przyciskami dolnymi.


## Znacznik gracza — 0.1.28

Znacznik na mapie ma symetryczną sylwetkę, wyśrodkowaną na osi obrotu.
Pełna czarna obwódka otacza również dolny czubek. Pozycja i obrót
nadal odpowiadają dotychczasowym danym gracza.
