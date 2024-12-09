Hint
====
- pliki JSON z pytaniami zmieniają się cyklicznie, więc ich cachowanie "raczej" nie ma sensu, a przynajmniej warto tego nie robić, aby nie zaliczać ogromnej liczby errorów.
- dane źródłowe mają zaimplementowany spowalniacz, który sprawia, że ściągają się 2-3 sekundy. Dwa pliki dają 4-6 sekund na samo pobranie danych. Zostaje za mało czasu na zapytanie o cokolwiek nawet najszybszego LLM-a
- warto pomyśleć o współbieżnym wykonywaniu zadań (wątki)
- istnieje szansa, że nawet zrównoleglenie zapytań nadal nie da Ci oczekiwanej prędkości wykonania zadania. Wtedy warto zoptymalizować swojego prompta
- może zamiana modelu na szybszy da Ci dodatkowe kilkaset milisekund cennego czasu?
- długość udzielanej odpowiedzi wpływa bezpośrednio na czas jej generowania. Czy naprawdę musisz pisać elaborat, aby automat zauważył, że w przesyłanych danych jest np. data bitwy pod Grunwaldem?
- przypomnij sobie, czym jest "prompt caching". Może dzięki temu zyskasz te 100-200ms per prompt? Co i kiedy może być cachowane?
- odpowiedź na zadanie musi być wysłana po polsku, ale kto powiedział, że prompt musi być w tym języku? Może to także da Ci dodatkowy czas?
- kombinuj