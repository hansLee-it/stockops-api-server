-- Store-originated purchase requests capture the requested delivery date
-- (client-web 발주 신청의 "희망 입고일"). Nullable: supplier-procurement orders
-- and legacy rows have no expected date.
ALTER TABLE purchase_orders ADD COLUMN expected_date DATE;
