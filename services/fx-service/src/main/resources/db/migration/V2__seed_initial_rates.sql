-- Initial market snapshot (data, not code). The provider moves it from here.
INSERT INTO fx_rates (base_currency, quote_currency, rate_micros, as_of, source) VALUES
    ('ILS', 'USD',  270270, TIMESTAMP WITH TIME ZONE '2026-01-01 00:00:00+00', 'seed'),
    ('USD', 'ILS', 3700000, TIMESTAMP WITH TIME ZONE '2026-01-01 00:00:00+00', 'seed');
