-- UKHONA PAY demo/reference data only.
-- NEVER run this file against a production database.
-- Production databases must be created and changed only through versioned migrations.

INSERT INTO taxi_associations (id, name) VALUES
(1, 'Mbombela Local Taxi Association (MALTA)'),
(2, 'KaNyamazane Taxi Association (KATA)'),
(3, 'White River Taxi Association (WRTA)'),
(4, 'Ehlanzeni District Taxi Council')
ON CONFLICT (id) DO NOTHING;

INSERT INTO taxi_ranks (id, name, location_name, association_id) VALUES
(1, 'Mbombela Taxi Rank', 'Mbombela CBD', 1),
(2, 'KaNyamazane Taxi Rank', 'KaNyamazane', 2),
(3, 'Sonheuwel Taxi Rank', 'Sonheuwel', 1),
(4, 'Kabokweni Taxi Rank', 'Kabokweni', 1),
(5, 'White River Taxi Rank', 'White River', 3)
ON CONFLICT (id) DO NOTHING;

-- Demo users, wallets, vendors, bank accounts and sample transactions remain
-- intentionally out of the production migration path. Seed them only in a
-- disposable development database when explicitly required.
