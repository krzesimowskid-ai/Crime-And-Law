# Pierwszy balans terytoriów — propozycja z 22 października 2026

Ten dokument i `balance/territory-v1.json` są propozycją do implementacji.
**Wydanie 0.1.15 nie odczytuje tego pliku i nie ma jeszcze czynnej gospodarki,
ulepszania terenów ani wypłat.** Nie zmieniono wersji JAR-a ani protokołu.

## Podstawa decyzji

Użytkownik pozwolił opracować własny rozsądny balans, inspirowany funkcjami
kontroli terytorialnej Majestic. Walutą mają być pieniądze w grze, np. dolary,
zamiast surowców pełniących rolę pieniądza. Część korzyści pozostaje materialna
lub produkcyjna: nie każdy teren musi wypłacać pieniądze.

Podane niżej kwoty, procenty i czasy to **nasze robocze wartości**, nie konfiguracja
Majestic ani potwierdzone przez użytkownika finalne ceny. Można je zmieniać
bez zmiany pomysłu na dany typ korzyści.

## Pieniądze i dochód

- Jednostka: dolar `$`, dokładność dwóch miejsc, obliczenia w całych centach.
- Osobne konta gracza i gangu, prowadzone przez serwer. To projekt przyszłego
  salda, nie wymiana emeraldów ani diamentów na pieniądze.
- **Zwykły chunk nie daje dochodu ani poziomów bonusu.** Można mieć kilkanaście
  chunków i nie otrzymywać z nich żadnej korzyści.
- W pierwszym modelu pieniądze wynikają ze zweryfikowanej lokalnej działalności
  w zakwalifikowanym źródle. Sam zakup lub upływ czasu nie tworzy dochodu.
  Pasywnych wypłat i naliczania po wyłączeniu serwera nie dodaje ten model.
- Referencyjne **100 $ na godzinę kwalifikowanej aktywności** służy porównywaniu
  ulepszeń. Nie jest obietnicą wypłaty każdemu budynkowi co godzinę.
- Realne źródła podstawowego zarobku, popyt i zakup neutralnej ziemi wymagają
  osobnego cennika. Nie utożsamiać procentowego udziału w obrocie z pełnym
  przychodem, zyskiem netto ani stałą wypłatą.

## Pięć poziomów

Standardem projektu jest pięć poziomów **kwalifikującego się źródła**, nie
każdego budynku lub chunka. Wyjątkowy poziom szósty pozostaje wyłączony.
Nie zmienia to osobnego przyszłego systemu poziomu gangu.

| Poziom | Dochód względem bazy | Koszt awansu względem godzinnego przychodu bazowego | Minimalna kontrola łącznie |
|---|---:|---:|---:|
| 1 | 100% | zdobycie terenu osobno | 0 h |
| 2 | 115% | ×0,60 | 2 h |
| 3 | 130% | ×1,20 | 6 h |
| 4 | 145% | ×2,40 | 14 h |
| 5 | 160% | ×3,60 | 30 h |

Koszt dotyczy pojedynczego przejścia na wskazany poziom, a czas jest minimalnym
łącznym czasem tego okresu kontroli. Nie wolno sumować czasów z tabeli ponownie.
Robocza propozycja łączy wymagany czas utrzymania i opłatę przy awansie; nie
uruchamia automatycznego pobierania pieniędzy ani automatycznego awansu.
Zegar kontroli działa podczas pracy serwera. Obliczanie po wyłączeniu świata,
reset przy zmianie właściciela i przyszłe reguły zdobywania wymagają kontrolera
własności oraz osobnego sprawdzenia.

Dla referencyjnej bazy 100 $/h ulepszenia kosztują 60 / 120 / 240 / 360 $.
Każdy poziom dodaje 15 $/h, więc koszt zwraca się z **dodatkowego** przychodu
odpowiednio po 4 / 8 / 16 / 24 godzinach referencyjnej aktywności. Ulepszenia
nie finansują natychmiast kolejnych. Przy mniejszym popycie zwrot trwa dłużej.
Kwoty skalują się z bazą źródła; nie stosować ceny 60 $ do dowolnie bogatego punktu.

## Korzyści inne niż pieniądze

To skale dla przyszłych kwalifikujących się efektów, nie lista gotowych
bonusów przyznawanych za aktualne etykiety mapy.

| Efekt | Poz. 1 | Poz. 2 | Poz. 3 | Poz. 4 | Poz. 5 |
|---|---:|---:|---:|---:|---:|
| Dodatkowy uzysk zasobów | +2% | +4% | +6% | +8% | +10% |
| Tempo wzrostu upraw | +5% | +9% | +12% | +14% | +15% |
| Tempo produkcji | +10% | +18% | +24% | +28% | +30% |
| Obniżka kwalifikującego się kosztu | 2% | 4% | 6% | 8% | 10% |
| Skrócenie przestoju po sabotażu | 10% | 18% | 24% | 28% | 30% |
| Skrócenie kwalifikującego się czasu odnowienia | 5% | 9% | 12% | 14% | 15% |

Mocniejsze efekty mają malejące przyrosty. **+15% tempa wzrostu** daje czas
cyklu równy `czas bazowy / 1,15`, czyli około 13,04% krótszy, nie 15% krótszy.
Przedmiotów i centów nie zaokrągla się po każdej małej czynności w dół:
ułamkowy postęp jest zachowywany do kolejnego naliczenia.

**Blokada sabotażu** i **dostęp do produkcji** pozostają osobnymi możliwościami,
bez wymyślonej skali „20–100% odblokowania”. Blokady nie zastępuje samo
skracanie przestoju. Warunki uzyskania tych możliwości wymagają przypisania
do odpowiednich obiektów. Nie dodano nowych receptur ani pierwiastków w tym etapie.

## Kwalifikacja i łączenie efektów

- Zapisany biom, gleba, woda lub fragment wioski nie wystarcza do kwalifikacji.
  Każdy typ wymaga osobnego zestawu krytycznych kryteriów. Brak danych oznacza
  brak przyznanego bonusu. Obecne 16 próbek nie jest pełnym pomiarem kwalifikacji.
- Podstawy mają wynikać z pierwotnego terenu oraz wygenerowanych budynków.
  Budynek gracza nie tworzy nowego źródła. Raz prawidłowo potwierdzony potencjał
  pozostaje stały mimo przebudowy.
- Korzyść jest lokalna dla właściciela kwalifikującego się chunka. Nie zmienia
  się automatycznie w premię obejmującą cały świat lub wszystkie konta gangu.
- Ten sam wygenerowany budynek przecięty granicami chunków ma jeden identyfikator
  źródła; fragmenty nie stają się osobnymi kopiami nagrody.
- Dla jednego zdarzenia i tego samego efektu wybiera się najwyższą prawidłową
  wartość z limitem tabeli. Nie dodaje się procentów ani nie mnoży ich wielokrotnie.
- To samo zdarzenie gospodarcze nie może zostać ponownie wypłacone po odświeżeniu
  menu, rozłączeniu lub ponownym przetworzeniu. Konieczne będzie rozliczanie i zapis
  po stronie serwera. Symulacja nie zastępuje tej przyszłej implementacji.
- Więcej rzeczywistych, niezależnych działań może dawać więcej dochodu. Stała
  pula tych samych działań rozłożona na więcej chunków nie tworzy dodatkowej kasy.

## Zakres

Osobna zakładka **Premie** pozostaje do późniejszego opracowania. Nie jest
przekształcana w zakładkę bonusów terenowych. Transport nadal ma wyłącznie
ukrytą definicję. Nie aktywowano policji, gangów NPC, samolotów, samochodów,
automatycznego przejmowania ani wszystkich kategorii znanych z GTA.

## Sprawdzenie modelu

```text
python3 tools/territory_balance.py --output docs/SYMULACJA_BALANSU.md
```

Narzędzie sprawdza granice procentów, przyrosty i zwrot kosztów oraz scenariusze:
zwykłe/bezczynne tereny, nakładanie efektów, podział tej samej aktywności na wiele
działek i małe naliczenia z zachowaniem ułamków. Korzysta wyłącznie z biblioteki
standardowej Pythona; nie otwiera świata i nie modyfikuje danych graczy.
Raport: [SYMULACJA_BALANSU.md](SYMULACJA_BALANSU.md).

Dobór rzadkości kwalifikujących się terenów, kosztów pozostałych rodzajów
ulepszeń, dochodów NPC i dostępności surowców wymaga dalszej implementacji
i testów rozgrywki. Obliczenia nie dowodzą idealnej trudności.

## Źródła inspiracji i odróżnienie od naszych liczb

- [Mitsuki — mapa kontroli](https://mitsuki-hub.ru/captures?tab=map): opisy
  funkcji punktów. Katalog opisów nie jest pełną konfiguracją serwera Majestic.
- [Oficjalna wiki — kontrola terytorium](https://wiki.gta5majestic.com/pl/posts/kontrola-terytorium):
  rozwój przez kontrolę, dodatkowy szósty poziom za sprawą laboratorium,
  osobne trzy poziomy pomieszczeń produkcyjnych i przykłady skali bonusów.
- [Oficjalna aktualizacja z 25.08.2026](https://wiki.majestic-rp.ru/ru/changelogs/sborka-4.2636.1329):
  w Majestic osiąganie piątego poziomu pola po dziesięciu dniach utrzymania.
  Nasz roboczy harmonogram ma inne czasy i opłatę; nie jest kopią tych zasad.
- [Oficjalna wiki — zarządzanie organizacją](https://wiki.gta5majestic.com/pl/posts/zarzadzanie-organizacja-2):
  osobne drzewo ulepszeń rodzinnych; szczegółowe wymagania widoczne w grze.
