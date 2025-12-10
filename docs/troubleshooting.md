# Troubleshooting

## Build / Env

- Mill not executable: `chmod +x mill`
- Wrong Java: use JDK 17+ (`java -version`)
- Slow import (IDE): run `./mill -w __.compile` to warm caches

## boogieloops.web

- 400 errors: check `Content-Type: application/json` and request body JSON
- Port conflict: default example uses `8082` (override `port` or stop other process)
- Status codes: GET/PUT/PATCH/DELETE status chosen from first success response in `responses`

## ChezWiz

- Missing keys: set `OPENAI_API_KEY` / `ANTHROPIC_API_KEY` or use a local OpenAI‑compatible endpoint
- Local LLM: verify endpoint (`http://localhost:1234/v1` etc.) and model ID; disable strict validation if needed
- Structured output failures: tighten schema or lower temperature
