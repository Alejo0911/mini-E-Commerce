-- Migración para añadir soporte de imágenes a la tabla products
ALTER TABLE products ADD COLUMN IF NOT EXISTS image_url VARCHAR(255);

-- Actualizar los registros de prueba existentes con URLs funcionales
UPDATE products SET image_url = 'https://images.unsplash.com/photo-1603302576837-37561b2e2302?w=600&auto=format&fit=crop&q=80' WHERE id = 1;
UPDATE products SET image_url = 'https://images.unsplash.com/photo-1527443224154-c4a3942d3acf?w=600&auto=format&fit=crop&q=80' WHERE id = 2;
UPDATE products SET image_url = 'https://images.unsplash.com/photo-1587829741301-dc798b83add3?w=600&auto=format&fit=crop&q=80' WHERE id = 3;
UPDATE products SET image_url = 'https://images.unsplash.com/photo-1615663245857-ac93bb7c39e7?w=600&auto=format&fit=crop&q=80' WHERE id = 4;
UPDATE products SET image_url = 'https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=600&auto=format&fit=crop&q=80' WHERE id = 5;

