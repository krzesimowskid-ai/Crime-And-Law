# Walidacja 0.1.30 — 24 października 2026

Minecraft 1.21.1, NeoForge 21.1.248, Java 21. Protokół 18, schemat danych 4.

## Przegląd ekonomii

Test losowy wykonuje 10 000 operacji na 20 graczach w czterech gangach,
z poprawnymi i błędnymi kwotami oraz odbiorcami. Po każdej operacji sprawdza
sumę gotówki, banków i kas gangów; co 1000 operacji zapisuje i odczytuje dane.
Suma pozostaje równa 220 000 $. Nie wykazano tworzenia pieniędzy.

Sprawdzono niezależność gotówki i banku, migrację 0.1.29 bez powielania środków,
limity kont, zero/kwoty ujemne, ułamki i przekroczenie zakresu, nieautoryzowane
operacje, błędnego lub powtórzonego odbiorcę, całość wypłaty masowej oraz
powtórzenie rozliczonego pakietu. Odczyt pliku kont odrzuca ułamkowe kwoty
zamiast obcinać je podczas konwersji Gson → long.

Lista wypłat jest kopiowana przed walidacją i realizacją. Tożsamość aktora
pochodzi z połączenia, a odbiorcy z aktualnego członkostwa. Gotówka/bank
są zapisane w pojedynczych danych świata, poza klonowaną encją i inventory.

Testy są dowodem sprawdzonego zakresu, nie matematycznym dowodem braku
każdego możliwego błędu ani zgodności ze wszystkimi innymi modami.

## Prawdziwy serwer i dwa klienty

- Zwykłe wpłaty, wypłaty, premie pojedyncze i masowe po TCP.
- Odbiorca offline zachowuje przyznaną premię.
- Członek bez prawa premii nie może wykonać jej ręcznie wysłanym pakietem.
- Operator przyznaje gotówkę nową komendą; ten sam rozkaz zwykłego gracza
  jest odrzucany przez dispatcher poleceń.
- Serwer naprawdę zabija jednego klienta, a klient odradza się. Bank 500 $
  i gotówka 567 $ pozostają bez zmian oraz są ponownie synchronizowane z HUD-em.
- Po końcu testu banki graczy + saldo gangu nadal mają 100 000 $, a osobna
  gotówka 1802 $. Siedem udanych transakcji finansowych; próby błędne niczego
  nie dopisują.
- Z panelu dodano członka offline do czarnej listy, usuwając go ze składu,
  następnie zdjęto wpis i odczytano dwie pozycje w historii. Pieniądze tej
  osoby nie zniknęły ani nie zostały skopiowane.
- Otworzono rzeczywisty panel trzech tygodniowych celów bez nagród.
- Serwer zapisano, zakończono i uruchomiono ponownie. Wczytano salda, historię
  finansową, członkostwo, uprawnienia i historię czarnej listy.

## HUD i mapa

Obejrzano renderer przy 1920×1080 i ustawieniach GUI 2/4. Sprawdzono oba
symbole, normalne i maksymalne kwoty, radar, układ z waniliowymi paskami
oraz przesunięcie nieaktywnego czatu. W świecie testowym zbudowano kolorowe
punkty orientacyjne. Minimapka pokazała je zgodnie z rzeczywistymi blokami.
Pamięć minimapy została sprawdzona jako niepusta i ograniczona do 256 kafli.
Oddzielny test geometrii obejmuje osiem wielkości natywnego GUI.

Pierwsza inspekcja wykazała kolizję pieniędzy z dużą skalą waniliowego HUD-u
i nakładanie nieaktywnych wiadomości czatu. Poprawiono geometrię i ponowiono
pełną próbę dwóch klientów. Końcowe zrzuty nie mają tych kolizji.

## Czarna lista i cele — testy modelu

Zmiana nicku nie omija UUID. Sprawdzono blokadę zgłoszeń i przyjmowania,
kasowanie oczekującego zgłoszenia, osobne prawa listy/usuwania członków,
zakaz blokowania lidera/siebie oraz zachowanie salda usuwanej osoby.

Cele deduplikują osoby i wydarzenia, zachowują postęp w zapisie i resetują
go w nowym tygodniu. Cofnięcie czasu nie pozwala wielokrotnie zerować postępu.
Operacje celów nie zmieniają danych finansowych.

Sterowniki rzeczywistych klientów/serwera: `src/financeTest`, opcja
`-PfinanceSmoke`. Są wyłączone z normalnego JAR-u.

## Wydanie

Końcowe `clean build --offline`: sukces. **151 testów**, zero błędów
i pominięć. CRC JAR-u i 15 zagnieżdżonych bibliotek poprawne. Brak
klas automatyzacji/podglądów.

Plik: `crimeandlaw-0.1.30.jar`, 1999078 bajtów.

SHA-256: `f4dc0e0d8d2084179ba973308aadd01b8f5be1f251f0aafb29a734fbdb0a1c27`.
