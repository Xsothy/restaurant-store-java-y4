-- Khmer Food Categories
INSERT INTO "categories" ("id", "name", "description", "created_at", "updated_at") VALUES
(1, 'ប្រភេទបុកល្ហុង', 'Boklahong - Khmer foods', datetime('now'), datetime('now')),
(2, 'ប្រភេទភ្លា', 'Flash - Khmer foods', datetime('now'), datetime('now')),
(3, 'ប្រភេទបំពង', 'Fried - Khmer food', datetime('now'), datetime('now')),
(4, 'ប្រភេទបាយឆា', 'Fried rice - Khmer foods', datetime('now'), datetime('now')),
(5, 'ប្រភេទឆាក្តៅ', 'Stir-fried - Khmer Foods', datetime('now'), datetime('now'));

-- Khmer Food Products
INSERT INTO "products" ("id", "name", "description", "price", "image_url", "is_available", "category_id", "created_at", "updated_at") VALUES
-- Boklahong Category
(1, 'បុកល្ហុងប៉ាតេ', 'Brother Long Pate - Khmer foods', 1.25, 'https://cqodncaubuytsrskeuxy.supabase.co/storage/v1/object/public/khmer-food/products/0ffd35b2-9780-4fba-bf88-fbd3c8080504.jpg', true, 1, datetime('now'), datetime('now')),
(2, 'បុកល្ហុងជើងមាន់', 'Chicken feed papaya - Khmer foods', 1.75, 'https://cqodncaubuytsrskeuxy.supabase.co/storage/v1/object/public/khmer-food/products/449f1c30-0087-49f4-a505-9b6101788760.jpg', true, 1, datetime('now'), datetime('now')),
(3, 'បុកមីក្តាមប្រៃ', 'Crush papaya - Khmer food', 1.5, 'https://cqodncaubuytsrskeuxy.supabase.co/storage/v1/object/public/khmer-food/products/b8bd28b9-691d-432b-ba86-b110bf646bc7.jpg', true, 1, datetime('now'), datetime('now')),
(4, 'បុកពោតគ្រឿងសមុទ្រ', 'Crush the corn kernels - Khmer food', 2.5, 'https://cqodncaubuytsrskeuxy.supabase.co/storage/v1/object/public/khmer-food/products/613256c7-05f1-4617-b1b3-8e5f421716a0.jpg', true, 1, datetime('now'), datetime('now')),
(5, 'បុកល្ហុងងាវ', 'Crush the papaya - Khmer food', 2.0, 'https://cqodncaubuytsrskeuxy.supabase.co/storage/v1/object/public/khmer-food/products/d5c34847-9ec2-465d-91d7-d4729f35d67b.jpg', true, 1, datetime('now'), datetime('now')),
(6, 'បុកល្ហុងបបង្គារ', 'Crushed papaya and shrimp - Khmer foods', 2.0, 'https://cqodncaubuytsrskeuxy.supabase.co/storage/v1/object/public/khmer-food/products/0f002c5f-86e2-403d-93d3-eb9940895e9f.jpg', true, 1, datetime('now'), datetime('now')),
(7, 'បុកល្ហុងក្តាមសេះ', 'Horse crab papaya - Khmer foods', 2.0, 'https://cqodncaubuytsrskeuxy.supabase.co/storage/v1/object/public/khmer-food/products/82c40202-e31c-4b72-aef0-743b90cae719.jpg', true, 1, datetime('now'), datetime('now')),
(8, 'បុកល្ហុងគ្រឿងសមុទ្រ', 'Papaya, seafood - Khmer food', 2.5, 'https://cqodncaubuytsrskeuxy.supabase.co/storage/v1/object/public/khmer-food/products/2bae94d9-cd1c-4f91-ba51-5cd1ba496f68.jpg', true, 1, datetime('now'), datetime('now')),
(9, 'បុកល្ហុងក្តាមប្រៃ', 'Salted crab and papaya - Khmer foods', 1.25, 'https://cqodncaubuytsrskeuxy.supabase.co/storage/v1/object/public/khmer-food/products/6209e042-1160-4726-b63c-9c88f1f939ca.jpg', true, 1, datetime('now'), datetime('now')),

-- Flash Category (ភ្លា)
(10, 'ភ្លាសាច់គោ', 'Beef shoulder - Khmer food', 2.5, 'https://cqodncaubuytsrskeuxy.supabase.co/storage/v1/object/public/khmer-food/products/8a0e88e0-4e7e-424b-b9d3-60eba756f5cf.jpg', true, 2, datetime('now'), datetime('now')),
(11, 'ឡាបសាច់គោ', 'Beef stew - Khmer food', 2.5, 'https://cqodncaubuytsrskeuxy.supabase.co/storage/v1/object/public/khmer-food/products/1808dbca-747b-41cb-b4d4-bfdd7eedf93b.jpg', true, 2, datetime('now'), datetime('now')),
(12, 'ញាំស្វាយបង្គារក្រៀម', 'Eat dried shrimp and mango - Khmer food', 2.5, 'https://cqodncaubuytsrskeuxy.supabase.co/storage/v1/object/public/khmer-food/products/7c0819be-67d6-4e3b-9e14-703ee3fdae42.jpg', true, 2, datetime('now'), datetime('now')),
(13, 'ញាំស្វាយជើងមាន់', 'Eat mango and chicken feet - Khmer food', 2.5, 'https://cqodncaubuytsrskeuxy.supabase.co/storage/v1/object/public/khmer-food/products/2a82dd0b-8d4c-4273-8f98-db4ea7b0079e.jpg', true, 2, datetime('now'), datetime('now')),
(14, 'ឡាបត្រី', 'Fish fillet - Khmer food', 2.5, 'https://cqodncaubuytsrskeuxy.supabase.co/storage/v1/object/public/khmer-food/products/72953c3e-82d8-4e48-b2d1-86de196b03b7.jpg', true, 2, datetime('now'), datetime('now')),
(15, 'ភ្លាត្រី', 'Fish fillet - Khmer food', 2.5, 'https://cqodncaubuytsrskeuxy.supabase.co/storage/v1/object/public/khmer-food/products/9d2bbc57-e44d-442d-83eb-cc324e72fff7.jpg', true, 2, datetime('now'), datetime('now')),
(16, 'ភ្លាត្រីជាមួយម្រះ', 'Fish with salt - Khmer food', 2.5, 'https://cqodncaubuytsrskeuxy.supabase.co/storage/v1/object/public/khmer-food/products/4a38f8e2-5ce8-4763-abc8-8618fe081ded.jpg', true, 2, datetime('now'), datetime('now')),
(17, 'ឡាបបង្គារស្រស់', 'Fresh shrimp paste - Khmer food', 3.0, 'https://cqodncaubuytsrskeuxy.supabase.co/storage/v1/object/public/khmer-food/products/67d8e410-71ab-44de-bf59-e6d6bbadf380.jpg', true, 2, datetime('now'), datetime('now')),
(18, 'ភ្លាសាច់គោម្រះ', 'Spicy beef stew - Khmer food', 2.5, 'https://cqodncaubuytsrskeuxy.supabase.co/storage/v1/object/public/khmer-food/products/e35cabbb-3cb5-4847-aa85-2c7e0c1441a0.jpg', true, 2, datetime('now'), datetime('now')),
(19, 'ភ្លាបង្គារជាមួយម្រះ', 'The sea with the sea - Khmer food', 3.0, 'https://cqodncaubuytsrskeuxy.supabase.co/storage/v1/object/public/khmer-food/products/249df69f-3ea6-4b90-8859-25cd48668752.jpg', true, 2, datetime('now'), datetime('now')),

-- Fried Category (បំពង)
(20, 'ចាបបំពង', 'Fried chicken - Khmer food', 3.0, 'https://cqodncaubuytsrskeuxy.supabase.co/storage/v1/object/public/khmer-food/products/617eec63-3576-4a5f-a5ff-3c83ace3bdda.jpg', true, 3, datetime('now'), datetime('now')),
(21, 'រ៉ៃបំពង', 'Fried rice - Khmer food', 2.5, 'https://cqodncaubuytsrskeuxy.supabase.co/storage/v1/object/public/khmer-food/products/1a23d221-e082-4cc0-bf83-14b974897f9c.jpg', true, 3, datetime('now'), datetime('now')),
(22, 'សាច់គោបំពងល្ង', 'Fried beef with sesame seeds - Khmer food', 2.5, 'https://cqodncaubuytsrskeuxy.supabase.co/storage/v1/object/public/khmer-food/products/43434673-8758-4c5f-8e8c-5b23ca8f770c.jpg', true, 3, datetime('now'), datetime('now')),
(23, 'សរសៃកែងមាន់បំពងល្ង', 'Fried beef with sesame seeds - Khmer food', 2.5, 'https://cqodncaubuytsrskeuxy.supabase.co/storage/v1/object/public/khmer-food/products/e0f7cd19-6aee-475c-85d2-16c378fa1422.jpg', true, 3, datetime('now'), datetime('now')),
(24, 'ជើងមាន់បំពងខ្ទឹ​មស', 'Fried chicken feet with white sauce - Khmer food', 2.5, 'https://cqodncaubuytsrskeuxy.supabase.co/storage/v1/object/public/khmer-food/products/c9372b2e-2947-405c-9f0f-68497e60a22b.jpg', true, 3, datetime('now'), datetime('now')),
(25, 'សរសៃកែងមាន់បំពងខ្ទឹមស', 'Fried chicken thighs with garlic - Khmer food', 2.5, 'https://cqodncaubuytsrskeuxy.supabase.co/storage/v1/object/public/khmer-food/products/f61e4ca1-436c-4d6b-8af3-e12ad0945164.jpg', true, 3, datetime('now'), datetime('now')),
(26, 'អន្ទង់បំពងខ្ទឹមស', 'Fried eel with garlic - Khmer food', 3.0, 'https://cqodncaubuytsrskeuxy.supabase.co/storage/v1/object/public/khmer-food/products/f116d7eb-725c-4e09-9220-50229c07f8b9.jpg', true, 3, datetime('now'), datetime('now')),
(27, 'ដុកឌឿបំពង', 'Fried fish - Khmer food', 2.5, 'https://cqodncaubuytsrskeuxy.supabase.co/storage/v1/object/public/khmer-food/products/e999bdbe-00b6-467a-8127-cb333817c689.jpg', true, 3, datetime('now'), datetime('now')),
(28, 'កង្កែបបំពងខ្ទឹ​មស', 'Fried frog with white onion - Khmer food', 2.5, 'https://cqodncaubuytsrskeuxy.supabase.co/storage/v1/object/public/khmer-food/products/434ae122-c234-44ef-b2fa-ed36af9986b1.jpg', true, 3, datetime('now'), datetime('now')),
(29, 'បង្គារបំពងម្សៅ', 'Fried shrimp in flour - Khmer food', 2.5, 'https://cqodncaubuytsrskeuxy.supabase.co/storage/v1/object/public/khmer-food/products/954e3b3d-cfb2-42ee-89cd-8c0252c4ca31.jpg', true, 3, datetime('now'), datetime('now')),

-- Fried Rice Category (បាយឆា)
(30, 'បាយឆាយ៉ាងចូវ', 'Fried rice, Yangchou - Khmer food', 1.25, 'https://cqodncaubuytsrskeuxy.supabase.co/storage/v1/object/public/khmer-food/products/85e846cb-e65f-4a16-bad8-c24e21df9450.jpg', true, 4, datetime('now'), datetime('now')),
(31, 'បាយឆាគ្រឿងសមុទ្រ', 'Seafood fried rice - Khmer food', 1.75, 'https://cqodncaubuytsrskeuxy.supabase.co/storage/v1/object/public/khmer-food/products/24754c12-75e3-4756-959e-6f3e9807faf2.jpg', true, 4, datetime('now'), datetime('now')),

-- Stir-fried Category (ឆាក្តៅ)
(32, 'ឆាឡុកឡាក់សាច់គោ', 'Beef Charoen Lak - Khmer foos', 2.5, 'https://cqodncaubuytsrskeuxy.supabase.co/storage/v1/object/public/khmer-food/products/e76ee3b4-0377-46b9-ab6a-4fa6100c3ad6.jpg', true, 5, datetime('now'), datetime('now')),
(33, 'ឆាក្តៅអន្ទង់', 'Black eel stew - Khmer foods', 2.5, 'https://cqodncaubuytsrskeuxy.supabase.co/storage/v1/object/public/khmer-food/products/1f485ff2-15b6-4093-b329-0b566444ce9e.jpg', true, 5, datetime('now'), datetime('now')),
(34, 'សាច់គោឆាអង្រង', 'Fried beef with ginger - Khmer foods', 2.5, 'https://cqodncaubuytsrskeuxy.supabase.co/storage/v1/object/public/khmer-food/products/378efa82-43d3-483b-99f8-36831b79113c.jpg', true, 5, datetime('now'), datetime('now')),
(35, 'ឆាក្តៅសាច់មាន់', 'Hot chicken curry - Khmer food', 2.5, 'https://cqodncaubuytsrskeuxy.supabase.co/storage/v1/object/public/khmer-food/products/8019bf1f-1346-4cd5-99fd-1cb6e792d7a7.jpg', true, 5, datetime('now'), datetime('now')),
(36, 'ឆាក្តៅកង្កែប', 'hot frog curry - Khmer foods', 2.5, 'https://cqodncaubuytsrskeuxy.supabase.co/storage/v1/object/public/khmer-food/products/2b403a90-828b-4a91-be7d-90a8e47cd180.jpg', true, 5, datetime('now'), datetime('now')),
(37, 'ឆាអណ្តាតគោអង្រង', 'Stir-fried beef tongue - Khmer food', 3.0, 'https://cqodncaubuytsrskeuxy.supabase.co/storage/v1/object/public/khmer-food/products/3098cb43-0f7c-416c-80bf-a64438674bdd.jpg', true, 5, datetime('now'), datetime('now')),
(38, 'សាច់គោឆាម្រេចខ្ចី', 'Stir-fried beef with fresh pepper - Khmer food', 2.5, 'https://cqodncaubuytsrskeuxy.supabase.co/storage/v1/object/public/khmer-food/products/d415c763-1898-4806-a155-1ce34f0e3ca5.jpg', true, 5, datetime('now'), datetime('now')),
(39, 'ឆាម្នាស់សាច់គោ', 'Stir-fried beef with pineapple - Khmer food', 2.5, 'https://cqodncaubuytsrskeuxy.supabase.co/storage/v1/object/public/khmer-food/products/30d981e5-074c-434c-bb36-de452cd3a266.jpg', true, 5, datetime('now'), datetime('now')),
(40, 'ឆាមឹកម្រេចខ្ជី', 'Stir-fried squid with chili pepper - Khmer food', 2.5, 'https://cqodncaubuytsrskeuxy.supabase.co/storage/v1/object/public/khmer-food/products/9510cd7b-84b4-472d-b622-2d777814195a.jpg', true, 5, datetime('now'), datetime('now'));

-- Sample Customer (password is 'password')
INSERT INTO "customers" ("id", "name", "email", "phone", "password_hash", "address", "created_at", "updated_at") VALUES
(1, 'Test User', 'test@example.com', '+85512345678', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhLu', 'Phnom Penh, Cambodia', datetime('now'), datetime('now'));
