import { createClient, createConfig } from './client/client';

const BASE_URL = process.env.BASE_URL || 'http://0.0.0.0:8082';

const client = createClient(
  createConfig({ baseUrl: BASE_URL, responseStyle: 'data', throwOnError: true })
);

const json = (obj: any) => JSON.stringify(obj, null, 2);

async function run() {
  console.log(`SDK demo using API at ${BASE_URL}`);

  const health = await client.get({ url: '/health' });
  console.log('Health:', json(health));

  const user = await client.post({
    url: '/users',
    headers: new Headers({ 'Content-Type': 'application/json' }),
    body: { name: 'John Doe', email: `john.${Date.now()}@example.com`, age: 30, isActive: true },
    bodySerializer: JSON.stringify,
  });
  console.log('Created user:', json(user));

  const users = await client.get({ url: '/users' });
  console.log('Users:', json(users));

  const userId = user?.id || '1';
  const got = await client.get({ url: `/users/${userId}` });
  console.log('Get user:', json(got));

  const updated = await client.put({
    url: `/users/${userId}`,
    headers: new Headers({ 'Content-Type': 'application/json' }),
    body: { age: 31 },
    bodySerializer: JSON.stringify,
  });
  console.log('Updated user:', json(updated));

  const deleted = await client.delete({ url: `/users/${userId}` });
  console.log('Deleted:', json(deleted));
}

run().catch((err) => {
  console.error('Demo failed:', err);
  process.exit(1);
});

