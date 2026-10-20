# Sprawdzenie 0.1.3

20 października 2026. Minecraft 1.21.1, NeoForge 21.1.248, JDK 21.

## Testy automatyczne

27 testów JUnit bez błędów: 11 modelu gangów, 5 stronicowania/prywatności,
2 walidacji PNG, 7 edycji obrazów i kolorów, 2 proporcji/współrzędnych menu.

Sprawdzono zachowanie starszego zapisu z dodatkowymi rangami i uprawnieniami:
właściciel, członkowie, zgłoszenia, logo i motyw pozostają, dawny rekruter
nie uzyskuje czynności lidera. Przekazanie przywództwa odbiera zarządzanie
poprzedniemu liderowi. Próby przyjęcia, odrzucenia, usunięcia, przekazania
przywództwa oraz zmiany wspólnego wyglądu/logo przez zwykłego członka są odrzucane.

Gang z 1001 członkami i 2000 zgłoszeń przechodzi zapis/odczyt bez limitu.
Dla 10 000 członków oraz 5000 zgłoszeń przejrzano wszystkie 1667 stron członków:
każdy występuje raz na liście. Właściciel i odbiorca pozostają w danych kontekstu,
bez dodatkowych wierszy na stronie. Liczniki pokazują całą grupę. Widok z logo
maksymalnej długości ma mniej niż 30 000 znaków przy limicie pakietu 250 000.
Strony spoza zakresu są korygowane, a zwykły członek nie otrzymuje zgłoszeń.

## Klient graficzny

Rzeczywisty klient Minecraft 1920×1080, OpenGL 4.5 Mesa llvmpipe, przykładowe
dane bez połączenia z serwerem. Końcowy przebieg: CRIME_UI_SMOKE_PASS,
14 zrzutów obrazu. Sprawdzono:

- tworzenie, wpisywanie i przełączanie zakładek przy GUI 2 i 4;
- centralną kartę, brak zakładki rang i ustawienia nad opuszczeniem gangu;
- listę członków, drugą stronę i oddzielne zarządzanie lidera;
- ukrycie zarządzania i zgłoszeń przed zwykłym członkiem;
- rzeczywiste piksele gradientu: czerwony u góry jasności, czerń na dole,
  neutralny odcień na dole nasycenia;
- zmianę krycia pionowym suwakiem, przeciąganie jasności oraz Home/strzałkę w dół;
- obsługę upuszczenia PNG, asynchroniczne otwarcie edytora, przesuwanie,
  przybliżanie i zatwierdzenie kadru, walidację końcowego logo;
- widoczność „Anuluj” oraz tła nagłówka, ramki nazwy i nawigacji po zmianach widoku.

Oględziny zrzutów wykryły zanik części tła po przełączaniu zakładek.
Włączono zapis do bufora głębi przed jego czyszczeniem w panelu i edytorach.
Dodane sprawdzenia pikseli przechodzą, a końcowe obrazy pokazują pełne tło.
Obejrzano główny panel, gradienty, zarządzanie, logo i ustawienia członka.

Odtworzenie: `./gradlew -PuiSmoke runClient`, wynik w `build/ui-smoke`.
Wydanie JAR nie zawiera klasy GangUiSmokeTest ani danych testowych.

## Budowanie i zgodność

`clean build wrapper --gradle-version 9.2.1 --distribution-type bin` z JDK 21.
Przy wydaniu sprawdzane są testy, zawartość JAR-a, wersja 0.1.3 w metadanych
oraz zgodność binarnego wrappera z dotychczasowym plikiem repozytorium.

Protokół panelu ma wersję 2. Na kliencie i serwerze należy zainstalować 0.1.3;
nie łączyć jej z 0.1.2. Format zapisu świata pozostaje zgodny ze starszą wersją.

## Granice weryfikacji

Test UI używa przykładowych danych, nie łączy dwóch graczy. Synchronizacja
między rzeczywistymi klientami nie została sprawdzona w tym przebiegu.
Obsługa upuszczenia jest wywoływana przez metodę Minecrafta; fizyczne
przeciąganie z Eksploratora Windows i natywne okno plików na komputerze
użytkownika pozostają poza tym testem. Mapy/terytoria i pozostałe systemy
nie należą do tej wersji.

## Środowisko budowania

Lokalny NeoForm Runtime 2.0.31 ma fallback java.home/bin/java w dwóch miejscach,
gdzie ograniczone środowisko nie udostępnia ścieżki ProcessHandle. Zmiana tego
narzędzia nie trafia do repozytorium ani JAR-a. Zasoby pobrano z oficjalnych
serwerów Minecrafta z weryfikacją rozmiarów/hashów indeksu. Niedostępne usługi
kont Minecraft nie blokowały testu graficznego z danymi przykładowymi.
