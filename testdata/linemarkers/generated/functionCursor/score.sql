CREATE GRAIN demo VERSION '1.0';

create function fun<caret>c(p int) as select order_id from OrderLine where  line_no = $p;