drop table sales_data if exists;
create table sales_data (id bigint generated by default as identity, agency_code varchar(40),amount decimal(10,2), primary key (id));
