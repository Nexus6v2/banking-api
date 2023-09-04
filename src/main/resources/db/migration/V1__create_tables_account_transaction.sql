CREATE TABLE account (
     id SERIAL PRIMARY KEY,
     balance DECIMAL NOT NULL,
     version INT NOT NULL DEFAULT 0
);

CREATE TABLE transaction (
     id SERIAL PRIMARY KEY,
     amount DECIMAL NOT NULL,
     sender INT NOT NULL REFERENCES account(id),
     recipient INT NOT NULL REFERENCES account(id)
);
