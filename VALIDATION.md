Aktualne wydanie: [walidacja 0.1.41](docs/VALIDATION_0.1.41.md).

# Weryfikacja 0.1.24

Minecraft 1.21.1, NeoForge 21.1.248, Java 21, protokół 14.
Data weryfikacji: 23 października 2026.

## Wynik i zakres

Końcowe `build --offline`: **BUILD SUCCESSFUL**. Wszystkie **104 testy JUnit**
przeszły; zero błędów, niepowodzeń i pominięć. Sprawdzono także prawdziwy
renderer Minecrafta, osobny serwer po dwóch uruchomieniach oraz dwa klienty
połączone przez TCP i trwałość danych po restarcie serwera.

Mapa pobiera bufor 21×21 przy widocznych 11×11 i wyprzedza kierunek ruchu
o maksymalnie trzy chunki. Klient przechowuje do 8192 kafli, chroni okolicę
postaci i używa jej natychmiast po Home. Osobna pamięć do 1024 rozkodowanych
obrazów ogranicza powtarzanie dekodowania przy przesuwaniu.

Kamera pozostaje swobodna. Wysyłanie łączy zmiany w najnowszy cel: jedno
aktualne żądanie, co najmniej 150 ms między wysyłkami, ponowienie po dwóch
sekundach bez odpowiedzi. Numer odpowiedzi zapobiega zwolnieniu nowszego
żądania przez spóźnioną odpowiedź. Spóźnione dane mogą uzupełniać pamięć,
ale nie przesuwają kamery.

Serwer zachowuje maksymalnie 1024 zadania i 64 bieżące obszary oglądania;
nieaktualne zadania wygasają. Pierwszeństwo mają brakujące widoczne kafle.
Odczyt ma limity 2048 kolumn/tick, czterech nowych odczytów/tick i ośmiu
rzeczywiście trwających odczytów oraz miękki budżet dwóch milisekund.
Porzucenie widoku nie zwalnia zajętego slotu I/O przed zakończeniem pracy;
gotowy wynik nadal trafia do zapisanej mapy. Nadmiar pracy jest pomijany
i może zostać pobrany w następnej aktualizacji.

## Testy automatyczne

- Bramka żądań: 10 000 szybkich prób, pasująca i spóźniona odpowiedź,
  odstęp wysyłania, ponowienie i czyszczenie stanu bez ponownego użycia numeru.
- Zapotrzebowanie serwera: 10 000 zmian widoku, zastępowanie starego celu,
  niezależność graczy i wymiarów, wyprzedzenie, limit 64 obszarów i wygasanie.
- Pamięć kafli: długie przesuwanie, twardy limit 8192, ochrona okolicy postaci
  także po około 40 000 odległych kafli i powrót do zachowanych danych.
- Kodek sieci: pełne 441 komórek z cechami, właścicielami i nazwami Unicode,
  zachowanie numeru żądania oraz rozmiar kodowanej wiadomości poniżej 1 MiB.
- Zachowane testy członkostwa, uprawnień, menu, logo i migracji banera do 2K.

## Prawdziwy klient — `fast24a`: PASS

Przy 1920×1080, GUI 2/4 i trzech przybliżeniach porównano piksele renderera
po puszczeniu myszy, przełączeniu siatki, opóźnionych odpowiedziach,
zmianie skali GUI i kolejnych gestach. Nie wystąpiło przyciąganie kamery
do środka chunka ani odsłonięcie krawędzi poprzedniego prostokąta.
Trafianie w częściowo widoczne chunki pozostało poprawne.

24 odległe odpowiedzi przekroczyły pojemność pamięci; Home odtworzył obraz
chronionej okolicy bez połączenia. 80 szybkich skoków do granic świata,
NaN i nieskończoności nie spowodowały wyjątku. Zapisano 15 zrzutów.
Sprawdzono także redukcję dawnego banera 8192×1901 do tekstury 2048×475,
mipmapy i wygładzanie pomniejszenia.

Raport lokalny: `build/ui-smoke-fast24a/result.txt`.

## Osobny serwer — `fast24b`: FIRST_RUN PASS i RESTART PASS

Sprawdzono rzeczywistą wygenerowaną wioskę w chunku (38, 51), jej pierwotne
cechy i farmę, rekonstrukcję starszych danych, wszystkie kolumny kolorów,
styki kafli oraz aktualizacje powierzchni. Widoki odległe i przy granicach
świata nie generowały chunków ani nie tworzyły pustych plików regionów.

Odczytano rzeczywisty zapis FULL z paletami i heightmapą osiem chunków od
środka kamery, poza widocznym promieniem pięciu chunków. Pobieranie poza
widokiem działało bez ticketów chunków, również po restarcie i zimnym
odczycie pliku regionu.

Raport lokalny: `build/server-smoke-fast24b/result.txt`.

## Dwa klienty i restart — `net24a`: PASS

Właściciel przesuwał mapę przez sześć zmian kierunku podczas trzymania
myszy i odbierania prawdziwych odpowiedzi TCP. Sprawdzono bufor 21×21,
brak uciętych pikseli, stabilny wybór terenu, ułamkowe położenie po
puszczeniu i siatkę. Następnie wykonano 80 gwałtownych skoków do granic
świata, natychmiastowy Home oraz odzyskanie aktualnych danych. Otrzymano
35 odpowiedzi mapy.

Obserwator kolejki sprawdzał limity w każdym ticku serwera. W tej próbie
maksima wyniosły 441 zadań, cztery rzeczywiste sloty odczytu i jeden
aktywny widok mapy; nie przekroczono żadnego limitu. Dwa klienty były
połączone, lecz test nie stanowi pomiaru obciążenia 64 równoczesnych map.

Drugi klient dołączył przez panel, otrzymał delegowane uprawnienie,
identyczne szczegółowe logo, baner 2K i wspólny motyw. Próby niedozwolonej
zmiany motywu, banera i usunięcia członka zostały odrzucone; prywatne
zgłoszenia nie zostały ujawnione. Restart zachował członków, właściciela,
rangę, uprawnienia, wygląd i daty dołączenia.

Raporty lokalne: `build/network-smoke-net24a/owner.PASS`, `member.PASS`,
`restart.PASS` i `budget.txt`.

## Plik wydania

- `crimeandlaw-0.1.24.jar`: **1 850 067 bajtów**.
- SHA-256: `3184e7161a95abcaee016329e9741535fe8e556a77502cb6c57131616a57bd31`.
- Poprawne CRC całego archiwum oraz 15 dołączonych zależności.
- 101 klas produkcyjnych identycznych bajtowo z końcową kompilacją.
- Bez powtórzonych wpisów i bez klas testów uruchamianych w grze.
- Metadane wskazują 0.1.24. Serwer i wszyscy gracze wymagają aktualizacji.

## Granice potwierdzenia

Bufor i pamięć przyspieszają dostęp do znanych danych; nie oznaczają
natychmiastowego odczytu dowolnie dalekiego miejsca na każdym sprzęcie.
Po wyprzedzeniu pobierania mogą pojawić się chwilowo puste kafle, a mapa
uzupełni je po otrzymaniu danych. Nigdy niewygenerowany teren pozostaje
nieznany. Mapa nie generuje go w tle. Budżet czasu serwera jest miękki,
a rzeczywisty czas zależy od dysku, sieci i obciążenia świata.

Poprzedni raport: [0.1.23](docs/VALIDATION_0.1.23.md).
