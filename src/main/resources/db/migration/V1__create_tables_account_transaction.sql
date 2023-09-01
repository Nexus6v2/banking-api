CREATE TABLE account (
     id SERIAL PRIMARY KEY,
     balance DECIMAL NOT NULL,
     version INT NOT NULL DEFAULT 0
);

CREATE TABLE transaction (
     id SERIAL PRIMARY KEY,
     amount DECIMAL NOT NULL,
     "from" INT NOT NULL REFERENCES account(id),
     "to" INT NOT NULL REFERENCES account(id)
);
