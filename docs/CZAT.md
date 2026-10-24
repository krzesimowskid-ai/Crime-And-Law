# Czat — 0.1.33

Klasyczny kompaktowy układ w lewym górnym rogu, inspirowany Majestic RP:
gładka czcionka, godziny wiadomości, kolory, półprzezroczystość i pole pod
historią. Wybrano wariant bez zakładek; Majestic opisuje także taki tryb.
Nie dodano pozornych kanałów frakcji lub rodziny bez odpowiednich mechanik.

## Obsługa

- T: pisanie, /: rozpoczęcie komendy.
- Enter lub strzałka obok pola: wysłanie; Esc: zamknięcie.
- Góra/dół: ostatnio wysłane teksty i powrót do bieżącego szkicu.
- Kółko i Page Up/Down: historia, także przez przeciąganie paska.
- Tab: podpowiedzi nazw i komend otrzymanych z serwera.
- Kliknięcia i dymki wiadomości korzystają z ich oryginalnych komponentów.
- Zębatka: ustawienia czatu Minecrafta. Szerokość, skala, odstępy, kolory,
  przezroczystość tekstu/tła i ukrycie czatu wpływają na prezentację.

Minimapa, pieniądze i zegar pozostają widoczne podczas pisania. Dolny HUD
ma margines sześciu jednostek od lewej i od dołu. F1 i pozostałe menu nadal
kontrolują widoczność HUD-u. Nie ma przyciemnienia całego świata przy czacie.

## Zgodność i granice

Wysyłanie i komendy są obsługiwane przez natywny ChatScreen i połączenie
Minecrafta. Zachowano limit 256 znaków pola, podpisy, statusy zaufania,
dekorację nadawcy, historię wysyłania, kolejkę opóźnionych wiadomości
i usuwanie wiadomości. Nowy widok czyta oryginalną historię ChatComponent;
nie utrzymuje drugiej kopii wiadomości, która omijałaby usuwanie.

Natywna historia ma do 100 wpisów. Bufor widoku ma do 800 zawiniętych
wierszy, a godziny do 128 wpisów przed przycięciem do żywej historii.
Tekst jest zawijany ponownie po zmianie wiadomości, szerokości lub zasobów.
Podczas czytania starszych wiadomości nowy wpis zachowuje bieżącą kotwicę.
Przy zmianie GUI i powrocie z ustawień szkic nie znika.

Osobny podzbiór DejaVu Sans Regular (127 020 bajtów przed kompresją)
obejmuje polskie znaki, łacinę, grekę, cyrylicę i potrzebne symbole.
Pozostałe znaki korzystają z domyślnej czcionki gry. Licencja dołączona.

Zmiany są po stronie klienta. Protokół 19 i schemat zapisu 4 pozostają.
Pełne kanały IC/OOC, frakcyjne, głosowe i wiadomości nad postacią nie są
implementowane przez tę zmianę wyglądu istniejącego czatu Minecrafta.

Źródło wariantu klasycznego: [oficjalny changelog Majestic 4.267.157](https://wiki.majestic-rp.ru/ru/changelogs/sborka-4.267.157).
