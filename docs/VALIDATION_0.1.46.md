# Weryfikacja 0.1.46 — fioletowe źródło UV

2026-10-26. Minecraft 1.21.1, NeoForge 21.1.248, Java 21.
Wersja testowa; protokół sieciowy 25 pozostaje bez zmian.

## Zmiana

Lampa wcześniej emitowała zwykłe światło poziomu 10, a osobny kompozytor
barwił gotowy obraz. Po doprecyzowaniu wymagania usunięto oba te elementy.
Górna część stelaża ma teraz zwykłą emisję równą zero.

Własne pole fioletowego światła jest próbkowane w shaderach materiałów
terenu, modeli, obiektów oraz przedmiotu w ręce. Kolor trafia do lightmapy
przed pomnożeniem przez teksturę i przed mgłą. Nie ma końcowego filtra
obrazu ani dodatkowego białego źródła tej lampy. Inne źródła światła
i światło dzienne nadal działają. HUD i nieoświetlane shadery interfejsu
pozostają poza polem UV.

Pole ma poziom źródła 10, spadek o jeden poziom na blok i propagację przez
sześciu sąsiadów. Obejście przeszkody przez drzwi kosztuje długość drogi.
Nieprzezroczysta, pełna ściana odcina przejście; szkło przepuszcza światło.
Wynik uwzględnia ustawienie jasności Minecrafta. Obsługiwanych jest do ośmiu
najbliższych lamp w już wczytanych chunkach.

## Wykonane sprawdzenia

- **234 testy w 39 zestawach**, zero niepowodzeń, błędów i pominięć.
  Nowe przypadki obejmują ominięcie przeszkody, otwieranie przejścia,
  symetrię i wygaszanie pola, zablokowane źródło oraz włączenie UV do
  lightmapy bez zmieniania nieoświetlanych shaderów i ich metadanych.
- Pełna próba `uv-material-3`: dwa rzeczywiste klienty TCP, dedykowany
  serwer, cały cykl przygotowania i zbioru oraz wcześniejsze kontrole
  ochrony, rezerwacji, zużycia zasobów i ponawiania pakietów — PASS.
- Rzeczywisty widok w zaciemnionym pokoju: fioletowe ściany, podłoga,
  stelaż i trzymana konewka w **Fancy oraz Fabulous** — PASS.
  Obejrzano zapisane obrazy i sprawdzono kolor stałego fragmentu ściany.
- Wyjęcie lampy usuwa fiolet; ponowny montaż i przeładowanie zasobów
  przywracają oświetlenie — PASS. Serwer sprawdza, że zamontowane UV
  ma zwykłą emisję blokową równą **0**.
- HUD zachowuje własne kolory w obejrzanych obrazach. Przygotowanie
  sprawdzono także w czterech wcześniejszych rozmiarach okna/skalach GUI.
- Zapis i ponowny start w tej próbie — PASS. Pierwszy serwer zakończył
  pracę kodem 0; stan wyposażenia, rośliny i ekwipunku przetrwał restart.
- JAR zbudowano po zakończeniu próby i wyłączeniu źródeł testowych.
  Integralność archiwum, zgodność klas/zasobów z kompilacją i brak klas
  automatyzacji są odnotowane w `validation-0.1.46.json`.

Dwie wcześniejsze próby bieżącej poprawki przerwała kontrola koloru ściany:
UV nie uwzględniało jeszcze korekcji jasności stosowanej do zwykłej
lightmapy. Uzupełniono tę korekcję i ponowiono pełną próbę z wynikiem PASS.
Nie uznano samego wykrycia aktywnej lampy za dowód poprawnego obrazu.

## Ograniczenia

**Pozostaje otwarta obserwacja zapisu z 0.1.45** opisana w
[poprzednim raporcie](VALIDATION_0.1.45.md). Obecny udany restart nie
wyjaśnia ani nie naprawia wcześniejszej utraty zmian stanowiska. Korzystaj
z kopii świata. Ta wersja poprawia oświetlenie.

Pole UV nie zwiększa zwykłej jasności blokowej po stronie serwera.
Aurelia wykrywa zamontowaną lampę bezpośrednio; inne mechaniki Minecrafta,
które wymagają waniliowego poziomu światła, nie otrzymują od UV białego
źródła. Częściowe kształty i nietypowe tłumienie bloków są przybliżone.
Nie wykonano pomiaru FPS, długotrwałej próby obciążeniowej ani kontroli
zamienników renderera i zewnętrznych paczek shaderów.
