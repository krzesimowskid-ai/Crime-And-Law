# Katalog uprawnień — 0.1.39

Opcje z referencyjnych ekranów rang, dopasowane do gangu w Minecraft.
Dodatkowo zachowane prawo wyglądu menu/logo/baneru.
Wiersze „W przygotowaniu” są widoczne, ale nie można ich włączyć.
Serwer odrzuca próbę nadania takiego prawa również w ręcznie wysłanym pakiecie.

| Uprawnienie | Grupa | Stan |
| --- | --- | --- |
| Zapraszanie / zgłoszenia | Personel | Działa |
| Usuwanie członków | Personel | Działa |
| Zarządzanie rangami | Personel | Działa |
| Wygląd i logo gangu | Mienie i zaopatrzenie | Działa |
| Zmiana rang członków | Personel | Działa |
| Wydawanie ostrzeżeń | Personel | W przygotowaniu |
| Dostęp do magazynu / depozytu łupów | Mienie i zaopatrzenie | Działa dla depozytu łupów |
| Tworzenie przedmiotów z materiałów | Mienie i zaopatrzenie | W przygotowaniu |
| Wpłata środków na saldo | Finanse | Działa |
| Wypłata środków z salda | Finanse | Działa |
| Wypłacanie premii | Finanse | Działa |
| Resetowanie pojazdów gangowych | Transport | W przygotowaniu |
| Rozpoczynanie ataku na terytorium | Aktywności | Działa |
| Dostęp do napadu na bank | Aktywności | W przygotowaniu |
| Udział w ataku na terytorium | Aktywności | Działa |
| Zarządzanie czarną listą | Personel | Działa |
| Otwarcie AirDrop | Aktywności | W przygotowaniu |
| Zarządzanie szybkimi zestawami | Mienie i zaopatrzenie | W przygotowaniu |
| Otwieranie / zamykanie szybkich zestawów | Mienie i zaopatrzenie | W przygotowaniu |
| Tworzenie i usuwanie działów | Personel | W przygotowaniu |
| Przydzielanie działów | Personel | W przygotowaniu |
| Zbieranie dowodów podczas rajdu | Aktywności | W przygotowaniu |
| Przygotowania do rajdu / anulowanie | Aktywności | W przygotowaniu |

Kolejność rang nie nadaje praw. Lider zachowuje własność gangu niezależnie
od położenia na liście i może wykonywać wszystkie działające czynności.
Tworzenie/edycja rang oraz przypisywanie rang członkom są rozdzielone.
Starsze zapisane prawo zarządzania rangami zachowuje obie możliwości.

Grupa Transport w katalogu jest wyłącznie zbiorem nieaktywnych praw.
Główna kategoria transportu nadal jest ukryta. Nie dodano pojazdów, fizycznego magazynu,
zestawów ani rajdów. Finanse mają osobne działające uprawnienia.

Lider aktywuje gałąź kontroli terytoriów. Rozpoczynanie captu i udział w nim
można przyznać niezależnie rangom. Prywatne oznaczenia mapy należą do gracza;
oznaczenia gangowe może edytować każdy obecny członek tego gangu.

W neutralnym capcie rozpoczęcie udziału nowego gangu wymaga decyzji lidera
i własnego terenu w jednym z ośmiu sąsiednich chunków. Prawo udziału pozwala
członkom dołączyć do już zapisanej drużyny; nie zastępuje decyzji lidera.

Czarna lista usuwa obecnego członka tylko wtedy, gdy działająca osoba ma
również prawo usuwania członków. Lidera ani siebie nie można zablokować.

## Granice delegowania praw

Poza liderem osoba edytująca rangę musi posiadać wszystkie dotychczasowe
prawa tej rangi i wszystkie prawa, które chce zapisać. Przypisanie rangi
wymaga posiadania praw zarówno obecnej, jak i docelowej rangi członka.
Nie można w ten sposób nadać sobie lub komuś praw finansowych, których
samemu się nie posiada. Ograniczenia sprawdza serwer, również dla ręcznie
wygenerowanych pakietów. Edytor blokuje niedostępne przełączniki i zapis
chronionej rangi. Zmiana kolejności nadal nie nadaje żadnego prawa.
