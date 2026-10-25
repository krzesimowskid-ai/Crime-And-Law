# Weryfikacja 0.1.43 — jednorazowe uprawy w doniczkach

25 października 2026. Minecraft 1.21.1, NeoForge 21.1.248, Java 21.
Protokół 23; wymagane jednakowe wydanie u graczy i na serwerze.

## Testy jednostkowe

**198 testów w 35 zestawach**, bez błędów, niepowodzeń ani pominięć.
Obejmują dotychczasowe menu, finanse, uprawnienia, mapę i reguły zbioru oraz:

- Jedno podłoże i jedno nasiono na cykl, brak wzrostu bez ziemi/wody/światła.
- Skończoną wodę, napełnianie bez przekroczenia pojemności, aktywny czas.
- Wszystkie graniczne plony 3–6 / 4–8 i jednokrotne wywołanie losowania.
- Próg UV 80%, granicę 14 399 / 14 400 ticków, brak pełnej premii za lampę
  dołożoną na koniec i właściwy przedział wzrostu kupionej sadzonki.
- Zachowanie tożsamości i częściowego zbioru po odtworzeniu danych,
  zakończenie zapisanego wyczerpanego cyklu i sanitację uszkodzonych danych.
- Wszystkie **40 320 kolejności ośmiu liści** oraz dotychczasowe 720 sześciu.
- Osobne konto gracza i typ salda, odrzucanie niedozwolonych kwot,
  brak debetu poniżej zera i zachowanie banku przy zakupach gotówkowych.

## Rzeczywista rozgrywka sieciowa

Uruchomiono serwer dedykowany i dwa osobne klienty TCP. Właściciel
wykonał rzeczywiste akcje okien i gry, zamiast bezpośrednio zmieniać stan
produkcji:

1. PPM na Ogrodnika, siedem zakupów przyciskami w przewijanym katalogu:
   doniczka, duży worek, woreczek nasion, konewka, stelaż, UV, nożyce.
2. Ponowiony pakiet zakupu dał jeden przedmiot i jeden wydatek.
   Saldo **2000 → 746**, dokładny koszt 1254.
3. Ustawienie ramy przez standardowe użycie BlockItem, doniczka w dolnym
   i UV w górnym bloku. Konewka napełniona PPM ze źródła wody do 400.
4. Rzeczywisty skrót Z, ruch kursora, przytrzymanie/puszczenie w minigrze
   3D: doniczka pełna, duży worek **3 → 2**. Drugi klient nie przejął
   tej samej rezerwacji.
5. Otwarcie woreczka i sadzenie przyciskami: **5 → 4** luźne nasiona.
   Gest podlewania: konewka **400 → 300**, doniczka pełna.
6. Czas dojrzewania skrócono tylko w scenariuszu testowym przez przesunięcie
   serwerowego stanu do końca wzrostu. Pełne 18 000 kroków, zużycie wody
   i ekspozycję sprawdzają testy jednostkowe; test sieciowy nie trwał 15 minut.
7. Przejście do zbioru z nożycami. Osiem widocznych liści w kolejności
   **7, 2, 5, 0, 6, 1, 4, 3**, przez fizyczne zdarzenia okna i TCP.
   Każde cięcie: jeden liść, jeden punkt zużycia; ponowienie nic nie dodało.
8. Po zbiorze: osiem nagród, osiem punktów zużycia, brak rośliny i ziemi;
   doniczka, UV i rama pozostały.

Dodatkowo prawdziwe klienty odzyskały po trzech sekundach stan symulowanego
nieprzetworzonego zgłoszenia sklepu i przygotowania. Odświeżenie zmieniło
wycenę/rewizję, bez ponownego zakupu ani zużywania materiałów.

## Próby odrzucanych operacji

Na wątku serwera, w tym samym świecie testowym, sprawdzono:

- Zbyt wczesne zatwierdzenie, podmianę źródłowego worka po rozpoczęciu,
  stary pakiet kończący i wymianę całego bloku/BlockEntity podczas operacji.
- Pozostałe użycia **2 → 1 → zużyty** tego samego dużego worka na następne
  doniczki; powtórzenie potwierdzenia nie pobrało drugiej porcji.
- Ochronę miejsca przez veto zdarzenia, ścianę i odległy niezaładowany cel.
- Rozpakowanie dwóch opakowań w pełnym ekwipunku: brak zmian. Jedno ostatnie
  opakowanie zwolniło swój slot na dokładnie pięć nasion.
- Nagrodę przy pełnym ekwipunku kreatywnym: dokładnie jeden przedmiot na ziemi.
- Zakup przy pustej gotówce i zasilonym banku: bank bez zmian. Pełny ekwipunek
  nic nie kosztował. Stara wycena nie powtórzyła zakupu. Oddalenie od NPC
  unieważniło możliwość kupowania.
- Niszczenie dolnego i górnego bloku ramy: dokładnie jeden stelaż,
  jedna doniczka i jedna lampa, bez nasion, ziemi ani liści.
- Górny blok w kreatywnym nie powielił dolnego dropu. Eksplozja obejmująca
  obie części oddała wyposażenie dokładnie raz.
- Nieobecną recepturę nasion, brak sadzenia ItemNameBlockItem na ziemi,
  puste łupy dawnej rośliny.

## Zapis na dysku i restart

Zatrzymano serwer po rzeczywistym zapisie i uruchomiono go ponownie na tym
samym katalogu świata. Zachowały się:

- Pusta po zbiorze ziemia i wielorazowe wyposażenie pierwszego stelaża.
- Druga, zwykła roślina: raz wylosowany plon 6, maska 47 (pięć pozostałych
  liści po wycięciu ID 4) oraz dokładnie ten sam UUID.
- Dane gracza: 8 liści, 4 nasiona, 300 wody, 0 dużych worków, 8 zużycia nożyc.
- Gotówka właściciela 746.

## Grafika i okna

Obejrzano prawdziwy render modeli wyposażenia, nasypywanie, podlewanie,
dojrzałą roślinę, ośmiolistny zbiór i sklep. Okno przygotowania sprawdzono
w czterech przypadkach: **1920×1080 / GUI 2**, **1920×1080 / GUI 4**,
**855×481 / GUI 2**, **640×480 / GUI 2**. Zapisano 24 zrzuty z TCP oraz
końcową galerię dziesięciu przedmiotów.

Kadr dojrzałej rośliny mieści koronę poniżej nagłówka. Katalog przewija się,
a skala okna jest uwzględniana przy klikaniu. Materiały mają własne palety
16px, liście i narzędzia zachowują 32px; powierzchnie nie pobierają tekstur
bloków. Atlas bloków pozostał **1024×512×4**. Końcowa kontrola renderera
potwierdza dwublokowe granice widoczności ramy bez wyłączania odrzucania
geometrii poza kamerą dla całej farmy. Nie jest to pomiar FPS dużej farmy.

## Archiwum wydania

- `crimeandlaw-0.1.43.jar`: **3 912 625 bajtów**.
- SHA-256: `fd123e26e5d45f1ee3c9ca719443112ddf24e5d6e0d70a79208ab5e41e29f055`.
- CRC poprawne, brak powtórzonych wpisów i klas scenariuszy testowych.
- **235 klas produkcyjnych** i **34 zasoby** identyczne bajtowo
  z końcowymi plikami kompilacji/źródeł.
- W JAR nie ma starej receptury ani jej odblokowania; łupy nasion są puste.
- Dane maszynowe: [validation-0.1.43.json](validation-0.1.43.json).

## Odtworzenie

`./gradlew -PgardenSmoke compileJava prepareClientRun prepareServerRun`
kompiluje opcjonalne scenariusze. `tools/garden_smoke.py` uruchamia je
na Linuksie z lokalnym Java 21 i Xvfb; ścieżkę narzędzi przyjmuje przez
`CRIMEANDLAW_BUILD_TOOLS`. `--visual` uruchamia galerię, a `--output` wybiera
osobny katalog wyników. Nie używać świata gry jako świata scenariusza.
Scenariusze pozostają poza normalnym wydaniem JAR.

## Zakres potwierdzenia

To kontrola nowego cyklu i jego styku z ekwipunkiem, pieniędzmi, światem,
klientem i zapisem oraz istniejącego zestawu testów. Nie jest ponownym
odczytem każdego pliku menu ani gwarancją braku wszystkich możliwych
błędów. Sprawdzono zwykły restart po zapisie; nie wykonywano awaryjnego
wyłączenia procesu między osobnymi zapisami plików Minecrafta.
Pełny wcześniejszy przegląd menu opisuje [0.1.40](VALIDATION_0.1.40.md).
