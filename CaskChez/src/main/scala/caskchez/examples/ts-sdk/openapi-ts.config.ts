import { defineConfig } from '@hey-api/openapi-ts';

export default defineConfig({
  input: 'http://0.0.0.0:8082/openapi',
  output: 'src/client',
});
