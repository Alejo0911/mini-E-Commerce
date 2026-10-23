CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(255) NOT NULL DEFAULT 'ROLE_CUSTOMER',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE products (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    price NUMERIC(12, 2) NOT NULL CHECK (price >= 0),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE inventories (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    stock_available INT NOT NULL CHECK(stock_available >= 0),
    stock_reserved INT NOT NULL DEFAULT 0 CHECK(stock_reserved >= 0),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_inventory_product UNIQUE (product_id)
);

CREATE TABLE orders (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING_PAYMENT',
    total_amount NUMERIC(12, 2) NOT NULL CHECK(total_amount >= 0),
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP 
);

CREATE INDEX idx_orders_status_expires ON orders(status, expires_at);

CREATE TABLE order_items (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL REFERENCES orders(id) ON DELETE CASCADE, 
    product_id BIGINT NOT NULL REFERENCES products(id),
    quantity INT NOT NULL CHECK(quantity > 0),
    unit_price NUMERIC(12, 2) NOT NULL CHECK(unit_price >= 0)
);