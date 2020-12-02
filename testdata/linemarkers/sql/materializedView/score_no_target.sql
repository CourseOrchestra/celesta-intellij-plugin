CREATE GRAIN demo VERSION '1.0';

create materialized view OrderLine<caret>edQty as
  select item_id, sum(qty) as qty from OrderLine group by item_id;