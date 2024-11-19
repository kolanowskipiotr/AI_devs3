https://jina.ai/embeddings/
https://{{ApiKeys.qdrantApiUrl}}/dashboard#/tutorial/quickstart

# Twożenie kolekcji
Można użyć konsoli: {{ApiKeys.qdrantApiUrl}}/dashboard#/console
```http request
PUT collections/vector_store_example
{
  "vectors": {
    "size": 1024,
    "distance": "Dot"
  }
}
```