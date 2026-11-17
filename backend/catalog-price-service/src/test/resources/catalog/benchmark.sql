INSERT INTO stores(id, name) VALUES ('00000000-0000-0000-0000-000000000001', 'Synthetic benchmark store');
INSERT INTO products(id, name, category, calories_per_100g, proteins_per_100g, fats_per_100g, carbs_per_100g)
SELECT md5(i::text)::uuid, 'benchmark product ' || i, 'benchmark', i % 900, i % 100, i % 80, i % 90
FROM generate_series(1, 100000) i;
INSERT INTO prices(product_id, store_id, currency, unit_price_minor, price_basis_milligrams)
SELECT id, '00000000-0000-0000-0000-000000000001', 'RUB', 10000, 1000000 FROM products;
ANALYZE products;
ANALYZE prices;
