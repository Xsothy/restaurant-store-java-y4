-- Sample Categories
INSERT INTO categories (id, name, description, created_at, updated_at) VALUES
(1, 'Appetizers', 'Start your meal with our delicious appetizers', datetime('now'), datetime('now')),
(2, 'Main Courses', 'Hearty main dishes to satisfy your appetite', datetime('now'), datetime('now')),
(3, 'Desserts', 'Sweet treats to end your meal perfectly', datetime('now'), datetime('now')),
(4, 'Beverages', 'Refreshing drinks and beverages', datetime('now'), datetime('now')),
(5, 'Salads', 'Fresh and healthy salad options', datetime('now'), datetime('now'));

-- Sample Products
INSERT INTO products (id, name, description, price, image_url, is_available, category_id, created_at, updated_at) VALUES
-- Appetizers
(1, 'Spring Rolls', 'Crispy spring rolls with fresh vegetables', 8.99, 'https://example.com/spring-rolls.jpg', true, 1, datetime('now'), datetime('now')),
(2, 'Chicken Wings', 'Spicy buffalo chicken wings with ranch dip', 12.99, 'https://example.com/chicken-wings.jpg', true, 1, datetime('now'), datetime('now')),
(3, 'Mozzarella Sticks', 'Golden fried mozzarella with marinara sauce', 9.99, 'https://example.com/mozzarella-sticks.jpg', true, 1, datetime('now'), datetime('now')),

-- Main Courses
(4, 'Grilled Salmon', 'Fresh Atlantic salmon with lemon herb butter', 24.99, 'https://example.com/grilled-salmon.jpg', true, 2, datetime('now'), datetime('now')),
(5, 'Beef Burger', 'Juicy beef burger with lettuce, tomato, and fries', 16.99, 'https://example.com/beef-burger.jpg', true, 2, datetime('now'), datetime('now')),
(6, 'Chicken Pasta', 'Creamy alfredo pasta with grilled chicken', 18.99, 'https://example.com/chicken-pasta.jpg', true, 2, datetime('now'), datetime('now')),
(7, 'Vegetarian Pizza', 'Wood-fired pizza with fresh vegetables', 19.99, 'https://example.com/veggie-pizza.jpg', true, 2, datetime('now'), datetime('now')),

-- Desserts
(8, 'Chocolate Cake', 'Rich chocolate cake with vanilla ice cream', 7.99, 'https://example.com/chocolate-cake.jpg', true, 3, datetime('now'), datetime('now')),
(9, 'Tiramisu', 'Classic Italian tiramisu with coffee flavor', 8.99, 'https://example.com/tiramisu.jpg', true, 3, datetime('now'), datetime('now')),

-- Beverages
(10, 'Fresh Orange Juice', 'Freshly squeezed orange juice', 4.99, 'https://example.com/orange-juice.jpg', true, 4, datetime('now'), datetime('now')),
(11, 'Iced Coffee', 'Cold brew coffee with milk and sugar', 3.99, 'https://example.com/iced-coffee.jpg', true, 4, datetime('now'), datetime('now')),
(12, 'Soft Drink', 'Choice of Coca-Cola, Pepsi, or Sprite', 2.99, 'https://example.com/soft-drink.jpg', true, 4, datetime('now'), datetime('now')),

-- Salads
(13, 'Caesar Salad', 'Classic Caesar salad with croutons and parmesan', 11.99, 'https://example.com/caesar-salad.jpg', true, 5, datetime('now'), datetime('now')),
(14, 'Greek Salad', 'Fresh Mediterranean salad with feta cheese', 12.99, 'https://example.com/greek-salad.jpg', true, 5, datetime('now'), datetime('now'));

-- Sample Customer
INSERT INTO customers (id, name, email, phone, password_hash, address, created_at, updated_at) VALUES
(1, 'John Doe', 'john.doe@example.com', '+1234567890', '$2a$10$N9qo8uLOickgx2ZMRZoMye1234567890123456789012345678901234567890', '123 Main Street, City, State 12345', datetime('now'), datetime('now')),
(2, 'Jane Smith', 'jane.smith@example.com', '+1987654321', '$2a$10$N9qo8uLOickgx2ZMRZoMye0987654321098765432109876543210987654321', '456 Oak Avenue, City, State 67890', datetime('now'), datetime('now'));

-- Sample Orders
INSERT INTO orders (id, customer_id, status, total_price, order_type, delivery_address, phone_number, special_instructions, created_at, updated_at, estimated_delivery_time) VALUES
(1, 1, 'DELIVERED', 45.97, 'DELIVERY', '123 Main Street, City, State 12345', '+1234567890', 'Please ring the doorbell', datetime('now', '-2 days'), datetime('now', '-1 day'), datetime('now', '-1 day', '+30 minutes')),
(2, 2, 'PREPARING', 28.98, 'DELIVERY', '456 Oak Avenue, City, State 67890', '+1987654321', 'Leave at door if no answer', datetime('now', '-1 hour'), datetime('now', '-30 minutes'), datetime('now', '+45 minutes'));

-- Sample Order Items
INSERT INTO order_items (id, order_id, product_id, quantity, price, special_instructions) VALUES
(1, 1, 1, 2, 8.99, null),
(2, 1, 5, 1, 16.99, 'Medium rare'),
(3, 1, 10, 2, 4.99, null),
(4, 2, 4, 1, 24.99, 'No lemon'),
(5, 2, 12, 1, 2.99, 'Extra ice');

-- Sample Payments
INSERT INTO payments (id, order_id, amount, status, method, transaction_id, created_at, updated_at, paid_at) VALUES
(1, 1, 45.97, 'COMPLETED', 'CREDIT_CARD', 'txn_1234567890', datetime('now', '-2 days'), datetime('now', '-1 day'), datetime('now', '-1 day')),
(2, 2, 28.98, 'PENDING', 'CREDIT_CARD', 'txn_0987654321', datetime('now', '-1 hour'), datetime('now', '-30 minutes'), null);

-- Sample Deliveries
INSERT INTO deliveries (id, order_id, driver_name, driver_phone, vehicle_info, status, pickup_time, estimated_arrival_time, current_location, created_at, updated_at, actual_delivery_time) VALUES
(1, 1, 'Mike Johnson', '+1555123456', 'Honda Civic - Red', 'DELIVERED', datetime('now', '-1 day', '+15 minutes'), datetime('now', '-1 day', '+45 minutes'), '123 Main Street, City, State 12345', datetime('now', '-1 day'), datetime('now', '-1 day'), datetime('now', '-1 day', '+40 minutes')),
(2, 2, 'Sarah Davis', '+1555654321', 'Toyota Prius - Blue', 'PICKED_UP', datetime('now', '-20 minutes'), datetime('now', '+25 minutes'), 'En route to delivery address', datetime('now', '-30 minutes'), datetime('now', '-5 minutes'), null);