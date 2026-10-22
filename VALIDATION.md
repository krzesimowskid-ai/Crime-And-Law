# Sprawdzenie 0.1.16

22 października 2026. Minecraft 1.21.1, NeoForge 21.1.248, JDK 21.

## Zmiana

Dopracowanie istniejącego panelu gangów: zamknięcie przed dołączeniem,
walidacja nazwy, Enter, niezależne przewijanie wyników wyszukiwania i rang,
nieruchome formularze/przyciski, zachowanie pustego szkicu po zmianie rozmiaru,
przewijanie przy przeciąganiu rang, ukrywanie zbędnych pasków przewijania,
podpowiedzi dla uciętych przycisków i spójne puste kategorie przyszłych systemów.
Zachowano zaakceptowany układ i kolory menu.

Mapa: przeciąganie z podglądem, przyciski kierunków, klawisze strzałek,
Home/przycisk Gracz oraz przybliżanie kółkiem. Klient łączy szybkie żądania,
odrzuca nieaktualne odpowiedzi i nie rysuje odległego znacznika nad opisem.
Serwer sprawdza współrzędne i odczytuje odległy widok wyłącznie z zapisanych
kafli. Analiza nowych danych nadal odbywa się tylko wokół gracza.
Sekcja „Bonusy terenu” pozostaje osobna od zakładki „Premie”.

## Testy jednostkowe

72 testy JUnit, bez błędów, niepowodzeń i pominięć.
Trzy nowe próby granic widoku obejmują ujemne współrzędne, skrajne pozycje,
bardzo duże przesunięcia i odrzucenie pozycji poza światem.
Zachowano wcześniejsze testy członkostwa, uprawnień, prywatności, rejestru,
logo, kolorów, profili generowania, mapy, zapisu oraz kodeków pakietów.

## Rzeczywisty serwer dedykowany

CRIME_TERRAIN_SERVER_PASS: FIRST_RUN oraz RESTART tego samego izolowanego
świata `terrain16a`, ziarno 123456789, warsztat w chunku (-17, 12), Overworld.

- Wygenerowana wioska/warsztat, zapis źródłowy i rekonstrukcja starszego chunka.
- Stały profil mimo przebudowy; pomijanie wysokiej konstrukcji gracza.
- Co najmniej 49 kafli odczytanych z prawdziwych bloków.
- Kolory poszczególnych kolumn i obu stron granicy chunków.
- Zmiana bloku odświeża raster, zachowując stałe cechy.
- Restart zachowuje piksele, profile, pomiary źródłowe i lokalną własność.
- Odległy widok (400000, -400000) zwraca wcześniej zapisany kafel, nie ładuje
  docelowego chunka i nie zmienia liczby wczytanych chunków.
- Widoki przy wszystkich czterech narożnikach granicy świata pozostają poprawne
  i nie wczytują terenu.
- Pozycja gracza pozostaje rzeczywistą pozycją, niezależną od środka mapy.

Bilety podtrzymujące wioskę i bloki kontrolne istnieją wyłącznie w świecie
tej automatycznej próby. Kod produkcyjny nie dodaje takich biletów.

## Rzeczywisty renderer Minecrafta

CRIME_UI_SMOKE_PASS w 1920×1080 i 1280×720, co najmniej 47 etapów na przebieg,
skale GUI 2 oraz maksymalna dostępna (4 albo 3).

Sprawdzono wszystkie obecne kategorie, tworzenie/wyszukiwanie, listę 12 gangów,
12 rang i ich przewijanie, zmianę rozmiaru z pustą nazwą rangi, przeciąganie
z przewijaniem, rejestr i strony, zarządzanie, kolory, import/kadrowanie logo,
nieaktywność ukrytego Transportu oraz zachowanie uprawnień w interfejsie.

Próby mapy obejmują wybór i przewijanie opisu, zoom kółkiem/przyciskami,
siatkę, podgląd podczas przeciągania, przesunięty widok, odrzucenie starej
odpowiedzi, przyciski kierunków, Home i powrót do gracza oraz przycięcie
znacznika gracza poza mapą. Część interakcji używa syntetycznego zestawu
cech i testowej rzeki. Końcowe ujęcia i porównanie pikseli korzystają
z danych rzeczywistej wioski powyższego serwera, nie z narysowanej makiety.
Wizualnie obejrzano gang, listy, puste kategorie i mapę w obu rozdzielczościach.

## Dwa klienty i restart

CRIME_NETWORK_OWNER_PASS, CRIME_NETWORK_MEMBER_PASS i CRIME_NETWORK_RESTART_PASS.
Dwa rzeczywiste klienty Minecrafta (960×540, konta deweloperskie Owner16/Member16)
połączone przez TCP z izolowanym serwerem na 127.0.0.1:25597; próba `net16b`.

- Utworzenie gangu, znalezienie, zgłoszenie i przyjęcie przez faktyczne menu.
- Zwykły członek nie widzi zarządzania ani prywatnych zgłoszeń; próba zmiany
  wspólnego motywu bez uprawnień zostaje odrzucona na serwerze.
- Lider tworzy rangę Stylista i przypisuje ją drugiemu graczowi. Uprawnienie
  wyglądu odblokowuje odpowiedni przycisk, bez dostępu do rang i zgłoszeń.
- Import pliku, okno kadrowania i zapis logo poprzez kontrolki klienta.
  Szczegółowy PNG 512×512, 1 200 288 znaków Base64: faktyczne przesłanie
  klient → serwer → oba klienty; zgodne SHA-256 danych i tekstura 512×512
  u odbiorcy. Ten przypadek przekracza dawny limit krótkiego ciągu znaków.
- Zapis wspólnego tła przez koło kolorów/pole HEX; zmiana dociera do obu klientów.
- Delegowana osoba nadal nie może usunąć właściciela. Serwer odrzuca pakiet;
  członkostwo dwóch graczy pozostaje nienaruszone.
- Klienty zamknięto, serwer zapisano i zatrzymano. Ponowny start tego samego
  świata potwierdził członków, właściciela, rangę/uprawnienia, tło i duże logo.

W pierwszej próbie `net16a` serwer dostał DISPLAY środowiska testowego,
a Java ImageIO próbowała użyć niedostępnej biblioteki X11. To nie jest wynik
zaliczony. Powtórzenie `net16b` i restart używały `java.awt.headless=true`;
ustawienie dodano też do zadania serwera tej próby. Ostrzeżenia usług uwierzytelniania
wynikają z lokalnych kont deweloperskich/offline, nie z prawdziwego logowania.

## Odtwarzanie

```text
./gradlew build --rerun-tasks
./gradlew -PserverSmoke -PserverSmokeId=terrain16a runServer
./gradlew -PserverSmoke -PserverSmokeId=terrain16a runServer
./gradlew -PuiSmoke -PuiSmokeId=menu16c runClient
./gradlew -PuiSmoke -PuiSmokeId=menu16-1280 -PuiWidth=1280 -PuiHeight=720 runClient
```

Test serwera wymaga zaakceptowania EULA w swoim izolowanym katalogu.
Test graficzny wymaga ekranu/OpenGL i używa pliku
`build/server-smoke-terrain16a/view.json`. Raporty i zrzuty trafiają do
`build/ui-smoke-<identyfikator>`.

Test sieciowy: najpierw zbudować klasy z `-PnetworkSmoke -PnetworkSmokeId=net16b`,
a następnie uruchomić `runNetworkServer`, `runNetworkOwner`, `runNetworkMember`
z tymi samymi właściwościami w oddzielnych terminalach (serwer jako pierwszy).
Świat próby musi być nowy, z EULA oraz `server-ip=127.0.0.1`, `server-port=25597`,
`online-mode=false`. To wyłącznie izolowany serwer testowy. Po raportach
owner.PASS/member.PASS zatrzymać serwer poleceniem stop i uruchomić go z
`-PnetworkPersistence=true`, aby otrzymać restart.PASS. Próby wykonywane tu
użyły przygotowanych przez Gradle parametrów startu bez równoległego kompilowania.

JAR wydania nie zawiera klas automatyzacji z `src/uiTest`, `src/serverTest`
ani `src/networkTest`; zawiera 15 bibliotek importu obrazów.
Wersja 0.1.16 i protokół 8 wymagają aktualizacji klienta i serwera.

## Granice sprawdzenia

Widoczny wycinek ma najwyżej 176×176 bloków, z przybliżeniem do 80×80.
Można przesuwać go po zapisanym atlasie. Nie ma osobistej mgły odkrycia,
widoku całego świata naraz ani pobierania nieznanego odległego terenu.
Atlas jest wspólny na serwerze, osobny dla każdego wymiaru.
Przetestowano powierzchnię Overworld; nie sprawdzano czytelności pod sufitem Netheru.

Odczyt powierzchni: najwyżej 128 kolumn na tick, porcje po 32 i miękki budżet
2 ms; pojedynczy krok może go przekroczyć. Kolejka do 256 zadań, wygaśnięcie
po 200 tickach, ponowne odświeżenie nie wcześniej niż po 100 tickach.
Praca obejmuje tylko dostępne chunki FULL. Klient utrzymuje jedną teksturę,
aktualizowaną po zmianie pikseli. Nie wykonano benchmarku dużego serwera.

Stałe cechy nadal opierają się na 16 próbkach i szablonach wiosek vanilla.
To opis terenu, nie silnik kwalifikacji rzadkich bonusów. Brak przejmowania,
ekonomii, receptur, roślin i aktywnych mnożników. Premie, Saldo, Czarna lista
oraz Cele mają tylko przygotowane panele. Transport pozostaje ukryty.
Import nie obsługuje HEIC/AVIF, PDF ani animowanego logo; pozostałe limity
są opisane w README. Te sprawdzenia nie oznaczają gwarancji braku błędów
na każdej konfiguracji i w dowolnej paczce modów.

Lokalna poprawka narzędzia NeoForm Runtime i konfiguracja Xvfb nie trafiają
ani do źródeł moda, ani do JAR-a.
