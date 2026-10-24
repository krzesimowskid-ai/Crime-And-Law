# Walidacja 0.1.33 — 24 października 2026

Minecraft 1.21.1, NeoForge 21.1.248, Java 21. Protokół 19, schemat danych 4.

## Zakres

Nowy widok natywnego czatu w lewym górnym rogu: tekst, czas nadejścia,
kolory, zawijanie, półprzezroczystość, historia, pasek przewijania, edytor
ze strzałką wysyłania i ustawieniami. Komendy, podpisy i usuwanie wiadomości
pozostają obsługiwane przez Minecrafta. Czytelność i trafianie kursorem są
niezależne od natywnego GUI 2/4 dzięki wspólnemu przeliczaniu współrzędnych.

Cały HUD jest widoczny przy otwartym czacie. Mapa ma margines 6 jednostek
od lewej i od dołu; liczniki przesuwają się razem z nią. F1 i pozostałe menu
zachowują dotychczasowe zasady. Ekonomia i zapis sald nie są zmieniane.

## Sprawdzenia automatyczne

159 testów JUnit: zachowane poprzednie testy oraz granice obszaru czatu,
edytora i HUD-u w ośmiu natywnych rozmiarach GUI, przeliczanie kliknięć,
przewijanie do obu krańców i zachowanie offsetu przy zmianie strony/rozmiaru.
Przekroczenia int podczas przewijania nie omijają granic.

Sterownik `src/financeTest` sprawdza rzeczywiste wysłanie tekstu przez TCP
i jego echo z serwera, historię góra/dół oraz szkic, Tab, przewijanie,
komponent z akcją SUGGEST_COMMAND, skalowanie GUI i przycisk wysyłania.
Test pikseli ikon jest wykonywany także przy otwartym czacie.

Wyniki rzeczywistego renderera, serwera, restartu i kontrola JAR-u są
zapisane w `validation-0.1.33.json`. Sterowniki nie są częścią wydania.

## Wynik końcowy

BUILD SUCCESSFUL: `build --offline -x createMinecraftArtifacts -x downloadAssets`.
Użyto gotowych, niezmienionych artefaktów Minecrafta / NeoForge 21.1.248.
159 testów: zero niepowodzeń, błędów i pominięć.

Rzeczywisty renderer, GUI 2/4: czat jest w lewym górnym rogu, a minimapa,
obie kwoty i zegar są widoczne podczas pisania. Obejrzano zrzuty pola,
podpowiedzi, starszej historii, klikanej wiadomości i powrotu z ustawień.
Test pikseli potwierdza widoczność obu ikon sald w otwartym czacie.

Przeszły: wysłanie i echo przez TCP, góra/dół ze szkicem, Tab, kółko/pasek,
kliknięcie SUGGEST_COMMAND, natywne opóźnione usunięcie wiadomości, zmiana
GUI i powrót z ustawień. Powtórzono testy finansów, czarnej listy, celów,
zegara oraz restartu. Test usuwania używa lokalnej próbki z sygnaturą
i natywnej metody ChatComponent; nie jest osobnym testem sieciowego pakietu
usunięcia z serwera.

CRC głównego JAR-u i 15 bibliotek: poprawne. Brak klas testów graficznych.
Nowy font i jego licencja są w paczce.

`crimeandlaw-0.1.33.jar`: 2 120 793 bajty.

SHA-256: `fca43da09c76a8b6bd3fd69add2bd9be09099230fede965e385169557cfa44ef`.
