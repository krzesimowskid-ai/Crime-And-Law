# Aurelia — pierwsza fikcyjna uprawa

Wersja 0.1.41, Minecraft 1.21.1, NeoForge 21.1.248, Java 21.
Na serwerze i u graczy potrzebny jest nowy JAR. Protokół sieciowy: **21**.

## Nasiona i sadzenie

Połącz w craftingu **nasiona pszenicy i świecące jagody**, aby otrzymać
dwa nasiona aurelii. To umowna receptura fikcyjnej rośliny w grze.
Nasiona są również w zakładce naturalnych bloków trybu kreatywnego.

Posadź na zaoranej ziemi. Światło i nawodnienie działają jak przy uprawach
Minecrafta. Roślina ma pięć etapów wzrostu, osobny model 3D i szczegółowe
liście. Można używać mączki kostnej. Podpowiedź po najechaniu pokazuje etap.

## Specjalne nożyce

Potrzebujesz **trzech sztabek żelaza, dwóch sztabek miedzi i patyka**.
Układ w stole:

| | | |
|---|---|---|
| pusto | żelazo | żelazo |
| pusto | miedź | żelazo |
| miedź | pusto | patyk |

Nożyce do przycinania są także w zakładce narzędzi trybu kreatywnego.
Mają własny model 3D, metalowe ostrza, miedziany przegub i kolorowe uchwyty.
Zwykłe nożyce nie uruchamiają zbioru.

## Zbiór

1. Weź nożyce do przycinania do **głównej ręki** i spójrz na dojrzałą roślinę.
2. Naciśnij **Z** zgodnie z podpowiedzią. Skrót można zmienić w sterowaniu.
3. Obracaj roślinę przeciąganiem tła lub prawym przyciskiem myszy.
   Kółko przybliża i oddala; strzałki lewo/prawo również obracają model.
4. Przytrzymaj lewy przycisk na jasnym znaczniku cięcia i puść w zielonym
   obszarze wskaźnika. Poprawnie odcięty liść odłącza się od modelu.
5. Każde poprawne cięcie daje **jeden liść** i zużywa narzędzie.
   Z dojrzałej rośliny można zebrać sześć liści. Po ostatnim odrasta
   od młodego etapu bez ponownego sadzenia.

Zbyt szybkie lub zbyt późne puszczenie przycisku nie daje nagrody i nie
usuwa liścia. Esc albo przycisk powrotu przerywa minigrę; pozostałe liście
zostają na roślinie, a już zebrane pozostają u gracza. Dotyczy to również
wyjścia z serwera i restartu. Przy pełnym ekwipunku liść wypada na ziemię.
Zniszczenie rośliny daje nasiona, a nie pominięcie minigry z nagrodą w liściach.

Roślina jest rezerwowana na czas zbioru dla jednego gracza. Serwer sprawdza
narzędzie, odległość, widoczność, ochronę miejsca, tożsamość rośliny,
konkretnego liścia oraz czas cięcia. Stary pakiet nie daje ponownej nagrody.
Przy utracie potwierdzenia okno odzyskuje stan bez ponawiania cięcia.

Liście są na razie surowcem do przyszłych fikcyjnych procesów. To pierwsza
gałąź produkcji; plan obejmuje również inne postacie produktów i inne
sposoby ich wytwarzania.

## Grafika

Roślina w świecie, model w minigrze i zebrany przedmiot korzystają z
rzeczywistej geometrii 3D. Liście mają zakrzywione powierzchnie, żyłkowanie,
ząbkowaną sylwetkę, delikatny ruch i animację odłączenia. Tekstura ma
własne mipmapy i filtrowanie trójliniowe; nie obniża mipmap atlasu Minecrafta.
Pochodzenie tekstury i pełny prompt: [AURELIA_ASSETS.md](AURELIA_ASSETS.md).
