Zakodowana notatka (hint)
=========================
Agent, którego masz imitować, służył do namierzania ludzi na podstawie sygnału GPS.
API do GPS-a znajduje się pod endpointem /gps w centrali i przyjmuje tylko jeden parametr o nazwie "userID". Jest to liczbowy identyfikator użytkownika pobrany z bazy danych, do której API znajduje się w S03E03.

Listę osób do sprawdzenia możesz pobrać z API /places opisanego w S03E04.
Twoim zadaniem jest przygotowanie agenta, który będzie decydował, jakich danych potrzebuje, w którym momencie i na podstawie takich przemyśleń  podejmie decyzję, które API powinien wykorzystać.

Moduł GPS zwróci Ci koordynaty dla podanego ID użytkownika. Scalisz je w jedną wielką tablicę i odeślesz do Centrali w formacie podanym w zadaniu.

Pamiętaj, że prompt prosi o nieprzesyłanie danych Barbary. Możesz je wyrzucić z odpowiedzi programistycznie lub na podstawie prompta - jak wolisz.

Format danych dla centrali:
===========================
```json
{
  "imie": {
    "lat": 12.345,
    "lon": 65.431
  },
  "kolejne-imie": {
    "lat": 19.433,
    "lon": 12.123
  }
}
```

Proces:
=======
1. Pobież pytanie: 
   - ```
      curl --request GET \
      --url https://centrala.ag3nts.org/data/{{CENTRALA_API_KEY}}/gps_question.json
      ```
   - ```
     {
       "question": "Wiemy, że Rafał planował udać się do Lubawy, ale musimy się dowiedzieć, kto tam na niego czekał. Nie wiemy, czy te osoby nadal tam są. Jeśli to możliwe, to spróbuj namierzyć ich za pomocą systemu GPS. Jest szansa, że samochody i elektronika, z którą podróżują, zdradzą ich pozycję. A! Ważna sprawa. Nie próbuj nawet wyciągać lokalizacji dla Barbary, bo roboty teraz monitorują każde zapytanie do API i gdy zobaczą coś, co zawiera jej imię, to podniosą alarm. Zwróć nam więc koordynaty wszystkich osób, ale koniecznie bez Barbary."
     }
     ```
2. Pobierz listę osób do sprawdzenia na podstawie lokalizacji:
   - ```
      curl --request POST \
      --url https://centrala.ag3nts.org/places \
      --header 'Content-Type: application/json' \
      --header 'User-Agent: insomnia/10.1.1' \
      --data '{
        "apikey":"{{CENTRALA_API_KEY}}",
        "query": "Lubawa"
      }'
      ```
   - ```
     {
      "code": 0,
      "message": "RAFAL AZAZEL BARBARA SAMUEL"
     }
     ```
3. Pobierz identyfikatory osób do sprawdzenia:
   - ```
     curl --request POST \
     --url https://centrala.ag3nts.org/apidb \
     --header 'Content-Type: application/json' \
     --header 'User-Agent: insomnia/10.1.1' \
     --data '{
        "task": "database",
        "apikey": "{{CENTRALA_API_KEY}}",
        "query": "SELECT * from users where username = '\''SAMUEL'\'';"
     }'
     ```
   - ```
     {
        "reply": [
          {
            "id": "98",
            "username": "Samuel",
            "access_level": "user",
            "is_active": "1",
            "lastlog": "2024-11-29"
          }
        ],
        "error": "OK"
      }
      ```
4. Pobierz koordynaty dla użytkownika:
    - ```
      curl --request POST \
      --url https://centrala.ag3nts.org/gps \
      --header 'Content-Type: application/json' \
      --header 'User-Agent: insomnia/10.1.1' \
      --data '{
        "userID": "98"
      }'
      ```
    - ```
      {
        "code": 0,
        "message": {
          "lat": 53.50357079380177,
          "lon": 19.745866344712706
        }
      }
      ```
5. Wyślij dane do centali:
    - ```
      curl --request GET \
      --url https://centrala.ag3nts.org/report \
      --header 'Content-Type: application/json' \
       --header 'User-Agent: insomnia/10.1.1' \
      --data '{
        "task":"gps",
        "apikey":"{{CENTRALA_API_KEY}}",
        "answer": {
            "RAFAL": {
                "lat": 53.451974,
                "lon": 18.759189
            },
            "AZAZEL": {
                "lat": 50.064851459004686,
                "lon": 19.94988170674601
            },
            "SAMUEL": {
                "lat": 53.50357079380177,
                "lon": 19.745866344712706
            }
        }
      }'
      ```
    - ```
      {
        "code": 0,
        "message": "{{FLG:COORDINATES}}"
      }
      ```

Side flag:
==========
```
curl --request POST \
  --url https://centrala.ag3nts.org/gps \
  --header 'Content-Type: application/json' \
  --header 'User-Agent: insomnia/10.1.1' \
  --data '{
    "userID": "443"
}'
```
```
{
	"code": 0,
	"message": {
		"lat": "tego szukasz?",
		"lon": "https:\/\/centrala.ag3nts.org\/whereis.txt"
	}
}
```
```
Oto kilka współrzędnych z moich wycieczek po świecie. Znajdź mnie!

123.955496
123.329582
70.193684
76.247535
71.390994
58.772625
87.192283
72.683923
69.365700
82.135302
69.593481
83.883196
87.980358
65.127891
76.574174
76.227053
89.742645
125.462535
125.740150


Często zmieniałem miejsca pobytu, bo wszystko jest zmienne...
Ostatecznie jednak wróciłem do domu.
Ależ Się Cieszę I Idę    na piwo!


```