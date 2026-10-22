# Sprawdzenie 0.1.18

22 października 2026. Minecraft 1.21.1, NeoForge 21.1.248, JDK 21.

## Zakres

- Baner bez odstępów od krawędzi całego pasa tożsamości; własna ramka nazwy.
- Logo 1024×1024, baner do 2048×512 (obecny pas: 2048×475), PNG do 4,3 MB.
  Starsze grafiki są nadal odczytywane; utracone szczegóły wymagają ponownego
  importu oryginału. Źródło jest pomniejszane do najwyżej 2048 px na dłuższym boku.
- Kadrowanie z oddalaniem poniżej 1×, zachowaniem całego obrazu i przesuwaniem
  także z marginesami. Kolor baneru jest tłem przezroczystych fragmentów PNG.
- Logo i baner są wewnątrz Wyglądu. Zarządzanie pozostaje osobną sekcją.
- Portret: oddzielne warstwy głowy, tułowia i obu rękawów, także slim,
  z mieszaniem alfa. Data starego członkostwa odzyskiwana z pasującego ostatniego
  wpisu rejestru, oznaczona „według rejestru”; brak wpisu nie tworzy nowej daty.
- 25 praw z grupami, wyszukiwaniem i przewijaniem. Pięć działa; 20 planowanych
  opcji jest zablokowanych także na serwerze. Osobne przypisywanie rang,
  ze zgodną migracją dotychczasowych zarządzających rangami.
- Przesuwanie mapy kończy podgląd natychmiast. Ograniczona pamięć 1024 kafli
  utrzymuje dane z serwera przy szybkim powrocie, bez generowania świata;
  oddzielne wymiary i poprawne narożniki granicy świata.
- Symetryczna ikona powrotu do gracza. Ładowanie pierwszego stanu bez migającego
  formularza tworzenia; kolejne otwarcie zachowuje znany stan.

## Testy jednostkowe i budowanie

91 testów JUnit: 0 błędów, 0 niepowodzeń, 0 pominiętych. Końcowe `build`
zakończone sukcesem. Nowe próby obejmują zachowanie skrajnych części obrazu
po dopasowaniu, oddalanie i przesuwanie, odzyskanie dat wyłącznie z zapisów,
migrację/rozdzielenie uprawnień i odrzucenie każdej przyszłej opcji bez mutacji.
Pamięć mapy sprawdzona przy powrocie bez odpowiedzi serwera, niepełnym
odświeżeniu, ograniczeniu rozmiaru, zmianie wymiaru i narożnikach świata.

Pięć dodatkowych prób transportu sprawdza małe części przez prawdziwy
Varint21LengthFieldPrepender, kompletność tekstu Unicode, granice rozmiaru,
kolejność, rozdzielenie połączeń i wygasanie nieukończonych transferów.

Sprawdzono rzeczywiste szczegółowe obrazy w kodekach i zapisie: logo 1024×1024
oraz baner 2048×512. JSON obu grafik przekracza 7 mln znaków; kodeki zachowują
wszystkie dane i odrzucają przekroczenie granic. Zachowane poprzednie próby
rang, rejestru, importu formatów, mapy, kolorów i migracji.

## Rzeczywisty renderer Minecrafta

Zaliczone `menu18c-1280` (1280×720, GUI 2/3) i `menu18-final`
(1920×1080, GUI 2/4). Po 59 zrzutów oraz asercje rzeczywistych kontrolek i pikseli.

Sprawdzono wcześniejsze kategorie i edytory, nowe obrazy i kadrowanie,
pełne krawędzie baneru, podsekcje wyglądu, ładowanie bez formularza,
fokus Enter, ciągłe przewijanie rang, mapę i zachowanie wyboru chunka.
Szybki ruch w obie strony jest wykonywany bez pośredniej odpowiedzi serwera;
każdy wcześniej znany kafel zachowuje piksele, a przesunięcie podglądu wynosi zero.
Przyszłe prawa są wyszukiwane i nie zmieniają zaznaczenia po kliknięciu/Spacji.
Krótka filtrowana lista nie pokazuje zbędnego paska przewijania.

Dwie tekstury testowe skinów wide/slim przechodzą przez tę samą funkcję
portretu co karta członka, w prawdziwym rendererze. Piksele potwierdzają
półprzezroczystą nakładkę głowy nad podstawą, kurtkę i obie zewnętrzne rękawy.
Wizualnie obejrzano baner, rangi, ustawienia, mapę i próbę warstw.
Próba nie wymaga pobrania skina z usług Mojang. Pobranie nowego niestandardowego
profilu z Mojang pozostaje niezweryfikowane w tym środowisku; normalna karta
używa PlayerInfo/SkinManager i zapamiętanej publicznej właściwości textures.

Część danych GUI jest syntetyczna. Końcowa mapa wioski pochodzi z nowego
przebiegu dedykowanego serwera `terrain16a` w 0.1.18: rzeczywista wygenerowana
farma (chunk 38, 51), 49 kafli, granice chunków, aktualizacja powierzchni,
niezmienność pierwotnego profilu, odległy zapisany widok i cztery narożniki świata.
Każdy dostępny piksel porównano z teksturą klienta. W tym wydaniu wykonano
FIRST_RUN tej próby; trwałość danych gangu sprawdzono osobnym restartem poniżej.

## Dwa klienty i zapis serwera

`net18d`: dwa rzeczywiste klienty 960×540, lokalne konta deweloperskie
Owner16/Member16, własny serwer 127.0.0.1:25597. OWNER_PASS i MEMBER_PASS.

- Utworzenie, znalezienie gangu, zgłoszenie, przyjęcie, utworzenie i przypisanie
  rangi przez kontrolki gry oraz rzeczywiste pakiety.
- Serwer odrzuca zmianę wspólnego wyglądu i baneru przez zwykłego członka.
  Po nadaniu samego prawa wyglądu oba klienty dostają aktualizację; prywatne
  zgłoszenia i zarządzanie rangami/członkami pozostają niedostępne.
- Szczegółowe losowe obrazy: logo 1024×1024 (4799736 znaków Base64),
  baner 2048×475 (4388836 znaków). Oba są jednocześnie w stanie gangu.
  Sprawdzono pełny przesył klient → serwer → oba klienty, dokładne SHA-256
  i natywne rozmiary tekstur. Nie ograniczono próby do jednobarwnych PNG.
- Licznik dwóch osób online i prawdziwe daty dołączenia pochodzą z serwera.
  Próba usunięcia właściciela bez uprawnień zostaje odrzucona.
- Po poprawnym zamknięciu obu klientów i zatrzymaniu/zapisie serwera ponownie
  uruchomiono ten sam świat. RESTART_PASS potwierdza członkostwo, właściciela,
  rangę/uprawnienia, wspólny motyw, duże logo i baner oraz daty.

## Odtwarzanie i wydanie

```text
./gradlew build
./gradlew -PuiSmoke -PuiSmokeId=menu18-final runClient
./gradlew -PuiSmoke -PuiSmokeId=menu18c-1280 -PuiWidth=1280 -PuiHeight=720 runClient
```

GUI wymaga ekranu/OpenGL i pliku `build/server-smoke-terrain16a/view.json`.
Dane przygotowuje `./gradlew -PserverSmoke -PserverSmokeId=terrain16a runServer`,
na izolowanym świecie z zaakceptowaną EULA. Przy świeżej próbie terenowej
użyć ziarna 1 jak w tym przebiegu lub dopuścić inne współrzędne wioski.

TCP: przygotować klasy z `-PnetworkSmoke`, uruchomić `runNetworkServer`,
`runNetworkOwner` i `runNetworkMember` z jednakowym świeżym `-PnetworkSmokeId`.
Po raportach zatrzymać serwer przez stop, a następnie uruchomić ten sam świat
z `-PnetworkPersistence=true`. W tym środowisku użyto przygotowanych przez
Gradle parametrów startu i osobnych procesów; podczas właściwej próby
nie przebudowywano klas. Konta testowe nie logują się do usług Mojang.

Protokół 10 wymaga 0.1.18 na serwerze i u wszystkich graczy. W JAR-ze jest
91 produkcyjnych klas i 15 dołączonych bibliotek importu obrazów.
Brak klas automatyzacji UI/serwera/sieci i powtarzających się wpisów ZIP;
wszystkie produkcyjne klasy odpowiadają końcowemu wynikowi kompilacji.

Nie dodano mechanik magazynów, finansów, rajdów, transportu, roślin i multibloków.
Transport nadal nie ma przycisku w głównej nawigacji. Nie wykonano benchmarku
dużego serwera, testu dowolnej paczki modów ani wszystkich możliwych plików obrazów.
HEIC/AVIF, PDF i animacje obrazów pozostają poza obsługiwanym zakresem.
Narzędzia środowiska, Xvfb i tymczasowe światy nie są częścią wydania.
