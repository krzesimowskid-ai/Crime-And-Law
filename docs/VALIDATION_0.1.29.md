# Walidacja 0.1.29 — 24 października 2026

Finanse gangu i usunięcie opcji zestawów ubrań. Minecraft 1.21.1,
NeoForge 21.1.248, Java 21. Schemat zapisu 3; protokół sieciowy 17.

## Testy kodu

141 testów przechodzi bez błędów i pominięć. Nowe testy sprawdzają zachowanie
sumy pieniędzy, osobne prawa wpłaty/wypłaty/premii, kwoty zerowe/ujemne,
przekroczenie salda lub limitu konta, błędnego odbiorcę na końcu paczki,
zmianę członkostwa przed wypłatą, własną premię, zbyt długi komentarz,
próby wstawienia formatowania, limity i filtrowanie historii oraz migrację.
Sprawdzono też ścisłe odczytywanie kwoty liczbowej z pakietu: tekst, ułamek,
wartość ujemna i przekroczenie zakresu są odrzucane.

Migracja usuwa wyłącznie historyczne identyfikatory EDIT_OUTFITS i CREATE_OUTFITS;
zachowuje pozostałe prawa, członkostwo i dane. Nieznane uprawnienie nadal
powoduje błąd zapisu zamiast nadania nowego prawa.

## Rzeczywisty serwer i dwa klienty

Uruchomiono dedykowany serwer i dwa klienty po lokalnym TCP, z rzeczywistymi
formularzami menu i pakietami. Trzeci członek gangu pozostawał offline.

- Wpłata 1000 $ przez lidera i wypłata 500 $ z formularzy.
- Premia pojedyncza 250 $ i premia masowa po 300 $ dla dwóch członków,
  w tym niepołączonego z serwerem.
- Członek bez prawa premii widzi nieaktywne przyciski. Samodzielnie wysłany
  pakiet wypłaty premii również jest odrzucany przez serwer.
- Członek z samym prawem wpłaty może wpłacić 50 $, lecz nie wypłacić pieniędzy.
- Powtórzenie rozliczonego pakietu, powtórzenie odbiorcy, ułamek, kwota ujemna
  i przekroczenie salda nie zmieniają kont ani historii.
- Końcowe salda: lider 19 500 $, członek 500 $, odbiorca offline 300 $,
  gang 79 700 $. Suma pozostaje równa 100 000 $.
- Siedem udanych transakcji w historii finansowej; trzy wydarzenia
  członkostwa w osobnym Rejestrze.
- Serwer zapisał świat, zakończył pracę i uruchomił go ponownie. Konta,
  wpisy, członkostwo oraz osobne prawa rang zostały odczytane poprawnie.

Sterowniki: `src/financeTest`, włączane przez `-PfinanceSmoke`.
Zadania `runFinanceServer`, `runFinanceOwner`, `runFinanceMember` obsługują
izolowane katalogi `run-finance-*`. Dla testów należy przygotować lokalny
serwer na 127.0.0.1:25595, tryb offline i zaakceptować EULA Minecrafta.
Weryfikację ponownego odczytu uruchamia `-PfinanceRestart=true`.
Automatyzacja i dane testowe są wyłączone z normalnego wydania.

## Interfejs

Obejrzano zrzuty rzeczywistego renderera Minecrafta przy 1600×900,
skalach GUI 2 i 4, na Linux/Mesa/Xvfb. Sprawdzono panel salda, historię premii,
formularze wpłaty, premii pojedynczej i masowej, sumę odbiorców i koszt,
stan przycisków członka bez uprawnień. Rozmiar głównego menu zachowano.
Pełne szczegóły transakcji są dostępne po najechaniu na wiersz.

Czarna lista i Cele pozostają panelami w przygotowaniu. Nie dodano AirDropów,
ubrań ani automatycznych zarobków z terytoriów. Istniejących testów rozgrywki
captów nie ponawiano; bieżący zakres dotyczy finansów i katalogu uprawnień.

## Plik wydania

Końcowe `clean build --offline`: sukces. CRC głównego JAR-u oraz 15 zagnieżdżonych
bibliotek poprawne. W wydaniu nie ma klas automatyzacji i podglądów.

Plik: `crimeandlaw-0.1.29.jar`, 1952925 bajtów.

SHA-256: `d413aa2f1cf0d7fe8200512d10657d6ff69bb0b1fd44ea3a546e4da52be340ff`.
