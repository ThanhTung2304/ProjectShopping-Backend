# 🛍️ LEANH Studio — Backend Service

Backend system for the **LEANH Studio** fashion e-commerce platform, built with Spring Boot following a clean layered architecture (Controller → Service → Repository), secured with JWT, and featuring an **AI shopping assistant powered by a RAG (Retrieval-Augmented Generation) pipeline** using the Google Gemini API.

## 📌 Features

- ✅ **JWT Authentication & Authorization** — access token + refresh token, automatic expiration handling
- ✅ **Google OAuth Login**
- ✅ **Email OTP Verification** (Mailtrap) — password reset, account recovery
- ✅ **Role-Based Access Control** — `ADMIN` / `CUSTOMER` roles via Spring Security (`hasRole`, `@PreAuthorize`)
- ✅ **Product Management** — categories, variants (size/color/price/stock), multiple images, primary image, active status
- ✅ **Cart & Order Management** — add/update/remove cart items, order creation, order status tracking, automatic cart restoration on cancelled/failed payments
- ✅ **VNPay Payment Integration** — IPN handler, payment confirmation, race-condition handling for stock/coupon reservation
- ✅ **Coupon System** — order-level discount codes
- ✅ **Product Reviews**
- ✅ **AI Shopping Assistant (RAG Chatbot)** — answers customers based on real product data, no hallucinated information
- ✅ **Global Exception Handling** — unified error responses via `GlobalExceptionHandler`
- ✅ **DTO-based Request/Response** — clean separation between entities and API payloads, mapped via MapStruct
- ✅ **Entity Auditing** — automatic `createdAt` / `updatedAt`
- ✅ **Pagination, Filtering & Search** — by keyword, category, price range, size, color

## 📁 Project Structure

```
fashionshop-BE
├── config             # Spring Security, JWT Filter, CORS
├── controller         # REST API Endpoints (public + admin)
├── dto                # Request/Response DTOs per domain
├── entity             # JPA Entities (Product, Order, Cart, ProductEmbedding...)
├── exception          # Custom Exceptions + GlobalExceptionHandler
├── mapper             # MapStruct mappers (Entity ↔ DTO)
├── repository         # Spring Data JPA Repositories
├── service            # Service Interfaces
├── service/impl       # Service Implementations
└── util               # Enums, shared constants
```

## 🧠 Tech Stack

| Technology | Purpose |
|---|---|
| Spring Boot 4.0.6 (Java 21) | Core framework |
| Spring Data JPA / Hibernate | ORM & repository layer |
| Spring Security | Authentication & authorization |
| JWT (jjwt) | Token-based auth |
| MySQL | Relational database |
| MapStruct | Entity ↔ DTO mapping |
| Lombok | Boilerplate reduction |
| Spring Mail (Mailtrap) | OTP email delivery |
| Docker | Local environment packaging |
| Railway | Production hosting (backend + MySQL) |
| Google Gemini API | Embeddings & response generation for the chatbot |

## 🤖 AI Shopping Assistant — RAG Architecture

No dedicated vector database (Pinecone/Qdrant, etc.) is used — cosine similarity is computed directly in memory, which is well suited for small-to-medium product catalogs and avoids extra infrastructure.

```
Customer sends a message
      ↓
Merge recent context (last 1-2 turns) into the query, to resolve pronouns ("that one", "this product")
      ↓
Generate an embedding for the query (Gemini Embedding API)
      ↓
Compute cosine similarity against embeddings of all stored products
      ↓
Filter by relevance threshold + classify intent (greeting / single product / browsing multiple options)
      ↓
Combine relevant product context + conversation history into the system prompt
      ↓
Gemini Chat API generates a natural, Vietnamese-language reply
```

**Automatic data sync:** whenever a product or variant is created, updated, or deleted through the admin panel, its embedding is automatically generated, refreshed, or removed — no manual step required. A `generate-all` endpoint is also available for bulk re-sync, processing each product in its own transaction so a single failure doesn't roll back the entire batch.

## 🚀 Getting Started

### Prerequisites
- Java 21+
- Maven
- MySQL
- Docker (recommended, to run backend + database together)
- A Google Gemini API key (free at [aistudio.google.com](https://aistudio.google.com))

### Configure environment variables

Create a `.env` file or set system environment variables:

```env
DB_HOST=localhost
DB_PORT=3306
DB_NAME=fashion_store
DB_USERNAME=root
DB_PASSWORD=your_password

JWT_SECRET=your_jwt_secret
JWT_EXPIRATION=86400000

MAILTRAP_USERNAME=
MAILTRAP_PASSWORD=

FRONTEND_URL=http://localhost:5173

VNPAY_TMN_CODE=
VNPAY_HASH_SECRET=
VNPAY_PAY_URL=
VNPAY_IPN_URL=
VNPAY_RETURN_URL=

GEMINI_API_KEY=
```

### Run with Docker (recommended)
```bash
docker compose up --build -d
```

### Or run directly with Maven
```bash
./mvnw spring-boot:run
```

The backend runs at `http://localhost:8080`.

## 📬 API Overview

### 🔐 Auth
```
POST /api/auth/register
POST /api/auth/login
POST /api/auth/refresh-token
POST /api/auth/forgot-password
```

### 🛒 Products & Categories
```
GET  /api/products          — Search, filter, paginate (public)
GET  /api/products/{slug}   — Product details (public)
GET  /api/categories        — List categories (public)
```

### 🛍️ Cart & Orders
```
GET    /api/cart
POST   /api/cart
PUT    /api/cart/{itemId}
DELETE /api/cart/{itemId}

POST   /api/orders
GET    /api/orders
```

### 💳 Payments
```
GET  /api/payments/vnpay/ipn      — Payment confirmation webhook (public)
GET  /api/payments/vnpay/return   — Post-payment redirect (public)
```

### 🤖 Chatbot
```
POST /api/chat
Body: { "message": "...", "history": [...] }
```

### 🛡️ Admin (requires ADMIN role)
```
POST/PUT/DELETE  /api/admin/products/**
POST/PUT/DELETE  /api/admin/categories/**
POST             /api/admin/embeddings/generate-all
```

## 🛡️ Roles & Permissions

| Role | Permissions |
|---|---|
| `ADMIN` | Full access to products, categories, orders, users, coupons |
| `CUSTOMER` | Cart, orders, reviews, personal account management |

Access control is centralized in `SecurityConfig.java`, combined with `JwtAuthFilter` for token validation on every request.

## 📄 Core Entities

`User`, `Product`, `Category`, `ProductVariant`, `ProductImage`, `ProductEmbedding`, `CartItem`, `Order`, `OrderItem`, `Payment`, `Coupon`, `Review`

## ⚠️ Operational Notes

Gemini models can be deprecated or renamed over time (this project previously hit deprecation issues with `text-embedding-004` and `gemini-2.0-flash`). If you encounter a `404 model not found` error, check the current list of supported models at [ai.google.dev/gemini-api/docs/deprecations](https://ai.google.dev/gemini-api/docs/deprecations) and update `app.gemini.embedding-model` / `app.gemini.chat-model` in `application.yml` accordingly.
