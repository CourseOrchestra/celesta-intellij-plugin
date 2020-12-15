CREATE GRAIN demo VERSION '1.0';

create function ffu<caret>nc(p int) as select order_id from OrderLine where  line_no = $p;