CREATE GRAIN demo VERSION '1.0';

CREATE TABLE Ord<caret>er(
  id VARCHAR(30) NOT NULL,
  date DATETIME,
  customer_id VARCHAR(30),
  customer_name VARCHAR(50),
  manager_id VARCHAR(30),
  CONSTRAINT Pk_Order PRIMARY KEY (id)
);