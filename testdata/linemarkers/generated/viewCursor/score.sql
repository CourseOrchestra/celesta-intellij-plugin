CREATE GRAIN demo VERSION '1.0';

CREATE TABLE OrderLine(
  id VARCHAR(30) NOT NULL,
  date DATETIME,
  customer_id VARCHAR(30),
  customer_name VARCHAR(50),
  manager_id VARCHAR(30),
  CONSTRAINT Pk_Order PRIMARY KEY (id)
);

create materialized view Order<caret>edQty as
  select item_id, sum(qty) as qty from OrderLine group by item_id;