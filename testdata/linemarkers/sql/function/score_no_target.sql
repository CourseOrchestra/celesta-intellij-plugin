CREATE GRAIN demo VERSION '1.0';

create function fu<caret>nc(p int) as select order_id from OrderLine where  line_no = $p;