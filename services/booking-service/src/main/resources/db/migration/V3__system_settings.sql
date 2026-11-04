-- System configuration owned by ADMIN. Exactly one row (id = 1).
CREATE TABLE system_settings (
    id                   BIGINT       PRIMARY KEY,
    default_currency     VARCHAR(3)   NOT NULL,
    supported_currencies VARCHAR(100) NOT NULL,   -- e.g. USD,ILS
    region_currencies    VARCHAR(500) NOT NULL,   -- e.g. IL=ILS,US=USD
    updated_by           VARCHAR(50)  NOT NULL,
    updated_at           TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT ck_settings_single_row CHECK (id = 1)
);

INSERT INTO system_settings
    (id, default_currency, supported_currencies, region_currencies, updated_by, updated_at)
VALUES
    (1, 'USD', 'USD,ILS', 'IL=ILS,US=USD', 'system', TIMESTAMP WITH TIME ZONE '2026-01-01 00:00:00+00');
