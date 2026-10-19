# Sprawdzenie 0.1.2

19 października 2026. Minecraft 1.21.1, NeoForge 21.1.248, JDK 21.
Gradle clean build i wrapper zakończone BUILD SUCCESSFUL.

## Testy automatyczne

23 testy JUnit bez błędów: 12 modelu gangów, 2 walidacji PNG, 7 edycji obrazów
oraz konwersji kolorów i 2 proporcji/współrzędnych menu.

Nowe przypadki obejmują dowolną wizualną kolejność rang (również nad Liderem),
brak nadawania uprawnień przez przesuwanie, odrzucanie niepełnej/powielonej listy,
zapis/odczyt kolejności, PNG i JPG, spacje i polskie znaki w ścieżkach,
obrazy większe niż poprzedni limit 4096 px, konkretne błędy braku pliku i błędnego
formatu, granice kadru, przezroczyste marginesy, HSV/RGB oraz słabo kompresujące
się miniatury RGBA mieszczące się w limicie pakietu.

## Klient graficzny

Uruchomiono prawdziwy klient Minecraft w 1920×1080 na wirtualnym ekranie,
OpenGL 4.5 Mesa llvmpipe. Test używa danych przykładowych, bez logowania konta.
Końcowy przebieg zakończył się CRIME_UI_SMOKE_PASS i zapisał 12 obrazów.

Sprawdzono pola tekstowe i zakładki przy skalach GUI 2/4, otwarcie koła kolorów,
wybór koloru i zmianę krycia, podgląd wyglądu, wywołanie obsługi upuszczenia
pliku, asynchroniczne otwarcie edytora, przesuwanie/przybliżanie kadru,
zatwierdzenie i walidację logo oraz przeciąganie rangi z linią docelową.
Obejrzano zrzuty głównych widoków i obu edytorów. Test obrazu dodatkowo
sprawdza tło po zmianie skali i widoczność napisu „Anuluj” w oknach edytorów.

Podgląd PNG większy niż stos LWJGL jest dekodowany ze strumienia; końcowy test
korzysta z takiego obrazu i nie zgłasza błędu stosu. Warstwa kontrolek edytora
jest rysowana po wyczyszczeniu głębi, by podgląd nie zasłaniał przycisków.

Odtworzenie: `./gradlew -PuiSmoke runClient`. Kod testu graficznego jest
wykluczony z produkcyjnego JAR-a; sprawdzono jego zawartość.

## Granice weryfikacji

Test wywołuje metodę Minecrafta obsługującą upuszczenie pliku; nie odtwarza
fizycznego przeciągania z Eksploratora Windows ani systemowego okna wyboru
na komputerze użytkownika. Pozostają do sprawdzenia dwa rzeczywiste klienty
na serwerze, powiązana synchronizacja oraz wygląd na innych GPU/rozdzielczościach.
Wersja 0.1.0 wcześniej uruchomiła serwer dedykowany i utworzyła świat.
Nie przedstawiamy tego jako testu sieciowego wersji 0.1.2.

## Środowisko budowania

Lokalny NeoForm Runtime 2.0.31 używa fallbacku java.home/bin/java w dwóch
miejscach, gdzie ograniczone środowisko nie udostępnia ścieżki ProcessHandle.
Ta poprawka narzędzia budującego nie trafia do repozytorium ani JAR-a moda.
Usługi kont Minecraft były niedostępne przez DNS; nie blokowało to testu UI
z danymi przykładowymi. Pobranie zasobów uzupełniono z oficjalnego CDN,
z kontrolą rozmiarów i SHA-1 z indeksu gry.
