# HUD, czarna lista i cele — 0.1.31

## Minimapka i liczniki

Minimapka działa bez członkostwa w gangu i bez otwierania menu. Pokazuje
rzeczywiście załadowany teren klienta, obraca się zgodnie z kierunkiem gracza
i ma wskaźnik północy przy krawędzi mapy. Stonowana szarość lądu i niebieska
woda zastępują jaskrawe kolory biomów. Teren ma krycie 176/255 (około 69%);
bez nieprzezroczystego podkładu. Filtracja dwuliniowa wygładza obrót i piksele.
Brakujące dane pozostają delikatnie przyciemnione. Nie pobiera ziarna
świata, nie generuje chunków i nie wysyła ciągłych zapytań o mapę na serwer.

Przetwarzanie: najwyżej 512 kolumn na tick, miękki budżet 2 ms, pamięć do
256 kafli chunków. Dane powierzchni są odświeżane po sześciu sekundach;
brakujące kafle są przygotowywane wcześniej. Jedna tekstura 176×176 jest
aktualizowana co najwyżej co pięć ticków, a nie na każdą klatkę.
Zmiana wymiaru i wyjście z serwera czyszczą pamięć oraz teksturę.

Obok mapki: oryginalne symbole banknotu i karty oraz dwa małe wiersze kwot,
bez dużych kafli i etykiet. Gotówka jest jasnozielona, bank jasnoniebieski.
Kwoty i litera N korzystają z dołączonego, ograniczonego podzbioru DejaVu Sans
Bold (około 6 KB, licencja dołączona). Największa kwota jest automatycznie
skalowana, aby pozostała pełna i mieściła się w dostępnej szerokości.
Nie są to znaki z przypadkowej czcionki. Symbole są rysowane wektorowo na
płótnie 128 jednostek i skalowane wraz z całym HUD-em. Układ uwzględnia
faktyczny rozmiar waniliowego hotbara, serc i pancerza. Nieaktywny czat jest
odsunięty ponad HUD; po otwarciu czatu jego zwykłe położenie pozostaje.

Nowy HUD jest ukryty przy F1 oraz w otwartych ekranach. Waniliowe serca,
głód i hotbar pozostają dostępne. Dane pieniędzy są oddzielnym prywatnym
pakietem dwóch liczb, bez retransmisji logo czy banera.

## Gotówka i bank

- Dotychczasowe pieniądze osobiste pozostają w banku. Gotówka zaczyna od 0 $.
- Wpłaty i wypłaty gangu oraz premie są operacjami bankowymi.
- Nie ma przelewów gotówka ↔ bank ani formularzy bankomatu.
- Śmierć, respawn i zmiana wymiaru zachowują oba salda. Pieniądze nie są
  przedmiotami wyrzucanymi z ekwipunku i nie są kopiowane z encji gracza.
- Operator, konsola albo właściciel singleplayer mogą używać:

```
/crimeandlaw pieniadze dodaj NazwaGracza gotowka 1000
/crimeandlaw pieniadze dodaj NazwaGracza bank 1000
```

Stara komenda bez rodzaju konta zasila bank. Komentarz operacji gangu
pozostaje opcjonalny. Nie ma opłat za śmierć ani automatycznych nagród z celów.

Sprawdzono publiczne materiały oficjalnej wiki Majestic dotyczące banku,
pierwszych kroków, śmierci/przedmiotów i zmian projektu. Nie znaleziono
dostatecznie jednoznacznego potwierdzenia, czy zwykła śmierć odbiera saldo
osobiste. Zasada zachowania pieniędzy w tym modzie nie jest przedstawiana
jako potwierdzona reguła Majestic. Przedmiot „Worek pieniędzy” jest osobną
rzeczą i jego opis nie rozstrzyga zachowania licznika gotówki gracza.

## Czarna lista

Lista, historia, wyszukiwanie, strony i przewijanie. Wpis ma gracza, UUID,
moderatora, opcjonalny powód i datę. Blokada należy do konkretnego gangu.
Nazwa może się zmieniać; blokada i pieniądze są przypisane do UUID.

Własne prawo rangi **Zarządzanie czarną listą** pozwala dopisywać i zdejmować
blokadę. Dodanie obecnego członka usuwa go z gangu, więc wymaga dodatkowo
prawa **Usuwanie członków**. Nie można zablokować lidera ani samego siebie.
Powód i autor pozostają w osobnej historii także po zdjęciu blokady.

Dodanie wpisu kasuje oczekujące zgłoszenie. Serwer ponownie sprawdza blokadę
podczas zgłaszania się i przyjmowania, niezależnie od aktywności przycisków.
Wyszukiwanie przy dodawaniu przyjmuje nick znany serwerowi albo pełny UUID;
nie odgaduje tożsamości nieznanego gracza. Lista i historia są ograniczone
technicznie do 10 000 wpisów, po 100 na stronę.

## Tygodniowe cele bez nagród

Wybrany przez użytkownika zakres to rzeczywisty postęp bez nagród pieniężnych
i rzeczowych. Robocze wymagania do dalszego dostrojenia:

| Cel | Wymaganie | Zliczanie |
| --- | --- | --- |
| Nowicjusze | 5 | Unikalne UUID przyjętych osób w danym tygodniu. |
| Wspólna akcja | 5 | Zakończone capty z rozstrzygnięciem; każde wydarzenie raz. |
| Kontrola terenu | 3 | Wygrane capty, po jednym naliczeniu za wydarzenie. |

Nowy tydzień zaczyna się w poniedziałek o 00:00 w strefie serwera ustawionej
dla captów. Odliczanie uwzględnia strefę i zmianę czasu. Cofnięcie zegara
nie resetuje powtórnie już rozpoczętego tygodnia. Postęp i identyfikatory
zdarzeń zapisują się ze światem. Anulowane capty nie naliczają postępu.
Nie istnieje przycisk ani pakiet „odbierz nagrodę”.
