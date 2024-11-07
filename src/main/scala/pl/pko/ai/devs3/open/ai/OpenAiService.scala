package pl.pko.ai.devs3.open.ai

object OpenAiService {

  /**
   * =========
   * curl https://api.openai.com/v1/chat/completions
   * -H "Content-Type: application/json"
   * -H "Authorization: Bearer $OPENAI_API_KEY"
   * -d '{
   * "model": "gpt-4o",
   * "messages": [
   * {"role": "user", "content": "write a haiku about ai"}
   * ]
   * }'
   * =========
   * {
   * model: "gpt-4o-mini",
   * messages: [
   * { role: "system", content: "You are a helpful assistant." },
   * {
   * role: "user",
   * content: "Write a haiku about recursion in programming.",
   * },
   * ],
   * }
   * =========
   * {
   * "choices": [
   * {
   * "finish_reason": "length",
   * "index": 0,
   * "logprobs": null,
   * "text": "\n\n\"Let Your Sweet Tooth Run Wild at Our Creamy Ice Cream Shack"
   * }
   * ],
   * "created": 1683130927,
   * "id": "cmpl-7C9Wxi9Du4j1lQjdjhxBlO22M61LD",
   * "model": "gpt-3.5-turbo-instruct",
   * "object": "text_completion",
   * "usage": {
   * "completion_tokens": 16,
   * "prompt_tokens": 10,
   * "total_tokens": 26
   * }
   * }
   * =========
   */
}
