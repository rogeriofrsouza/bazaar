INSERT INTO category (slug, name, parent_id)
VALUES ('electronics', 'Electronics', NULL),
       ('computers', 'Computers', NULL),
       ('home-kitchen', 'Home & Kitchen', NULL),
       ('fashion', 'Fashion', NULL),
       ('books', 'Books', NULL),
       ('sports-outdoors', 'Sports & Outdoors', NULL),
       ('toys-games', 'Toys & Games', NULL),
       ('beauty', 'Beauty & Personal Care', NULL),
       ('groceries', 'Groceries', NULL);

INSERT INTO category (slug, name, parent_id)
SELECT c.slug, c.name, p.id
FROM (VALUES ('phones', 'Phones', 'electronics'),
             ('tablets', 'Tablets', 'electronics'),
             ('headphones', 'Headphones', 'electronics'),
             ('cameras', 'Cameras', 'electronics'),
             ('tvs', 'TVs', 'electronics'),
             ('smartwatches', 'Smartwatches', 'electronics'),

             ('laptops', 'Laptops', 'computers'),
             ('desktops', 'Desktops', 'computers'),
             ('monitors', 'Monitors', 'computers'),
             ('keyboards-mice', 'Keyboards & Mice', 'computers'),
             ('storage', 'Storage', 'computers'),

             ('furniture', 'Furniture', 'home-kitchen'),
             ('kitchen-appliances', 'Kitchen Appliances', 'home-kitchen'),
             ('cookware', 'Cookware', 'home-kitchen'),
             ('bedding', 'Bedding', 'home-kitchen'),
             ('home-decor', 'Home Decor', 'home-kitchen'),

             ('mens-clothing', 'Men''s Clothing', 'fashion'),
             ('womens-clothing', 'Women''s Clothing', 'fashion'),
             ('shoes', 'Shoes', 'fashion'),
             ('bags', 'Bags', 'fashion'),
             ('jewelry', 'Jewelry', 'fashion'),

             ('fiction', 'Fiction', 'books'),
             ('non-fiction', 'Non-Fiction', 'books'),
             ('technology-books', 'Technology', 'books'),
             ('childrens-books', 'Children''s Books', 'books'),

             ('fitness', 'Fitness', 'sports-outdoors'),
             ('cycling', 'Cycling', 'sports-outdoors'),
             ('camping', 'Camping', 'sports-outdoors'),
             ('team-sports', 'Team Sports', 'sports-outdoors'),

             ('board-games', 'Board Games', 'toys-games'),
             ('video-games', 'Video Games', 'toys-games'),
             ('puzzles', 'Puzzles', 'toys-games'),
             ('building-toys', 'Building Toys', 'toys-games'),

             ('skincare', 'Skincare', 'beauty'),
             ('makeup', 'Makeup', 'beauty'),
             ('hair-care', 'Hair Care', 'beauty'),
             ('fragrances', 'Fragrances', 'beauty'),

             ('beverages', 'Beverages', 'groceries'),
             ('snacks', 'Snacks', 'groceries'),
             ('pantry', 'Pantry', 'groceries')) AS c (slug, name, parent_slug)
         JOIN category p ON p.slug = c.parent_slug;
