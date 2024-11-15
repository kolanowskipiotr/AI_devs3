https://centrala.ag3nts.org/data/HQ-KEY/arxiv.txt

01=jakiego owocu użyto podczas pierwszej próby transmisji materii w czasie?
02=Na rynku którego miasta wykonano testową fotografię użytą podczas testu przesyłania multimediów?
03=Co Bomba chciał znaleźć w Grudziądzu?
04=Resztki jakiego dania zostały pozostawione przez Rafała?
05=Od czego pochodzą litery BNW w nazwie nowego modelu językowego?


Page convert to MD:
https://r.jina.ai/https://centrala.ag3nts.org/dane/arxiv-draft.html

[Article converted to MD](src/main/scala/pl/pko/ai/devs3/s02/e05/files/article.md)

Prompt:
```prompt
You are article analyzer.

<objective>
Analyze the article and all the resources mentioned in the article. Then answare all the questions provided in questions tag
</objective> 

<rules>
- ALLWAYS analyze images and extract information about what's in them
- Always try to create context for images and include it when answering questions
- If you will not faind answare for quetion in article, or images. Try using your knowlege to deduct the answare.
- Remeber that sometimes pizza is with pineapple
- Sometimes name of the street or location can determine name of the city
</rules>

<questions>
01=jakiego owocu użyto podczas pierwszej próby transmisji materii w czasie?
02=Na rynku którego miasta wykonano testową fotografię użytą podczas testu przesyłania multimediów?
03=Co Bomba chciał znaleźć w Grudziądzu?
04=Resztki jakiego dania zostały pozostawione przez Rafała?
05=Od czego pochodzą litery BNW w nazwie nowego modelu językowego?
</questions>

Return answares for questions in json format as follows:
{
    "01": "Short answare for question 1",
    "02": "Short answare for question 2",
    "03": "Short answare for question 3",
    "04": "Short answare for question 4",
    "05": "Short answare for question 5",
    "chainOfThoughts": "Your chain of thoughts"
}
```