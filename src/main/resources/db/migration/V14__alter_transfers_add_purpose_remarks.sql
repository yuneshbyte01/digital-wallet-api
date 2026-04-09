ALTER TABLE transfers ADD COLUMN transaction_code    VARCHAR(30) UNIQUE;
ALTER TABLE transfers ADD COLUMN purpose             VARCHAR(30);
ALTER TABLE transfers RENAME COLUMN note TO remarks;