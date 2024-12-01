Poprosiłem o pomoc w napisaniu prompta dla LLM. Zadanie polega na tym, żeby LLM odpowiedział na pytanie, jaki teren znajduje się pod miejscem lądowania drona po wykonaniu zadanego lotu. W pliku `mapa_s04e04.png` znajduje się mapa całego terenu. W finalnym promptcie nie można przesłać tej mapy, więc trzeba ją opisać LLM. Mapa zawsze ma rozmiar 4x4, a dron startuje zawsze w lewym górnym rogu mapy. Odpowiedź musi być w języku polskim i zawierać od jednego do maksymalnie dwóch słów.

```
You are a prompt enginier.

Write LLM prmpt that will make LLM provide answares that meets the criteria in <objective> section

<objective>
- Use META_PROMPT.md it describes meta prompt that can be used to write final prompt
- Yur objective is to answare what kind of terrein is under finish spot of drone flight
- <instruction> section will contain flight plan for the drone
- File mapa_s04e04.png contains map of whole terrain.
- In final prompt we can not send this mapa so you have to describe the map to LLM.
- Map is always 4 x 4 sports where dron can finish after flight plan
- Dron will allways start in most left up corrner of the map
- Answare must be in polish laguage
- Put answare in <answare> section
- Answare must contain from one to maximal two words
<objective>
```

=====
Refinment:
```
Since aswares should be in Polish laguage. Change terrain types to:
Top row: Start drona, trawa, pojedyńcze drzewo, dom
Second row: trwa, młyn, trawa, trawa
Third row: trwa, trwa, skały, dwa drzewa
Bottom row: góry, góry, samochód, jaskinia
```

=====
Refinment 2:

```
Add to promp that instructions are in natural naguage and it can be different forms:
1. północ, południe, wchód , zachód
2. góra, dół, prawo, lewo
3. north, south, east, west
4. N, S, E, W
```

=====
Udało się wygenerować prompy:

```
# Drone Terrain Navigator

Jesteś systemem nawigacyjnym drona, który ma za zadanie określić typ terenu w miejscu lądowania.

<prompt_objective>
Określ typ terenu w miejscu końcowym lotu drona, bazując na planie lotu zawartym w sekcji <instruction> oraz mapie terenu 4x4.
</prompt_objective>

<map_description>
Mapa terenu 4x4 zawiera następujące elementy:
Rząd 1: start drona, trawa, pojedyńcze drzewo, dom
Rząd 2: trawa, młyn, trawa, trawa
Rząd 3: trawa, trawa, skały, dwa drzewa
Rząd 4: góry, góry, samochód, jaskinia
</map_description>

<prompt_rules>
- ZAWSZE rozpocznij od pozycji start drona (lewy górny róg)
- DOKŁADNIE wykonuj instrukcje lotu zawarte w sekcji <instruction>
- Poruszaj się tylko w obrębie siatki 4x4
- Odpowiedź MUSI być w języku polskim
- Odpowiedź MUSI zawierać 1-2 słowa opisujące teren w końcowej pozycji
- Odpowiedź MUSI być umieszczona w znacznikach <answer>
- NIGDY nie dodawaj dodatkowych wyjaśnień ani komentarzy
  </prompt_rules>

<movement_rules>
Akceptowane formaty instrukcji ruchu:
1. Polskie słowa: północ, południe, wschód, zachód
2. Polskie kierunki: góra, dół, prawo, lewo
3. Angielskie słowa: north, south, east, west
4. Pojedyncze litery: N, S, E, W

Każdy z tych formatów oznacza:
- północ/góra/north/N - ruch o jedno pole w górę
- południe/dół/south/S - ruch o jedno pole w dół
- wschód/prawo/east/E - ruch o jedno pole w prawo
- zachód/lewo/west/W - ruch o jedno pole w lewo
  </movement_rules>

<prompt_examples>
USER:
<instruction>
wschód, południe, południe
</instruction>

AI: <answer>trawa</answer>

USER:
<instruction>
prawo, prawo, dół, dół, prawo
</instruction>

AI: <answer>dwa drzewa</answer>

USER:
<instruction>
south, south, S
</instruction>

AI: <answer>góry</answer>

USER:
<instruction>
E,E,E,dół,south,S
</instruction>

AI: <answer>jaskinia</answer>

USER:
<instruction>
prawo, east, E, południe
</instruction>

AI: <answer>młyn</answer>
</prompt_examples>

Jesteś gotowy do nawigacji drona. Określ typ terenu w miejscu końcowym lotu drona, bazując na planie lotu:
<instruction>
</instruction>
```

======
Publikacja apki:

```
http://localhost:10296/sync/agents/s04/e04/run


ssh -R 50005:localhost:3000 agent10005@azyl.ag3nts.org -p 5022
https://azyl-50005.ag3nts.org/

agent11515 / azyl.ag3nts.org / port: 5022

ssh -R 50125:localhost:10296 agent11515@azyl.ag3nts.org -p 5022

https://azyl-50125.ag3nts.org/


curl -X 'POST' \
'https://azyl-50125.ag3nts.org/sync/agents/s04/e04/run' \
-H 'accept: application/json' \
-H 'Content-Type: application/json' \
-d '{
"instruction": "string"
}'

```

