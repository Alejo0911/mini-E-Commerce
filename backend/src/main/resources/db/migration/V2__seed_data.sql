-- Inserción de usuario demostrativo
INSERT INTO users (id, email, password_hash, role)
VALUES (1, 'cliente@demo.com', '$2a$10$wN1d1rYw6X9x9sMv8pG5.Oz3yX6/8ZJ6b6j6zX8xX8xX8xX8xX8xX', 'ROLE_CUSTOMER')
ON CONFLICT (id) DO NOTHING;

-- Inserción de productos de catálogo
INSERT INTO products (id, name, description, price)
VALUES 
    (1, 'Laptop Gaming Legion Pro 7', 'Intel Core i9, 32GB RAM, 1TB SSD, NVIDIA RTX 4080', 2199.99),
    (2, 'Monitor UltraWide 34 Pulgadas 144Hz', 'Panel IPS QHD, 1ms, HDR400, FreeSync Premium', 499.50),
    (3, 'Teclado Mecánico Inalámbrico Custom', 'Switches mecánicos lubricados, retroiluminación RGB, Hot-swap', 129.00),
    (4, 'Mouse Ergonómico Sensor Óptico 26K DPI', 'Ultra liviano 58g, conectividad 2.4GHz + Bluetooth', 79.99),
    (5, 'Auriculares Inalámbricos Hi-Res ANC', 'Cancelación activa de ruido inteligente y 40h de autonomía', 249.00)
ON CONFLICT (id) DO NOTHING;

-- Inserción de inventarios iniciales con control de versiones para bloqueo optimista
INSERT INTO inventories (id, product_id, stock_available, stock_reserved, version)
VALUES 
    (1, 1, 4, 0, 0),
    (2, 2, 6, 0, 0),
    (3, 3, 15, 0, 0),
    (4, 4, 20, 0, 0),
    (5, 5, 10, 0, 0)
ON CONFLICT (product_id) DO NOTHING;

-- Sincronización de secuencias de PostgreSQL
SELECT setval('users_id_seq', (SELECT MAX(id) FROM users));
SELECT setval('products_id_seq', (SELECT MAX(id) FROM products));
SELECT setval('inventories_id_seq', (SELECT MAX(id) FROM inventories));

