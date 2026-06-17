package com.ecommerce.api.config;

import com.ecommerce.api.model.*;
import com.ecommerce.api.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * DataSeeder is a Spring-managed CommandLineRunner that boots the application
 * with a production-representative dataset.
 *
 * This seeder is idempotent: it checks for existing data before inserting, making
 * it safe to run in any environment restart scenario. It seeds:
 *   - 5 categories with 10+ products each (55 products total)
 *   - 5 simulated user accounts
 *   - 5+ historical orders with mixed statuses (PENDING, PAID, SHIPPED)
 *
 * This dataset is designed to support downstream DevSecOps pipelines including
 * DAST scanning, API fuzzing, and functional security testing.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final CartRepository cartRepository;
    private final OrderRepository orderRepository;

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.count() > 0) {
            log.info("[DataSeeder] Database already contains data. Skipping seed operation.");
            return;
        }

        log.info("[DataSeeder] Empty database detected. Initiating automated data seeding...");

        List<User> users = seedUsers();
        List<Product> products = seedProducts();
        seedHistoricalOrders(users, products);

        log.info("[DataSeeder] Seeding complete. {} users, {} products, 5 orders inserted.",
                users.size(), products.size());
    }

    // =========================================================================
    // USERS
    // =========================================================================
    private List<User> seedUsers() {
        List<User> users = new ArrayList<>();
        String[][] userData = {
            {"alice_dev",    "alice.devlin@techcorp.io"},
            {"bob_qatester", "bob.qa@devops-lab.com"},
            {"carol_infosec","carol.sec@securenet.org"},
            {"dave_sre",     "dave.sre@cloudops.net"},
            {"eve_dast",     "eve.dast@pentest.tools"}
        };

        for (String[] row : userData) {
            User user = User.builder()
                    .username(row[0])
                    .email(row[1])
                    .build();
            // Manually set createdAt because @PrePersist fires on save
            users.add(userRepository.save(user));
        }
        log.info("[DataSeeder] Seeded {} users.", users.size());
        return users;
    }

    // =========================================================================
    // PRODUCTS  (55 products across 5 categories)
    // =========================================================================
    private List<Product> seedProducts() {
        List<Product> all = new ArrayList<>();

        all.addAll(seedElectronics());
        all.addAll(seedApparel());
        all.addAll(seedHomeAndKitchen());
        all.addAll(seedBooks());
        all.addAll(seedFitness());

        log.info("[DataSeeder] Seeded {} products.", all.size());
        return all;
    }

    private List<Product> seedElectronics() {
        Object[][] data = {
            {"Sony WH-1000XM5 Wireless Headphones",
             "Industry-leading noise cancelling headphones with 30-hour battery life, multipoint connection for two devices simultaneously, and crystal-clear hands-free calling with a precisely-placed beamforming microphone array.",
             BigDecimal.valueOf(349.99), 120},
            {"Apple MacBook Pro 14-inch M3 Pro",
             "Supercharged by the Apple M3 Pro chip with a 12-core CPU and 18-core GPU. Features a stunning Liquid Retina XDR display, up to 22 hours of battery life, and MagSafe charging. Ideal for professional developers and creative studios.",
             BigDecimal.valueOf(1999.00), 45},
            {"Samsung 65\" Neo QLED 8K Smart TV",
             "Experience cinematic 8K resolution with Quantum Matrix Technology, Neo Quantum Processor 8K AI upscaling, and Object Tracking Sound Pro. Includes a 120Hz refresh rate panel perfect for gaming and sports.",
             BigDecimal.valueOf(1499.00), 30},
            {"Logitech MX Master 3S Mouse",
             "Advanced ergonomic wireless mouse with 8K DPI any-surface tracking, MagSpeed electromagnetic scrolling, and customisable gesture buttons. Works silently across Windows, macOS, and Linux.",
             BigDecimal.valueOf(99.99), 200},
            {"Anker 737 Power Bank 24000mAh",
             "Ultra-high-capacity portable charger with 140W bi-directional power output. Charges a MacBook Pro from 0–50% in 36 minutes. Smart digital display shows real-time wattage and remaining capacity.",
             BigDecimal.valueOf(129.99), 175},
            {"Kindle Paperwhite Signature Edition",
             "Our best Kindle ever with a 6.8-inch glare-free display, wireless charging, auto-adjusting warm light, 32GB storage for thousands of books, and weeks of battery life.",
             BigDecimal.valueOf(139.99), 300},
            {"GoPro HERO12 Black Action Camera",
             "Capture stunning 5.3K60 video and 27MP photos with industry-leading HyperSmooth 6.0 stabilisation. Waterproof to 33ft. Supports 360-degree horizon levelling and HDR video.",
             BigDecimal.valueOf(399.99), 85},
            {"Bose SoundLink Max Portable Speaker",
             "Achieve premium stereo sound anywhere with this rugged IP67-rated wireless speaker. Features Bose Immersive Audio, 20-hour playtime, and a built-in handle for outdoor adventures.",
             BigDecimal.valueOf(449.00), 60},
            {"ASUS ROG Strix G16 Gaming Laptop",
             "Powered by Intel Core i9-14900HX and NVIDIA GeForce RTX 4080 with 12GB VRAM. 16-inch QHD+ 240Hz display, 32GB DDR5 RAM, 1TB PCIe 4.0 SSD. MUX Switch for maximum GPU performance.",
             BigDecimal.valueOf(2499.00), 20},
            {"Elgato Stream Deck MK.2",
             "Studio controller with 15 fully customisable LCD keys. Trigger unlimited actions including launching media, adjusting audio, switching scenes in OBS, posting to social media, and controlling smart home devices.",
             BigDecimal.valueOf(149.99), 140},
            {"WD 4TB My Passport Portable SSD",
             "Ultra-compact hardware-encrypted portable solid-state drive with USB-C connectivity. Delivers transfer speeds up to 1050MB/s. Password protection and automatic backup included.",
             BigDecimal.valueOf(119.99), 220}
        };
        return buildProducts("Electronics", data);
    }

    private List<Product> seedApparel() {
        Object[][] data = {
            {"Patagonia Men's Nano Puff Jacket",
             "Lightweight, packable insulation made with 100% recycled PrimaLoft Gold Eco insulation. Wind- and water-resistant shell. Stuffs into its own chest pocket for easy storage. Fair Trade Certified sewn.",
             BigDecimal.valueOf(229.00), 95},
            {"Levi's 511 Slim Fit Jeans",
             "Classic slim-cut jeans crafted from Levi's signature stretch denim. Sits below the waist with a slim fit through the thigh. Five-pocket styling. Available in multiple washes.",
             BigDecimal.valueOf(69.99), 350},
            {"Nike Air Force 1 '07 Sneakers",
             "The radiant design of the original basketball shoe inspires this streamlined icon. Premium tumbled leather upper with perforations for ventilation. Foam midsole for all-day cushioning.",
             BigDecimal.valueOf(110.00), 280},
            {"The North Face ThermoBall Eco Vest",
             "Lightweight vest filled with 100% recycled ThermoBall insulation that traps heat even when wet. Wind-resistant ripstop shell. Packable into its own hand pocket. Ideal layering piece.",
             BigDecimal.valueOf(119.95), 130},
            {"Uniqlo Ultra Light Down Compact Jacket",
             "An iconic packable down jacket filled with premium 90% down. Extremely lightweight at just 250g. Inner seam tape construction prevents down leakage. Folds compactly into its own chest pocket.",
             BigDecimal.valueOf(79.99), 420},
            {"Columbia PFG Tamiami II Long-Sleeve Shirt",
             "Purpose-built fishing shirt with built-in UPF 40+ sun protection, vented back design for airflow, and moisture-wicking Omni-Wick fabric. Two button-close chest pockets with hidden utility loop.",
             BigDecimal.valueOf(49.99), 310},
            {"Allbirds Men's Tree Runners",
             "Ultra-breathable running shoes crafted from TENCEL lyocell fibre derived from eucalyptus trees. Naturally moisture-wicking, temperature-regulating, and machine washable.",
             BigDecimal.valueOf(125.00), 165},
            {"Carhartt WIP Chase Hoodie",
             "Mid-weight fleece sweatshirt made from 100% organic cotton. Kangaroo pocket, adjustable drawstring hood, ribbed cuffs and waistband. Embroidered Chase logo on chest and back.",
             BigDecimal.valueOf(89.00), 200},
            {"Merino Wool Base Layer Top – Icebreaker 200",
             "Ultralight 200gsm merino wool base layer that regulates temperature naturally in both warm and cold conditions. Anti-odour, moisture-wicking, and bluesign® approved. Ideal for hiking and travel.",
             BigDecimal.valueOf(109.95), 145},
            {"Arc'teryx Cerium Hoody",
             "Premium 850-fill-power RDS-certified goose-down hoody with a Coreloft synthetic underarm panels for unrestricted movement. Adjustable StormHood. Ultralight at 275g.",
             BigDecimal.valueOf(349.00), 55},
            {"Darn Tough Vermont Micro Crew Socks",
             "Made in Vermont from blended fine Merino wool, nylon, and Lycra for unparalleled durability. Fully guaranteed for life — if they ever wear out, return them for a free replacement pair.",
             BigDecimal.valueOf(24.95), 500}
        };
        return buildProducts("Apparel", data);
    }

    private List<Product> seedHomeAndKitchen() {
        Object[][] data = {
            {"Instant Pot Duo 7-in-1 Electric Pressure Cooker 8Qt",
             "Replaces 7 kitchen appliances: pressure cooker, slow cooker, rice cooker, steamer, sauté pan, yogurt maker, and warmer. 13 one-touch Smart Programs. Stainless steel inner pot is dishwasher-safe.",
             BigDecimal.valueOf(89.99), 145},
            {"Vitamix 5200 Professional Blender",
             "Variable speed control with radial cooling fan and thermal protection system. Aircraft-grade stainless steel blades designed to handle the toughest ingredients. 64-oz low-profile container.",
             BigDecimal.valueOf(449.95), 70},
            {"Dyson V15 Detect Absolute Vacuum",
             "Reveals hidden dust with a laser that illuminates microscopic particles on hard floors. Automatically adapts suction and recommends run time based on the floor type detected.",
             BigDecimal.valueOf(749.99), 55},
            {"Le Creuset Signature Enameled Cast Iron Dutch Oven 5.5Qt",
             "Oven-safe to 500°F. Superior heat distribution and retention. Tight-fitting lid traps moisture and nutrients. Colourful exterior enamel resists chipping and cracking. Dishwasher-safe.",
             BigDecimal.valueOf(399.95), 80},
            {"Nespresso Vertuo Next Coffee Machine",
             "Brews five cup sizes at the touch of a button using Centrifusion™ technology. Automatically adjusts brewing parameters for each coffee capsule. Wi-Fi and Bluetooth enabled for firmware updates.",
             BigDecimal.valueOf(179.00), 120},
            {"iRobot Roomba j7+ Self-Emptying Robot Vacuum",
             "PrecisionVision Navigation avoids obstacles in its path like pet waste and charging cables. Clean Base Automatic Dirt Disposal holds 60 days of debris. Works with Alexa and Google Assistant.",
             BigDecimal.valueOf(799.99), 35},
            {"All-Clad D3 Stainless 10-Piece Cookware Set",
             "Tri-ply bonded cookware with alternating layers of stainless steel and aluminium for warp-resistant strength. Flush-riveted handles stay cool on the stovetop. Oven and broiler safe to 600°F.",
             BigDecimal.valueOf(699.99), 40},
            {"Philips Hue White & Colour Ambiance Starter Kit",
             "Includes 4 A19 smart bulbs plus a Hue Bridge. Millions of colours and whites. Compatible with Alexa, Google Assistant, and Apple HomeKit. Geofencing, timers, and scene routines via app.",
             BigDecimal.valueOf(199.99), 180},
            {"Coway AP-1512HH Air Purifier",
             "True HEPA filter captures 99.97% of particles as small as 0.3 microns. Four-stage air filtration with pre-filter, deodorization filter, HEPA, and vital ionizer. Air quality indicator.",
             BigDecimal.valueOf(109.99), 230},
            {"Cuisinart TOA-70 Air Fryer Toaster Oven",
             "Two appliances in one: a full-size convection toaster oven and an air fryer. 1800W heating element. Large 0.6 cu ft interior fits a 12-inch pizza or 6 slices of bread.",
             BigDecimal.valueOf(249.95), 95},
            {"Lodge 12-Inch Seasoned Cast Iron Skillet",
             "Pre-seasoned with 100% natural vegetable oil. Unparalleled heat retention for searing, sautéing, baking, broiling, braising, and frying. Compatible with all cooking surfaces including induction.",
             BigDecimal.valueOf(39.90), 400}
        };
        return buildProducts("Home & Kitchen", data);
    }

    private List<Product> seedBooks() {
        Object[][] data = {
            {"Designing Data-Intensive Applications – Martin Kleppmann",
             "The definitive guide to the internals of modern data systems, covering replication, partitioning, transactions, batch and stream processing, and the future of data engineering. Essential reading for every backend engineer.",
             BigDecimal.valueOf(54.99), 310},
            {"Clean Code: A Handbook of Agile Software Craftsmanship – Robert C. Martin",
             "A revolutionary paradigm for agile software development with practical examples in Java. Martin shows how to write human-readable, maintainable code and how to refactor existing messes into clean systems.",
             BigDecimal.valueOf(39.99), 280},
            {"The Pragmatic Programmer: Your Journey to Mastery – Hunt & Thomas",
             "Celebrated as one of the most influential books in software development. The 20th Anniversary Edition is updated with new technologies and the same wisdom that has guided a generation of software engineers.",
             BigDecimal.valueOf(49.99), 250},
            {"System Design Interview – An Insider's Guide Vol 1 – Alex Xu",
             "A step-by-step framework for how to ace the system design interview at top tech companies like Google, Amazon, and Meta. Covers rate limiting, consistent hashing, key-value stores, and URL shorteners.",
             BigDecimal.valueOf(34.95), 400},
            {"Zero to Production in Rust – Luca Palmieri",
             "Hands-on book on building production-ready RESTful APIs in Rust using Actix-web and PostgreSQL. Covers CI/CD with GitHub Actions, telemetry, authentication, and deployment to DigitalOcean.",
             BigDecimal.valueOf(44.99), 190},
            {"The Phoenix Project – Gene Kim, Kevin Behr, George Spafford",
             "A novel about IT, DevOps, and helping your business win. Follows Bill, an IT manager who must solve a crisis before the company CTO fires everyone. Based on the principles of lean manufacturing.",
             BigDecimal.valueOf(29.99), 350},
            {"Kubernetes: Up and Running – Brendan Burns, Joe Beda, Kelsey Hightower",
             "Written by three Kubernetes co-founders. Covers deploying, scaling, and managing containerised applications. Includes practical examples for managing distributed systems in the cloud.",
             BigDecimal.valueOf(59.99), 175},
            {"Security Engineering – Ross Anderson (3rd Edition)",
             "The authoritative reference for building secure, dependable systems. Covers the entire lifecycle of security architecture from cryptography and access control to psychology, economics, and adversarial machine learning.",
             BigDecimal.valueOf(79.99), 120},
            {"The DevOps Handbook – Gene Kim, Jez Humble, Patrick Debois",
             "How world-class companies achieve high performance through tight integration of technical and management practices. Case studies from Netflix, Etsy, and Google. Paired with DORA metrics research.",
             BigDecimal.valueOf(39.95), 300},
            {"Accelerate: The Science of Lean Software – Nicole Forsgren",
             "Based on four years of groundbreaking research examining DevOps practices worldwide. Identifies the capabilities that drive software delivery performance and what predicts organizational outcomes.",
             BigDecimal.valueOf(27.99), 340},
            {"Web Application Hacker's Handbook – Stuttard & Pinto",
             "Comprehensive guide to discovering, exploiting, and preventing web application vulnerabilities. Covers SQL injection, XSS, CSRF, authentication bypass, and advanced exploitation techniques used by professional penetration testers.",
             BigDecimal.valueOf(49.95), 145},
            {"Site Reliability Engineering – Beyer, Jones, Petoff, Murphy (Google)",
             "How Google runs production systems. SRE teams at Google share their principles and practices for building, deploying, monitoring, and maintaining systems at the largest and most complex scale in the industry.",
             BigDecimal.valueOf(59.99), 200}
        };
        return buildProducts("Books", data);
    }

    private List<Product> seedFitness() {
        Object[][] data = {
            {"Peloton Bike+",
             "Auto-follow resistance synchronises your workout to your instructor's cues. Rotating 24-inch HD touchscreen streams Peloton classes across Cycling, Strength, Yoga, Meditation, and Running. 30-day free membership included.",
             BigDecimal.valueOf(2495.00), 15},
            {"Bowflex SelectTech 552 Adjustable Dumbbells (Pair)",
             "Replace 15 sets of weights. Dial changes resistance from 5 to 52.5 lbs in 2.5-lb increments. Compact footprint. Moulded trays keep them organised. Commercial-grade with a 2-year warranty.",
             BigDecimal.valueOf(429.00), 55},
            {"TRX PRO4 Suspension Trainer System",
             "Professional-grade suspension training system used by US military, elite athletes, and professional sports teams. Anchors to any door, beam, or tree. Includes workout guide and access to app workouts.",
             BigDecimal.valueOf(199.95), 90},
            {"Garmin Fenix 7X Solar GPS Multisport Watch",
             "Solar-powered GPS smartwatch with up to 37 days of battery in smartwatch mode. Built-in flashlight, topographic maps, ski resort maps, and golf course layouts. Pulse oximetry and advanced sleep monitoring.",
             BigDecimal.valueOf(899.99), 40},
            {"Theragun Pro Plus Percussive Therapy Device",
             "6-in-1 wellness device combining percussive therapy, heat, cooling, and vibration. App-guided wellness routines. 2-hour battery life with rotating arm for reaching every muscle group.",
             BigDecimal.valueOf(599.00), 65},
            {"Rogue Ohio Power Bar 20KG",
             "Made in Columbus, Ohio. Dual-marked for both powerlifting and Olympic lifting. 29mm shaft diameter, 190,000 PSI tensile strength steel, and aggressive knurling. Rated to 1500 lbs.",
             BigDecimal.valueOf(325.00), 30},
            {"NordicTrack Commercial 1750 Treadmill",
             "30 digital incline settings from -3% to 15% and 12mph top speed. Includes a 1-year iFIT Family membership to access thousands of trainer-led on-demand and live workout classes worldwide.",
             BigDecimal.valueOf(1799.00), 18},
            {"Manduka PRO Yoga Mat 6mm",
             "Professional-grade yoga mat made from closed-cell PVC with an ultra-dense cushion. The non-slip surface improves with use. Certified free of toxic chemicals. Lifetime guarantee.",
             BigDecimal.valueOf(120.00), 200},
            {"Concept2 RowErg Indoor Rowing Machine",
             "Used in Olympic training, CrossFit, and rowing clubs worldwide. Performance Monitor 5 tracks pace, watts, split times, and calories. Flywheel with damper provides smooth, consistent resistance.",
             BigDecimal.valueOf(990.00), 22},
            {"Hydro Flask 32 oz Standard Mouth Water Bottle",
             "TempShield double-wall vacuum insulation keeps drinks cold for 24 hours and hot for 12 hours. 18/8 pro-grade stainless steel. Powder coat finish for secure grip. Dishwasher-safe, BPA-free.",
             BigDecimal.valueOf(44.95), 500},
            {"WHOOP 4.0 Fitness & Health Tracker",
             "Wrist-worn sensor tracks strain, recovery, and sleep 24/7 with medical-grade accuracy. No screen or buttons — pairs with the WHOOP app. Continuous pulse oximetry and skin temperature monitoring.",
             BigDecimal.valueOf(239.00), 110}
        };
        return buildProducts("Fitness", data);
    }

    private List<Product> buildProducts(String category, Object[][] data) {
        List<Product> products = new ArrayList<>();
        for (Object[] row : data) {
            Product product = Product.builder()
                    .name((String) row[0])
                    .description((String) row[1])
                    .price((BigDecimal) row[2])
                    .category(category)
                    .stockQuantity((Integer) row[3])
                    .build();
            products.add(productRepository.save(product));
        }
        log.info("[DataSeeder] Seeded {} '{}' products.", products.size(), category);
        return products;
    }

    // =========================================================================
    // HISTORICAL ORDERS  (5 orders with mixed statuses)
    // =========================================================================
    private void seedHistoricalOrders(List<User> users, List<Product> products) {
        // Order 1: Alice – SHIPPED – Electronics items
        createHistoricalOrder(
                users.get(0),
                List.of(
                    new OrderLineSpec(products.get(0), 1),  // Sony Headphones
                    new OrderLineSpec(products.get(3), 2)   // Logitech Mouse x2
                ),
                OrderStatus.SHIPPED,
                LocalDateTime.now().minusDays(30));

        // Order 2: Bob – PAID – Books
        // Books start at index 33 in the overall list (11 Electronics + 11 Apparel + 11 Home = 33)
        int booksOffset = 11 + 11 + 11;
        createHistoricalOrder(
                users.get(1),
                List.of(
                    new OrderLineSpec(products.get(booksOffset),     1),  // Designing Data-Intensive Apps
                    new OrderLineSpec(products.get(booksOffset + 2), 1),  // Pragmatic Programmer
                    new OrderLineSpec(products.get(booksOffset + 5), 1)   // Phoenix Project
                ),
                OrderStatus.PAID,
                LocalDateTime.now().minusDays(20));

        // Order 3: Carol – SHIPPED – Fitness items
        int fitnessOffset = 11 + 11 + 11 + 12;
        createHistoricalOrder(
                users.get(2),
                List.of(
                    new OrderLineSpec(products.get(fitnessOffset + 7), 2), // Manduka Yoga Mat x2
                    new OrderLineSpec(products.get(fitnessOffset + 9), 3)  // Hydro Flask x3
                ),
                OrderStatus.SHIPPED,
                LocalDateTime.now().minusDays(15));

        // Order 4: Dave – PENDING – Home & Kitchen
        int homeOffset = 11 + 11;
        createHistoricalOrder(
                users.get(3),
                List.of(
                    new OrderLineSpec(products.get(homeOffset),     1),  // Instant Pot
                    new OrderLineSpec(products.get(homeOffset + 4), 1),  // Nespresso
                    new OrderLineSpec(products.get(homeOffset + 10),1)   // Cast Iron Skillet
                ),
                OrderStatus.PENDING,
                LocalDateTime.now().minusDays(5));

        // Order 5: Eve – PAID – Mixed: Apparel + Electronics
        int apparelOffset = 11;
        createHistoricalOrder(
                users.get(4),
                List.of(
                    new OrderLineSpec(products.get(apparelOffset + 1), 1), // Levi's Jeans
                    new OrderLineSpec(products.get(apparelOffset + 2), 1), // Nike Air Force 1
                    new OrderLineSpec(products.get(4),                 1)  // Anker Power Bank
                ),
                OrderStatus.PAID,
                LocalDateTime.now().minusDays(10));

        log.info("[DataSeeder] Seeded 5 historical orders.");
    }

    private void createHistoricalOrder(User user,
                                       List<OrderLineSpec> specs,
                                       OrderStatus status,
                                       LocalDateTime timestamp) {
        Order order = Order.builder()
                .user(user)
                .status(status)
                .totalAmount(BigDecimal.ZERO) // calculated below
                .build();

        List<OrderItem> items = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        for (OrderLineSpec spec : specs) {
            OrderItem item = OrderItem.builder()
                    .order(order)
                    .product(spec.product())
                    .quantity(spec.quantity())
                    .priceAtPurchase(spec.product().getPrice())
                    .build();
            items.add(item);
            total = total.add(spec.product().getPrice().multiply(BigDecimal.valueOf(spec.quantity())));

            // Deduct stock to reflect realistic post-purchase state
            spec.product().setStockQuantity(
                    Math.max(0, spec.product().getStockQuantity() - spec.quantity()));
            productRepository.save(spec.product());
        }

        order.setItems(items);
        order.setTotalAmount(total);
        order.setCreatedAt(timestamp);

        orderRepository.save(order);
    }

    /**
     * Simple value record coupling a product reference with a desired quantity.
     */
    private record OrderLineSpec(Product product, int quantity) {}
}
