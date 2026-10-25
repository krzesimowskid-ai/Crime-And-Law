# Pełny przegląd 0.1.40 — 25 października 2026

Minecraft 1.21.1, NeoForge 21.1.248, Java 21. Protokół 20, schemat zapisu 4.
Punktem wyjścia jest commit `fc4358bbe7a0178a4d6ad7caf9b455ac5dcba26b` (0.1.39).

## Dokładny zakres

Odczytano i przeanalizowano **wszystkie 104 pliki Java kodu produkcyjnego**,
także ich wywołania między systemami. Nie jest to ponowne nazwanie wcześniejszej
kontroli finansów pełnym audytem. Przegląd objął również dziewięć zasobów
wchodzących do moda i siedem plików budowania/wrappera.

| Obszar kodu | Pliki | Zakres kontroli |
|---|---:|---|
| Rejestracja moda | 1 | Uruchamianie, rejestracja, odseparowanie klas klienta od serwera |
| Klient | 56 | Każdy ekran i widget, HUD, minimapa, czat, obrazy, tekstury, lokalne ustawienia, zmiana stanu |
| Gangi | 12 | Członkostwo, rangi, prawa, finanse, cele, czarna lista, widoki, zapis i migracje |
| Sieć | 4 | Pakiety, sesje, tokeny, fragmentacja, limity, typy danych i autorytet serwera |
| Tereny | 25 | Generowanie i rekonstrukcja opisów, kartografia, kolejki, odczyty dysku, capty, łupy i czas świata |
| Miksiny | 6 | Natywny czat, podpowiedzi i dostęp do czcionek; oddzielenie przedmiotów captu |

[Lista każdego sprawdzonego pliku z SHA-256](review-0.1.40.csv) zawiera
**122 pozycje**: 104 pliki kodu, dziewięć zasobów, siedem plików budowania
oraz dwa pliki nieaktywnego modelu balansu. CSV i
[raport maszynowy](validation-0.1.40.json) wskazują dokładne wersje plików.
Kontrola zasobów obejmuje JSON, TOML, konfigurację miksinów, tablice obu TTF,
znaki faktycznie używane w HUD-zie i dołączoną licencję czcionek.
Sprawdzono skrypty wrappera, jego CRC i skonfigurowany checksum Gradle.

## Znalezione i naprawione problemy

1. **Nieudany zapis osobistego motywu zmieniał aktywne ustawienie.**
   `PersonalTheme.set` modyfikował mapę w pamięci przed zapisem na dysk.
   Przy błędzie zwracał informację o porażce, ale nowy wygląd już działał
   i znikał po restarcie. Obecnie najpierw zapisuje kopię ustawień i dopiero
   po udanej podmianie pliku przyjmuje ją w pamięci. Podmiana jest atomowa,
   gdy system plików ją obsługuje; pozostaje obsługa zwykłej podmiany
   i sprzątanie pliku tymczasowego.
2. **Szkice oraz potwierdzenia mogły pozostać w kontekście poprzedniego gangu.**
   Po otrzymaniu nowego stanu klient powiadamiał tylko formularze finansów
   i czarnej listy. Na przykład edytor nowej rangi lub koloru pozostawał
   otwarty i mógł wysłać stare dane do aktualnego gangu. Wszystkie dialogi
   zapamiętują teraz gang, dla którego zostały otwarte, i zamykają stary szkic
   po zmianie członkostwa. Zwykłe odświeżenie tego samego gangu nie usuwa
   szkicu. Potwierdzenia również sprawdzają gang przed wykonaniem akcji,
   a zmiana gangu unieważnia oczekujący import obrazu. Kontrola uprawnień
   nadal odbywa się na serwerze.

Cztery scenariusze nowego sterownika `GangUiSmokeTestLifecycle` **najpierw
zakończyły się niepowodzeniem na kodzie 0.1.39**, a następnie przeszły po
poprawkach: błąd realnego zapisu, edytor rangi, edytor koloru i potwierdzenie.
Błąd zapisu wymuszono niepustym katalogiem w miejscu docelowego pliku
w osobnym katalogu testowego klienta; test odtwarza wcześniejsze ustawienia.

## Końcowe testy

- **175 testów JUnit, 32 zestawy**: zero błędów, niepowodzeń i pominięć.
  Zachowano regresje praw finansowych, typów pakietów, migracji, obrazów,
  map, captów, czatu i układu HUD-u. Przeszedł również test 10 000 losowych
  operacji finansowych z kontrolą zachowania sumy.
- **Pełny sterownik rzeczywistego interfejsu**: tworzenie i wyszukiwanie
  gangów, paginacja, członkowie, karty skinów, zarządzanie, rangi, przeciąganie
  z przewijaniem, rejestr, finanse, czarna lista i cele. Logo i baner 2K,
  koło kolorów, krycie, kadrowanie, anulowanie pracy asynchronicznej,
  mipmapy i minifikacja. Mapa rzeczywistej wioski, zaznaczanie,
  powrót do gracza, trzy przybliżenia i 18 zmian kierunku podczas trzymania
  mapy oraz opóźnione odpowiedzi. Przejście obejmuje GUI 2/4 i 89 zrzutów.
- **Uprawnienia w rendererze**: 1920×1080 przy GUI 2/4, 855×481 i 640×480;
  lider i delegowany kadrowy. Rzeczywiste współrzędne myszy, przełączniki
  praw, zablokowany zapis chronionej rangi, tworzenie rangi, korelacja
  odpowiedzi i zmiana gangu. Obejrzano również wybrane zrzuty mapy,
  edytora kolorów, karty członka oraz małego okna z dialogiem.
- **Finanse, osobny serwer i dwaj klienci TCP**: wpłaty, wypłaty, premie,
  odbiorca offline, prawa, limity, brak środków i komendy administratora.
  Śmierć i odrodzenie zachowały gotówkę i bank. Prawdziwy formularz
  odczekał pięć sekund i odzyskał stan po zasymulowanym niewykonanym
  żądaniu, bez ponawiania płatności. Dodatkowe wywołania procedury obsługi
  na wątku serwera sprawdziły cudzy, stary i powtórzony token, podmienione
  pole aktora, obejścia rang, błędne typy i 10 000 zagnieżdżonych tablic,
  atomowość premii oraz dwie równocześnie zakolejkowane wpłaty.
  Nie wszystkie te dodatkowe próby przechodzą przez TCP.
- **Suma pieniędzy i restart**: po próbach nadal **100 000 $** w bankach
  i saldzie gangu oraz osobno **1 802 $** gotówki. Restart tego samego świata
  zachował salda, członkostwo, prawa, 13 transakcji i sześć zdarzeń rejestru.
- **Capt i łupy**: dedykowany serwer z graczami testowymi sprawdził prawa,
  obecność, neutralne przejęcie, PvP, eliminację i ochronę. Spośród 31 sztuk
  depozytu pełny ekwipunek creative odebrał zero, miejsce na jedną sztukę
  pozwoliło odebrać jedną, potem pozostałe 30. Identyczne przedmioty leżące
  poza wydarzeniem nie zostały włączone do łupów.
  Następnie dwaj prawdziwi klienci przeszli aktywację, oznaczenie prywatne,
  start, potwierdzenie kontrataku, HUD, śmierć rywala, odrodzenie, zwycięstwo
  i odebranie 11 diamentów. Restart zachował właściciela, ochronę, siedem
  sztuk osobnego depozytu, zakres oznaczeń i strefę czasu. Niezakończony
  capt został anulowany z cooldownem, bez nagrody za czas offline.
- **Kartografia i jej restart**: wygenerowana wioska/farma przy chunku
  `(12, 54)` dla ziarna 12345. Pierwotne cechy, rekonstrukcja starego chunka,
  kolory wszystkich kolumn, styki kafli i aktualizacja powierzchni.
  Odczyt istniejącego FULL NBT poza załadowanym obszarem i ponowny zimny
  odczyt regionu po restarcie. Odległe widoki i granice świata nie ładowały
  nowych chunków ani nie tworzyły pustych plików regionów.
- **Czat, HUD i zegar** w próbie dwóch klientów: pisanie, wysyłanie i echo,
  historia, szkic, natywne podpowiedzi, kółko i scrollbar, klikalny komponent,
  usuwanie wiadomości, ustawienia, zmiana GUI i strefy czasu serwera.
  Otwarty czat zachował mapę, pieniądze i zegar.
- Opcjonalny, niewczytywany przez mod model balansu przeszedł sześć
  scenariuszy obliczeniowych. Nie zaliczono ich jako testów ekonomii gry.

Sterowniki integracyjne są opcjonalnymi źródłami `*Test`/`*Smoke` i nie
wchodzą do wydania. Nowe regresje uruchamia się z `-PuiSmoke` i właściwościami
`crimeandlaw.uiSmoke=false`, `crimeandlaw.reviewUiSmoke=true`,
`crimeandlaw.reviewUiOutput=<katalog wyników>`. Pozostałe sterowniki opisuje
także [raport 0.1.39](VALIDATION_0.1.39.md). Przy łączeniu konfiguracji
uruchamiania należy wyłączać pozostałe sterowniki i nie dublować argumentów
rozmiaru okna; poprawiono to w roboczym launcherze przed końcowymi próbami.

## Granice wniosku

Pełny przegląd oznacza odczyt całego kodu projektu wymienionego w spisie,
**nie 100% pokrycia wszystkich możliwych ścieżek wykonania**. Nie znaleziono
kolejnego sposobu tworzenia pieniędzy ani przedmiotów w opisanych próbach.
Nie audytowano źródeł Minecrafta, NeoForge i wszystkich bibliotek jako
osobnych projektów. Ich dołączone archiwa sprawdzono przy pakowaniu.

Restarty wykonano po prawidłowym zapisie. Nie symulowano zabicia procesu
w połowie zapisu plików, pełnego obciążenia 512 gangami z maksymalnymi
obrazami ani zgodności ze wszystkimi innymi modami. To pozostaje ograniczeniem
walidacji, a nie potwierdzeniem odporności na takie sytuacje.

Wyniki robocze: `validation-040/regression-before`, `final-review`,
`final-ui`, `final-full`, `final-finance`, `final-capture2`, `final-terrain`.
Paczka i jej suma kontrolna są zapisane w raporcie JSON obok tego dokumentu.


`crimeandlaw-0.1.40.jar`: **2 130 811 bajtów**.

SHA-256: `12777f54f4030a790212407ead6799a69312c22ca603371076f19e7b324ceb5a`.

Końcowe `build -x createMinecraftArtifacts -x downloadAssets`: BUILD SUCCESSFUL.
CRC paczki oraz wszystkich 15 dołączonych bibliotek poprawne; bez duplikatów
wpisów i klas sterowników testowych. 177 klas moda w formacie Java 21,
metadane 0.1.40, pięć miksinów klienta i jeden wspólny.
