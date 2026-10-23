# Weryfikacja 0.1.22

Minecraft 1.21.1, NeoForge 21.1.248, Java 21. Protokół 13.

## Zakres

Swobodne położenie kamery mapy między chunkami oraz szczegółowy baner
8192×1901 (limit 8192×2048 / 68 MB). Logo bez zmian: 2048×2048 / 17 MB.
Zachowany kadr, przybliżenie i oś nagłówka z 0.1.21.

## Potwierdzone wyniki

- `build --offline`: BUILD SUCCESSFUL.
- 98 testów JUnit: 0 niepowodzeń, 0 błędów, 0 pominięć.
- Import i zapis 16 777 216 pikseli źródła 8192×2048, porównanie wszystkich
  wierszy obrazu. Zapis, ponowny odczyt i kodeki stanu z dużym logo i banerem.
- Osobna walidacja banera oraz zachowana obsługa dawnych 4096×1024.
- Powtarzane gesty od ułamkowych współrzędnych przy każdym przybliżeniu.

## Prawdziwy renderer — free22c

Próba po odtworzeniu środowiska: 1920×1080, GUI 2 i 4, promienie 5, 3 i 2
(11×11, 7×7 i 5×5 chunków). 13 zapisanych klatek. Wynik: PASS, klient zakończony
kodem 0; raport `build/ui-smoke-free22c/result.txt`.

- Dokładnie identyczne piksele mapy przy puszczeniu myszy, ponownym
  naciśnięciu, przełączeniu siatki i spóźnionej odpowiedzi sąsiadującego bufora.
- Ułamkowe współrzędne zachowane także przy zmianie przybliżenia i skali GUI.
- Kolejny gest zaczyna się z zapisanego położenia. Kliknięcie w częściowo
  widoczny chunk przy ujemnych współrzędnych wybiera obszar pod kursorem.
- Tekstura banera ma faktycznie 8192×1901 pikseli. Sprawdzono wszystkie
  poziomy mipmap i filtry GPU; jednopikselowa szachownica po pomniejszeniu
  daje stabilny szary obraz, zamiast grubej, migoczącej siatki.

## Dwa prawdziwe klienty i restart — net22b

Dedykowany serwer oraz dwa niezależne klienty 960×540, połączenie TCP
127.0.0.1:25597. Osobny, nowy świat testowy. Wszystkie trzy procesy
i ponownie uruchomiony serwer zakończyły pracę kodem 0.

- `owner.PASS`: utworzenie, zgłoszenie i przyjęcie przez panel, własna ranga
  oraz przekazanie uprawnienia. Import logo i banera przez przeciągnięcie
  pliku, edytor kadru i zapis przez faktyczne wiadomości sieciowe.
- Logo: 19 196 396 znaków Base64; baner 8K: 37 287 232 znaków Base64.
  Baner przekracza dawny limit logo, oba obrazy są obecne w tym samym stanie.
- `member.PASS`: identyczne skróty obrazów u drugiego gracza, pełna tekstura
  banera 8192×1901, motyw i przypisana ranga. Odrzucone nieuprawnione
  zmiany wyglądu, banera i członkostwa; zgłoszenia pozostają prywatne.
- Mapa prawdziwego świata: sześć zmian kierunku przy trzymanej myszy,
  25 odpowiedzi sieciowych, zachowany fokus i wybór terenu. Sprawdzenie
  brzegów wyrenderowanej mapy, brak pustego pasa. Ostatni ruch pozostawia
  kamerę o −0,37 i +0,23 chunka od środka bufora; po puszczeniu, siatce
  i kolejnej odpowiedzi ta dokładna pozycja pozostaje zachowana.
- `restart.PASS`: ponowne uruchomienie tego samego świata zachowało dwóch
  członków, właściciela, rangę/uprawnienia, motyw, obrazy i daty dołączenia.

## Końcowe wydanie

Końcowe `build --offline`: BUILD SUCCESSFUL (97 klas produkcyjnych).
98 testów JUnit: 0 niepowodzeń, błędów i pominięć.

`crimeandlaw-0.1.22.jar`: 1 840 776 bajtów. Bez błędów CRC, powtórzonych
wpisów i klas automatyzacji. Wszystkie 97 klas zgodne bajt w bajt
z końcową kompilacją; 15 bibliotek JarJar, metadane wersji 0.1.22.

SHA-256: `74a34302ffc042ebb184d613b3b8def472d2c241095a54e4cceaae251c6d87b7`.

Próby obejmują kontrolowane dane oraz nowy świat na lokalnym serwerze.
Nie sprawdzano każdego zewnętrznego dodatku ani wszystkich kart graficznych.
Większy zapis zachowuje szczegóły oryginału; nie odzyskuje ich ze starego
obrazu o niższej rozdzielczości. Dla większej jakości trzeba zaimportować oryginał.
Protokół 13 wymaga tego samego wydania u graczy i na serwerze.

Poprzednie sprawdzenia: [0.1.21](VALIDATION_0.1.21.md),
[0.1.20](VALIDATION_0.1.20.md).
