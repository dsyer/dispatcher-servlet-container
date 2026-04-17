-- Create tables

CREATE TABLE fruits (
		id bigint NOT NULL,
		description VARCHAR(255),
		name VARCHAR(255) NOT NULL UNIQUE,
		PRIMARY KEY (id)
);

CREATE TABLE store_fruit_prices (
		price numeric(12,2) NOT NULL,
		fruit_id bigint NOT NULL,
		store_id bigint NOT NULL,
		PRIMARY KEY (fruit_id, store_id)
);

CREATE TABLE stores (
		id bigint NOT NULL,
		ADDress VARCHAR(255) NOT NULL,
		city VARCHAR(255) NOT NULL,
		country VARCHAR(255) NOT NULL,
		currency VARCHAR(255) NOT NULL,
		name VARCHAR(255) NOT NULL UNIQUE,
		PRIMARY KEY (id)
);

ALTER TABLE IF exists store_fruit_prices
	 ADD CONSTRAINT fruit_id_fk
	 FOREIGN KEY (fruit_id)
	 REFERENCES fruits;

ALTER TABLE IF exists store_fruit_prices
	 ADD CONSTRAINT store_id_fk
	 FOREIGN KEY (store_id)
	 REFERENCES stores;

CREATE SEQUENCE fruits_seq INCREMENT BY 1;
CREATE SEQUENCE stores_seq INCREMENT BY 1;
