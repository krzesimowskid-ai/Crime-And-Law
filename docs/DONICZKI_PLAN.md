# Doniczki i jednorazowe uprawy — projekt następnego etapu

Stan: wymagania i propozycja wykonania z 25 października 2026.
To plan. Wydany JAR 0.1.42 pozostaje bez zmian: sadzenie na zaoranej ziemi,
odrost, receptura i dotychczasowy drop nasion nadal w nim działają.

## Decyzje użytkownika

- Pełny zbiór zużywa roślinę. Kolejny cykl wymaga nowych kupowanych nasion
  lub sadzonki; jeden zakup nie może stworzyć nieskończonej farmy gangu.
- Docelowe sadzenie odbywa się we własnej doniczce. Zaorana ziemia jest
  pierwszą uproszczoną wersją, zastępowaną przez ten system.
- Przygotowanie doniczki obejmuje minigrę nasypywania specjalnej ziemi
  z worka: widoczne wysypywanie, opadanie i układanie się podłoża.
- Różne worki mają pojemność odpowiadającą jednemu, dwóm lub trzem
  nasypaniom. Przykładowe wielkości nie są jeszcze cenami ani bilansem.
- Istnieją oba przedmioty: woreczek z kilkoma nasionami oraz pojedyncze
  nasiono. Można rozpakować woreczek; sadzenie rozpoznaje oba warianty.
- Podlewanie odbywa się konewką.
- Do stalowego stelaża można zamocować lampę nad rośliną.
- Plon jest losowy. Lampa może poprawiać szansę lepszego wyniku i skracać
  wzrost, ale nie ma dawać zawsze identycznej maksymalnej nagrody.
- Zachowujemy własny pikselowy styl 3D roślin, liści i narzędzi z 0.1.42.

## Proponowany przebieg w grze

1. Gracz kupuje nasiona lub sadzonkę i potrzebne materiały.
2. Ustawia pustą doniczkę; uruchamia przygotowanie przyciskiem interakcji.
3. W minigrze przechyla worek, kieruje strumień do doniczki i nasypuje
   kolejne porcje. Ziemia osiada, a poziom podłoża zmienia model.
4. W tym samym oknie wybiera nasiono lub sadzonkę. Jeśli ma tylko woreczek,
   może rozpakować go w tym kroku i wykorzystać jedną sztukę.
5. Sadzi jedną roślinę i podlewa konewką. Stan wody jest widoczny w modelu
   oraz w podpowiedzi; konewka ma skończoną zawartość i daje się napełniać.
6. Opcjonalnie montuje stalowy stelaż i lampę. Doniczka działa także bez
   ulepszenia; lampa jest częścią wyposażenia gracza, nie darmowym plonem.
7. Po dojrzeniu gracz zbiera widoczne liście w dowolnej kolejności,
   korzystając z istniejącej minigry nożyc.
8. Po ostatnim liściu roślina kończy cykl. Doniczka, konewka, stelaż i lampa
   pozostają do ponownego użycia. Następny cykl wymaga nowego nasiona.

Receptury i premie są mechanikami fikcyjnych gatunków w grze.

## Otwieranie worka — doprecyzowanie użytkownika

Użytkownik rozważył krótkie nacięcie worka ziemi, aby utworzyć otwór do
wysypywania. To osobna czynność od przycinania liści. Pod koniec wypowiedzi
pozostawił ten pomysł otwarty, dlatego nie dodajemy obowiązku posiadania
specjalnych nożyc do otwierania worków. Możliwa realizacja: gest otwarcia
lub nacięcia w minigrze, następnie przechylenie worka. Otwarty worek
zachowuje pozostałą zawartość; samo otwarcie nie zużywa porcji ziemi.

## Pojemność worków i obsługa nasion

Propozycja robocza: worek przechowuje 1, 2 lub 3 **porcje ziemi**.
Doniczka potrzebuje ustalonej liczby porcji, przykładowo trzech. Wtedy można
użyć trzech małych worków, małego i średniego albo jednego dużego.
Każde nasypanie zużywa dokładnie jedną porcję i uruchamia osiadanie ziemi.
To interpretacja wykonawcza przykładu użytkownika, jeszcze nie ostateczny
przelicznik. Liczbę porcji do doniczki i wymianę podłoża między cyklami
należy dobrać razem z czasem minigry i ekonomią.

Propozycja woreczka: zamknięty przedmiot z opisaną liczbą i typem nasion.
Rozpakowanie zamienia go na dokładnie tę liczbę luźnych sztuk. Okno
sadzenia wykrywa luźne nasiona, zamknięte woreczki i obsługiwane sadzonki.
Domyślnie proponuje luźne nasiono; gracz może wybrać inne dostępne źródło.
Jeśli wybrano woreczek, pokazuje krok rozpakowania i pozostawia pozostałe
nasiona w ekwipunku. W przykładzie opakowania po 5 nasion po zasadzeniu
jednej rośliny zostają 4 — liczba 5 jest wyłącznie przykładem interfejsu.
Brak miejsca blokuje rozpakowanie przed zmianą przedmiotów.

## Ekonomia i zabezpieczenia do wdrożenia

- Wyłączyć odrost po pełnym zbiorze, odcinając dotychczasową pętlę
  `HarvestController` -> `regrow()` -> młody etap.
- Usunąć recepturę dwóch nasion z pszenicy i świecących jagód oraz jej
  odblokowanie. Zwykłe surowce nie mogą zastępować kupowanych nasion.
- Zastąpić loot rośliny: obecnie złamanie zawsze daje nasiono i może dać
  drugie. Zniszczona lub zebrana roślina nie oddaje nasion ani sadzonek.
- Objąć tym również sadzenie/niszczenie młodej rośliny, narzędzia z Fortune
  lub Silk Touch, eksplozje i ewentualne przyszłe automatyczne zbieracze.
- Przeprowadzka doniczki nie może odnowić rośliny, zwrócić zużytego nasiona
  ani ponownie wylosować plonu. Pusta doniczka zachowuje własny przedmiot.
- Zakup musi pobrać pieniądze i przyznać przedmiot jako jedną operację
  serwera, z kontrolą miejsca w ekwipunku oraz ochroną przed powtórzeniem.
  Sprzedawca, miejsce zakupu, ceny i rodzaj salda nie są jeszcze wybrane.
- Serwer sprawdza przedmioty przed akcją i przy jej zatwierdzeniu. Jedno
  sadzenie zużywa jedno nasiono, nie nasiono i woreczek jednocześnie.
- Rozpakowanie, nasypanie i sadzenie mają osobne, jednorazowe zatwierdzenia.
  Dwie osoby nie mogą jednocześnie zmieniać tej samej doniczki.
- Potwierdzona porcja ziemi zostaje w doniczce po zamknięciu okna;
  ponowne wejście, rozłączenie i restart nie cofają zużycia ani postępu.
- Ilość plonu jest losowana na serwerze raz na cykl, przy dojrzewaniu,
  i zapisywana z tożsamością rośliny oraz pozostałymi liśćmi.
  Ponowne otwarcie minigry, relog i restart nie dają kolejnego losowania.
- Bonus lampy uwzględnia jej działanie podczas wzrostu; zamontowanie jej
  tuż przed zbiorem nie przyznaje premii za cały wcześniejszy cykl.
- Kilka lamp nie może bez ograniczenia sumować premii na tej samej roślinie.
  Parametry zasilania, koszt utrzymania i zużycie lampy pozostają do bilansu.

## Losowość i lampa — rozdzielenie biologii od zasad gry

Rośliny korzystają ze światła do fotosyntezy; lampa ogrodnicza nie jest
synonimem lampy UV. Materiały University of Minnesota opisują oświetlenie
LED i światło użyteczne dla roślin. Badanie Qian i współautorów na ogórku
pokazuje, że odpowiedź na UV zależy od zakresu promieniowania: nie ma
uniwersalnej zasady „UV zawsze przyspiesza i zwiększa plon”.

Dlatego proponowana nazwa przedmiotu to **lampa do wzrostu roślin** lub
**lampa ogrodnicza LED**, montowana na stalowej ramie. Proponowane skrócenie
wzrostu i poprawa szansy większego plonu są umownymi parametrami gry,
a nie przełożeniem liczb z badań na rzeczywistą uprawę.

Dalsze doprecyzowanie po pytaniu użytkownika o UV: nazwa „ogrodnicza”
określa funkcję lampy, a „UV” zakres promieniowania. LED określa
technologię źródła i nie jest przeciwieństwem UV. Oświetlenie podstawowe
w uprawach obejmuje m.in. LED i HPS; dodatkowy emiter UV może być osobnym
modułem lub częścią tej samej oprawy. UV oddziałuje biologicznie, lecz nie
zastępuje podstawowego oświetlenia fotosyntetycznego.

Badania na konopiach nie potwierdzają uniwersalnej premii UV do ilości
plonu ani zawartości kannabinoidów. Rodriguez-Morrison et al. (2021) oraz
Llewellyn et al. (2022) nie wykazali takich ogólnych korzyści w swoich
warunkach; obserwowano także skutki niekorzystne. Huebner et al. (2024)
wykazali zmiany części związków aromatycznych, bez zmiany profilu
kannabinoidów, przy zróżnicowanych, często niekorzystnych wynikach dla
plonu i jakości. To wyjaśnienie nazwy i roli światła, bez przenoszenia
parametrów doświadczeń na mechanikę moda.

Propozycja do gry pozostaje otwarta: podstawowa lampa ogrodnicza na
stalowym stelażu, a moduł UV jako możliwe osobne ulepszenie dla fikcyjnej
rośliny. Ewentualny efekt UV byłby umownym balansem gry, nie biologiczną
gwarancją większego plonu. Nie jest to jeszcze dodany przedmiot ani
zatwierdzona nowa gałąź ulepszeń.

Przykład roboczego balansu w grze: plon 3–6 liści, a działająca lampa
zwiększa szansę wyniku 5–6 bez gwarantowania maksimum. Liczba widocznych,
możliwych do odcięcia liści odpowiada wylosowanemu wynikowi; jeden liść
jest jedną nagrodą. Zakres, rozkład prawdopodobieństwa i ewentualna premia
do czasu nie są jeszcze ostatecznie ustalone.

Źródła sprawdzone 25 października 2026:

- [University of Minnesota — Lighting for indoor plants and starting seeds](https://extension.umn.edu/garden-and-home/yard-and-garden/gardening-in-minnesota/lighting-for-indoor-plants).
- [Qian et al., Plant Physiology 2021 — Downsizing in plants](https://academic.oup.com/plphys/article/187/1/378/6294311), DOI 10.1093/plphys/kiab262.

Dodatkowe źródła o lampach i UV, sprawdzone 25 października 2026:

- [University of Missouri — Understanding Grow Lights](https://extension.missouri.edu/publications/g6987).
- [Rodriguez-Morrison et al. 2021 — Cannabis Inflorescence Yield and Cannabinoid Concentration Are Not Increased With Exposure to Short-Wavelength Ultraviolet-B Radiation](https://www.frontiersin.org/journals/plant-science/articles/10.3389/fpls.2021.725078/full).
- [Llewellyn et al. 2022 — Indoor grown cannabis yield increased proportionally with light intensity, but ultraviolet radiation did not affect yield or cannabinoid content](https://www.frontiersin.org/journals/plant-science/articles/10.3389/fpls.2022.974018/full).
- [Huebner et al. 2024 — Influence of different UV spectra and intensities on yield and quality of cannabis inflorescences](https://www.frontiersin.org/journals/plant-science/articles/10.3389/fpls.2024.1480876/full).

## Kolejność wykonania

1. Jednorazowy cykl, kupowane wejściowe surowce i usunięcie dróg odzyskiwania
   nasion; sposób zakupu dopasowany do istniejącej ekonomii.
2. Doniczka, stan ziemi i wody, worki, opakowania nasion i konewka.
3. Minigra nasypywania, rozpakowanie i sadzenie w jednym przebiegu okna.
4. Stalowy stelaż, lampa, zapisany losowy plon i umiarkowane premie.
5. Próby dwóch graczy: rezerwacja, anulowanie, pełny ekwipunek, rozpakowanie,
   kilka worków, powtórzone pakiety, wymiana przedmiotów, relog i restart.

Nie używać tego dokumentu jako wykazu funkcji już działających w JAR.
