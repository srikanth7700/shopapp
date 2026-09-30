-- Demo catalog. Inserted in this order so the ids are 1..12 on a fresh database;
-- inventory-service seeds stock for the same ids.
INSERT INTO products (sku, name, description, category, price) VALUES
('SS-ELEC-001', 'Wireless Noise-Cancelling Headphones', 'Over-ear headphones with active noise cancelling and a 30-hour battery.', 'Electronics', 199.99),
('SS-ELEC-002', '27-inch 4K Monitor', 'IPS panel, USB-C with 65W charging, height-adjustable stand.', 'Electronics', 329.00),
('SS-ELEC-003', 'Pro Creator Laptop 16', 'Top-spec workstation laptop. Priced above the $5,000 demo payment limit: order it to watch a payment get declined and the saga roll back.', 'Electronics', 5499.00),
('SS-ELEC-004', 'Limited Edition Mechanical Keyboard', 'Hot-swappable switches, aluminium case. Only 2 in stock: order 3 to watch inventory reject the order.', 'Electronics', 149.50),
('SS-HOME-001', 'Stainless Steel French Press', 'Double-wall insulated, 1 litre.', 'Home & Kitchen', 34.95),
('SS-HOME-002', 'Cast Iron Skillet 12-inch', 'Pre-seasoned, oven safe, lasts a lifetime.', 'Home & Kitchen', 44.00),
('SS-HOME-003', 'Smart LED Desk Lamp', 'Adjustable colour temperature with a USB charging port.', 'Home & Kitchen', 59.99),
('SS-BOOK-001', 'Microservices in Practice', 'Patterns for splitting a monolith: sagas, outbox, API gateways.', 'Books', 42.00),
('SS-BOOK-002', 'The Event-Driven Handbook', 'Kafka from first principles to production.', 'Books', 38.50),
('SS-SPRT-001', 'Pro Yoga Mat', 'Non-slip, 6 mm thick, with carry strap.', 'Sports', 39.00),
('SS-SPRT-002', 'Adjustable Dumbbell Set', 'Quick-change weights from 5 to 52.5 lb per dumbbell.', 'Sports', 249.00),
('SS-FASH-001', 'Waterproof Trail Jacket', 'Breathable shell with sealed seams and packable hood.', 'Fashion', 129.00);
