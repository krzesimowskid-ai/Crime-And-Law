# Sprawdzenie 0.1.4

20 października 2026. Minecraft 1.21.1, NeoForge 21.1.248, JDK 21.
`clean build` zakończone poprawnie. 27 testów JUnit bez błędów.

## Zakres zmiany

Główna zakładka gangu według szkicu `image(20261020-111515).png`:
logo i nazwa na środku u góry szerokiej części treści, linia od nawigacji
do prawej ramki, nagłówek „Informacje o gangu” i trzy równe kolumny.
Pokazywane są liczba członków, lider i miejsce na poziom („— / Wkrótce”).
Nie dodano zasad XP ani awansowania; kolory, gradienty i przyciski zachowano.

## Klient graficzny

Rzeczywisty Minecraft 1920×1080, OpenGL 4.5 Mesa llvmpipe, dane przykładowe.
Końcowy przebieg CRIME_UI_SMOKE_PASS: 15 zrzutów, skale GUI 2 i 4.
Nowy panel sprawdzono bez logo i z zaimportowanym obrazem, także z nazwą
32-znakową. Test obrazu sprawdza linię pod nagłówkiem w trzech miejscach
oraz widoczny tekst we wszystkich trzech kolumnach. Obejrzano oba zrzuty.

Dotychczasowy przebieg dalej sprawdza tworzenie/wyszukiwanie, wpisywanie,
stronicowanie członków, zarządzanie lidera, ograniczenia zwykłego członka,
piksele pionowych gradientów, suwaki myszą/klawiaturą, krycie oraz import
pliku i przesuwanie/przybliżanie/zatwierdzanie kadru. Kontrolowane są też
tło nagłówka, nawigacja i napis „Anuluj” w edytorach.

Odtworzenie: `./gradlew -PuiSmoke runClient`. Kod testu graficznego nie trafia
do produkcyjnego JAR-a; sprawdzono zawartość i wersję 0.1.4 w metadanych.

## Testy i zgodność

27 testów: 11 modelu gangów (w tym migracja dawnych rang i duże grupy),
5 stronicowania/prywatności, 2 walidacji PNG, 7 edycji obrazów i kolorów,
2 proporcji menu. W tym wydaniu nie zmieniono modelu danych ani sieci.
Zachowano protokół 2 i zgodność z serwerem 0.1.3.

## Granice weryfikacji

Test graficzny używa danych przykładowych, bez dwóch połączonych klientów.
Nie odtwarza fizycznego przeciągania z Eksploratora Windows ani natywnego
okna wyboru pliku na komputerze użytkownika. System poziomów, nowe przyciski,
mapa i terytoria pozostają do dalszego ustalenia/wykonania.

## Środowisko

Budowanie korzysta z lokalnego NeoForm Runtime z fallbackiem java.home/bin/java
w dwóch miejscach, gdzie środowisko nie udostępnia ścieżki ProcessHandle.
Ta zmiana narzędzia nie trafia do repozytorium ani JAR-a. Brak dostępu do usług
kont Minecraft nie blokował testu z danymi przykładowymi.
