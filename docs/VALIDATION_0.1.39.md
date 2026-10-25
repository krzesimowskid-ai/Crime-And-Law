# Przegląd 0.1.39 — 25 października 2026

Minecraft 1.21.1, NeoForge 21.1.248, Java 21. **Protokół 20**, schemat danych 4.
Wymagana aktualizacja klienta i serwera.

## Znalezione problemy i wynik zmian

1. **Obejście praw finansowych przez rangi.** Osoba zarządzająca rangami
   mogła dopisać sobie prawo wypłaty. Osoba przypisująca rangi mogła
   wybrać dla siebie istniejącą rangę finansową. Dwa testy regresji
   odtworzyły problem w 0.1.38, a następnie przeszły po poprawce.
   Serwer ogranicza edycję i delegowanie do praw posiadanych przez aktora;
   chroni także zmianę rangi osoby z prawami, których aktor nie posiada.
   Lider zachowuje wszystkie działające prawa, a kolejność wizualna rang
   nadal nie zmienia uprawnień. Edytor respektuje te same ograniczenia.
2. **Oczekiwanie bez końca na odrzucone żądanie.** Po pominięciu pakietu
   przez limit 12 żądań/s formularz przelewu mógł pozostać zablokowany.
   Po pięciu sekundach klient żąda aktualnego stanu oraz unieważnienia
   niewykonanego żądania. Nie ponawia automatycznie transferu.
3. **Stary kontekst finansów i odpowiedzi formularzy.** Token jest teraz
   związany z sesją gracza i konkretnym gangiem. Czarna lista rozpoznaje
   odpowiedź przez identyfikator własnej operacji; obcy komunikat „Zapisano.”
   nie zamyka jej formularza. Jest też obsługa braku potwierdzenia
   i zmiany gangu.
4. **Niejawne konwersje pól pakietów.** Tablice nie mogą udawać napisów,
   liczb ani flag przez rekurencyjne odczyty Gson. Liczby stron są dokładnymi
   liczbami całkowitymi, flagi wymagają typu boolean. Ochrona obejmuje
   główne akcje, filtry finansów, czarnej listy i kolejność rang.
5. **Koszt przetwarzania obrazów.** Prawa są sprawdzane przed dekodowaniem
   logo i baneru. Nowe banery wymagają maks. 2048×512; zapisane wcześniej
   obrazy 8K nadal przechodzą migrację. Zmniejszono budżety pakietów do
   obecnych obrazów i ograniczono zmiany obrazów do jednej na dwie sekundy
   w danej sesji. Nie wprowadzono nowego limitu członków ani mechanik aktywności.

## Sprawdzenia

- **175 testów JUnit**: zero błędów, niepowodzeń i pominięć. Obejmują
  nowe regresje praw i typów danych oraz dotychczasowe testy migracji,
  kont, limitów, historii, uprawnień, mapy, captów, HUD-u i czatu.
  Przeszedł też istniejący test 10 000 losowych transferów zachowujących
  łączną pulę pieniędzy.
- **Prawdziwy serwer i dwa klienty TCP**: wpłaty, wypłaty, premie pojedyncze
  i masowe, członek offline, cofnięte/brakujące prawa, niedozwolone kwoty,
  powtórzony odbiorca, brak środków, jednorazowość tokenu i komendy operatora.
  Śmierć oraz odrodzenie nie zmieniły gotówki ani banku.
- **Rzeczywiste okno transakcji**: lokalnie zasymulowano nieprzetworzony
  pakiet przez pozostawienie formularza w stanie oczekiwania. Po co najmniej
  pięciu rzeczywistych sekundach nastąpiło odświeżenie przez serwer.
  Formularz odblokował się, salda nie zmieniły się, a opóźnione żądanie
  ze starym tokenem zostało odrzucone. Symulacja nie wysyłała pierwotnego
  przelewu; nie jest pomiarem utraty pakietów w sieci.
- **Dodatkowe wywołania serwera przy obu połączonych sesjach**: cudzy
  token, podmienione pole aktora, token starego gangu, powtórzenie wypłaty,
  próby obejścia rang, nieprawidłowy ostatni odbiorca, zagnieżdżenie
  10 000 tablic oraz inne błędne typy. Dwa niezależne żądania wpłaty
  zakolejkowano równocześnie z osobnych wątków. Faktyczne mutacje odbyły
  się kolejno na wątku gry, zgodnie z domyślnym HandlerThread.MAIN
  używanego NeoForge. Te dodatkowe próby wywołują procedurę obsługi
  z kontekstem prawdziwych graczy; nie wszystkie przechodzą ponownie przez TCP.
- **Zachowanie sumy i restart**: po całej próbie banki graczy oraz saldo
  gangu nadal wynosiły łącznie **100 000 $**, a osobna gotówka **1 802 $**.
  Ponowne uruchomienie serwera z tego samego świata zachowało salda,
  uprawnienia, członkostwo, 13 poprawnych wpisów finansowych i sześć
  publicznych zdarzeń. Odrzucone żądania nie dopisały operacji finansowych.
- **Capt i depozyt łupów na dedykowanym serwerze z graczami testowymi**:
  obecność, eliminacja, ochrona, oddzielenie łupów od identycznych przedmiotów
  poza wydarzeniem, zapis/odczyt NBT. Pełny ekwipunek creative odebrał zero
  przedmiotów; wolne miejsce na jedną sztukę pozwoliło odebrać jedną, a potem
  pozostałe 30 z początkowych 31. Łączna liczba przedmiotów została zachowana.
- **Prawdziwy renderer i współrzędne myszy**: 1920×1080 przy GUI 2/4,
  855×481 i 640×480, lider oraz delegowany kadrowy. Sprawdzono zablokowane
  i dozwolone przełączniki, zapis chronionej rangi, tworzenie rangi,
  korelację odpowiedzi czarnej listy oraz zamknięcie starego formularza
  po zmianie gangu. Faktyczny rozmiar framebufferu i skalę GUI zweryfikowano
  po zmianie okna. Obejrzano małe okno i dialog nowej rangi.
- Dotychczasowy sterownik dwóch klientów sprawdził także czat, podpowiedzi,
  historię, klikalny komponent, ustawienia ze szkicem oraz HUD i zegar
  przy GUI 2/4 i zmianie strefy czasu serwera.
- Końcowe `build -x createMinecraftArtifacts -x downloadAssets`: BUILD
  SUCCESSFUL. Użyto niezmienionych oficjalnych artefaktów Minecrafta i NeoForge.
  CRC JAR-u oraz 15 dołączonych bibliotek poprawne; bez powtórzonych wpisów
  i bez klas sterowników testowych. Metadane 0.1.39, pięć miksinów klienta.

Sprawdzony restart nastąpił po poprawnym zapisie serwera. Nie symulowano
awaryjnego przerwania procesu w połowie zapisu ani pełnego obciążenia
512 gangami z maksymalnymi obrazami. Wyniki potwierdzają opisane scenariusze,
nie stanowią gwarancji braku każdego możliwego błędu.

Wyniki i zrzuty: roboczy katalog `validation-039/final-finance`,
`final-capture` oraz `final-ui`, poza repozytorium. Sterowniki są w
`src/financeTest` i `src/uiTest`, włączane opcjonalnie; nie wchodzą do JAR-u.
Tryb rozszerzony finansów wymaga `-PfinanceSmoke -PfinanceAudit=true`
oraz dwóch klientów testowych; osobny restart używa także `-PfinanceRestart=true`.

`crimeandlaw-0.1.39.jar`: 2 130 208 bajtów.

SHA-256: `ff03f8b675dc62860bc012922903b653c615a01466e4319a875f08c7745e2a8d`.
