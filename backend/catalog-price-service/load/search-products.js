import grpc from 'k6/net/grpc';
import { check } from 'k6';
import { Rate } from 'k6/metrics';

const client = new grpc.Client();
client.load(['../../../proto'], 'nutritionist/catalog/v1/catalog.proto');
const errors = new Rate('catalog_search_errors');

export const options = {
  scenarios: {
    search_smoke: {
      executor: 'shared-iterations',
      vus: 5,
      iterations: 100,
      maxDuration: '30s',
    },
  },
  thresholds: {
    checks: ['rate==1'],
    catalog_search_errors: ['rate==0'],
    grpc_req_duration: ['p(95)<200'],
  },
};

const metadata = __ENV.CATALOG_TOKEN
  ? { authorization: 'Bearer ' + __ENV.CATALOG_TOKEN }
  : __ENV.ALLOW_TRUSTED_LOCAL === 'true'
    ? { 'x-user-id': 'catalog-load-smoke', 'x-user-roles': 'user' }
    : null;

export default function () {
  if (!metadata) throw new Error('Set CATALOG_TOKEN, or explicitly enable ALLOW_TRUSTED_LOCAL for loopback testing');
  if (__ITER === 0) {
    client.connect(__ENV.CATALOG_TARGET || '127.0.0.1:9192', {
      plaintext: __ENV.PLAINTEXT === 'true',
      timeout: '5s',
    });
  }
  const request = { pageSize: 2, currency: 'RUB' };
  if (__ENV.CATEGORY) request.category = __ENV.CATEGORY;
  if (__ENV.QUERY) request.query = __ENV.QUERY;
  const first = client.invoke('com.budgetntr.catalog.v1.CatalogService/SearchProducts', request, { metadata, timeout: '2s' });
  const valid = check(first, {
    'SearchProducts OK': (response) => response.status === grpc.StatusOK,
    'seed products returned': (response) => response.message && response.message.products && response.message.products.length > 0,
  });
  errors.add(!valid);
  const token = first.message && first.message.nextPageToken;
  if (token) {
    const second = client.invoke(
      'com.budgetntr.catalog.v1.CatalogService/SearchProducts',
      Object.assign({}, request, { pageToken: token }),
      { metadata, timeout: '2s' },
    );
    const ids = (first.message.products || []).map((product) => product.productId);
    const nextValid = check(second, {
      'next page OK': (response) => response.status === grpc.StatusOK,
      'no page duplicates': (response) => response.message && (response.message.products || []).every((product) => !ids.includes(product.productId)),
    });
    errors.add(!nextValid);
  }
}
