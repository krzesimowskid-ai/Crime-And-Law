# Walidacja 0.1.28 — 24 października 2026

Wynik: **PASS**. Symetryczny znacznik gracza z pełnym czarnym konturem.

Poprzedni znacznik miał prostokąty o nieparzystych szerokościach,
przesunięte o pół jednostki względem osi obrotu. Ciemny kontur obejmował
jedynie górną część. Wszystkie stopnie nowej sylwetki są symetryczne
względem osi X = 0. Nieprzezroczysty czarny kontur otacza całą sylwetkę,
włącznie z dolnym czubkiem. Dane położenia i kierunku pozostają bez zmian.

`build`: sukces. 128 istniejących testów przechodzi, bez błędów i pominięć.
Sprawdzono 16 widoków w rzeczywistym rendererze Minecrafta: osiem kierunków
co 45 stopni, każdy przy skalach GUI 2 i 4. Użyto istniejącego `TerrainPanel`
z danymi podglądu, bez połączenia z serwerem; jest to inspekcja wizualna.
Środowisko: Linux/Mesa/Xvfb, 1600×900. Tymczasowy sterownik podglądu
pozostaje poza repozytorium i wydaniem. Nie ponawiano testu dwóch graczy,
ponieważ poprawka dotyczy wyłącznie rysowania lokalnego znacznika.

CRC archiwum oraz 15 zagnieżdżonych bibliotek poprawne. Brak klas podglądu
i automatyzacji w JAR-ze. Protokół sieciowy pozostaje 16.

Plik: `crimeandlaw-0.1.28.jar`, 1917245 bajtów.

SHA-256: `972bc7f309a4ffd104177165e88900571fb51d1b7461df4ec0c5c36f67ef5ebf`.
