# Weryfikacja aurelii — 0.1.41

25 października 2026. Minecraft 1.21.1, NeoForge 21.1.248, Java 21,
**protokół 21**, schemat gangów nadal 4. Baza:
`f0f38e69000c826068fc3ecccef3d05cff68ccad` (0.1.40).

## Wykonany zakres

Pierwsza fikcyjna roślina: nasiona i receptura, pięć etapów wzrostu na
ziemi uprawnej, obsługa mączki kostnej przez bazową mechanikę upraw,
zakrzywiony model 3D i szczegółowa tekstura z żyłkowaniem. Specjalne
nożyce mają recepturę, wytrzymałość i własny model. Podpowiedź po najechaniu
pokazuje wzrost albo skrót Z. Skrót można zmienić w ustawieniach.

Minigra wyświetla rzeczywistą geometrię, obrót, zoom i sześć osobnych
punktów cięcia. Poprawnie przytrzymany i puszczony przycisk odcina jeden
liść; jest animacja odłączenia i licznik postępu. Zakończona roślina
odrasta. Przerwanie pozostawia częściowy stan. Liść w ekwipunku również
ma renderer 3D. Tekstura zachowuje oryginalny PNG, pełne mipmapy i
filtrowanie trójliniowe, bez obniżania mipmap wspólnego atlasu Minecrafta.
Instrukcja: [AURELIA.md](AURELIA.md), pochodzenie grafiki:
[AURELIA_ASSETS.md](AURELIA_ASSETS.md).

Serwer posiada stan liści, rezerwacje, sesje, nagrody i zużycie narzędzia.
Pakiet gracza nie zawiera liczby nagród, identyfikatora przedmiotu ani
wyniku minigry. Sprawdzane są widoczność, odległość, dojrzałość, narzędzie,
konkretna roślina i liść, ochrona spawnu i anulowanie interakcji NeoForge.
Jedna roślina może mieć jednego zbierającego. Są limity żądań, limit sesji,
wygaśnięcie, sprzątanie po wyjściu i zatrzymaniu serwera.

## Wyniki

- **181 testów JUnit, 33 zestawy**: zero niepowodzeń, błędów i pominięć.
  Sześć nowych testów obejmuje jednokrotność usuwania liścia, błędne indeksy,
  granice czasu, wybór tylko istniejących liści, częściowe maski i 10 000
  mieszanych żądań. Zachowano wcześniejsze testy finansów, obrazów i menu.
- **Prawdziwy renderer**: 1920×1080 przy GUI 2/4, 855×481 i 640×480.
  Sprawdzono sześć odrębnych projekcji punktów cięcia, obracanie, zoom,
  przyciski i skalowanie. Sprawdzono faktyczny parametr OpenGL minifikacji
  po narysowaniu encji: `GL_LINEAR_MIPMAP_LINEAR`. Obejrzano zrzuty modelu,
  podpowiedzi w świecie i trwającego zbioru. Osiem zrzutów w teście widoków.
- **Dedykowany serwer i dwaj rzeczywiści klienci TCP**: właściciel nacisnął
  Z przez mapowanie klawisza, otworzył minigrę i wykonał sześć rzeczywistych
  kliknięć/przytrzymań/puszczeń w projekcjach punktów. Każde dało dokładnie
  jeden liść i jedno zużycie narzędzia. Po sześciu roślina wróciła do etapu 1.
  Drugi klient dostał odmowę rezerwacji tej samej rośliny, bez otwarcia gry
  i bez zmiany jej liści. Zapisano 16 zrzutów przebiegu.
- **Brak potwierdzenia**: na prawdziwym ekranie zasymulowano niewykonane
  żądanie, pozostawiając go w stanie oczekiwania, bez wysłania pierwotnego
  cięcia. Po trzech rzeczywistych sekundach klient odzyskał stan przez
  anulowanie próby. Nie ponawiał cięcia; nagrody i zużycie pozostały zerowe.
  To test obsługi oczekiwania, nie pomiar utraty pakietów.
- **Dodatkowe wywołania na wątku serwera z połączonymi graczami**:
  zwykłe nożyce, przeszkoda w linii widzenia, odległy/niedostępny cel,
  anulowanie interakcji przez zdarzenie ochrony, cudzy i nieprawidłowy token,
  niezgodny liść, puszczenie za wcześnie i za późno, powtórzenie poprawnego
  pakietu oraz pakiet zamkniętej sesji. Odrzucone próby niczego nie przyznały.
  Te dodatkowe próby nie wszystkie ponownie przechodzą przez TCP.
- **Pełny ekwipunek creative**: dodatkowo sprawdzono funkcję wydania nagrody
  na prawdziwym graczu. Brak miejsca nie zniszczył liścia: pojawił się dokładnie
  jeden przedmiot na ziemi, bez niemożliwego wstawienia do ekwipunku.
- **Trwałość**: po sześciu sieciowych cięciach dodatkowe poprawne cięcie
  osobnej rośliny zwiększyło wynik do dokładnie siedmiu liści i siedmiu
  punktów zużycia. Pozostawiło pięć liści na drugiej roślinie. Sprawdzono
  identyfikator i maskę w NBT, zapis świata, zatrzymanie i rzeczywisty restart.
  Ponownie odczytano pięć pozostałych liści, etap odrostu pierwszej rośliny
  oraz dokładnie siedem liści i siedem zużycia w pliku gracza.
- **Paczka**: końcowe `build -x createMinecraftArtifacts -x downloadAssets`
  zakończyło się BUILD SUCCESSFUL. CRC JAR-u i 15 wewnętrznych bibliotek
  poprawne, bez powtórzonych wpisów oraz sterowników `*Smoke`. Jest jedna
  kopia tekstury i obie receptury. Metadane 0.1.41.

Testy integracyjne są opcjonalne: `-PharvestSmoke`. Właściwości:
`crimeandlaw.harvestSmoke=true` dla serwera, `crimeandlaw.harvestRole=owner`,
`rival` albo `visual` dla klientów i `crimeandlaw.harvestOutput=<katalog>`.
Osobny restart używa `crimeandlaw.harvestRestart=true`. Sterowniki źródłowe
są w `src/harvestTest` i nie są pakowane do wydania. Wyniki robocze:
`validation-041/final-visual` oraz `validation-041/final-tcp`.

## Ograniczenia kontroli

Naturalne naliczanie losowych etapów korzysta z bazowego `CropBlock`.
W świecie integracyjnym zamrożono losowe ticki, aby dokładnie sprawdzić
odrost i zapis. Nie jest to pomiar czasu wzrostu w wielogodzinnej rozgrywce.
Nie wykonano prób ze wszystkimi modami ochrony działek ani przy setkach
jednoczesnych zbierających. Sprawdzono standardową ochronę spawnu w kodzie
i anulowanie interakcji przez zdarzenie NeoForge w teście.

Restart nastąpił po poprawnym zapisie. Nie symulowano zabicia procesu między
zapisem chunka i ekwipunku. Wyniki potwierdzają opisane scenariusze, nie dają
gwarancji braku każdego możliwego błędu. Użyto tymczasowego adaptera narzędzia
NeoForm dla ograniczonego odczytu `ProcessHandle` w środowisku budowania;
adapter nie wchodzi do JAR-u i nie jest wymagany u graczy.

`crimeandlaw-0.1.41.jar`: **4 124 374 bajty**.

SHA-256: `b3dd9ca00b87d4465700d31e5dd40c9d2887314ca33fde4bb747adbd31e752d2`.

Dokładne wyniki i hashe nowego kodu: [validation-0.1.41.json](validation-0.1.41.json).
