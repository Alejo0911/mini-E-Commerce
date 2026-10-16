# Mini E-commerce con Reserva Temporal de Stock

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3+-6DB33F?style=flat&logo=spring-boot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Vue 3](https://img.shields.io/badge/Vue.js-3.x-4FC08D?style=flat&logo=vuedotjs&logoColor=white)](https://vuejs.org/)
[![TypeScript](https://img.shields.io/badge/TypeScript-5.x-3178C6?style=flat&logo=typescript&logoColor=white)](https://www.typescriptlang.org/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15+-4169E1?style=flat&logo=postgresql&logoColor=white)](https://www.postgresql.org/)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

Un sistema de comercio electrónico de alta consistencia diseñado para gestionar concurrencia masiva mediante **bloqueos transaccionales pesimistas/optimistas**, expiración automática de reservas (**TTL**) y reversión atómica de inventario.

---

## 📌 Descripción del Proyecto

El reto técnico principal de este proyecto reside en garantizar la **consistencia de datos ante escenarios de alta concurrencia** sin provocar sobreventa (*overbooking*). Cuando un usuario inicia el proceso de checkout, el sistema realiza una reserva temporal de inventario con un tiempo de vida acotado (ej. 10 minutos). Si el pago no se confirma dentro del periodo asignado, un worker en segundo plano ejecuta una **reversión atómica** devuelviendo las unidades al inventario disponible.

---

## 🏛️ Arquitectura de Software

El proyecto aplica los principios de **Clean Architecture** (Arquitectura Limpia) e inversión de dependencias (**SOLID**), garantizando el desacoplamiento entre las reglas de negocio, los frameworks y la infraestructura de almacenamiento.

```text
               +-------------------------------------------------------+
               |                  Capa de Presentación                 |
               |       (REST Controllers / Vue 3 + Pinia UI)          |
               +---------------------------+---------------------------+
                                           |
                                           v
               +-------------------------------------------------------+
               |                  Capa de Aplicación                   |
               |         (Casos de Uso / Servicios de Negocio)         |
               +---------------------------+---------------------------+
                                           |
                                           v
               +-------------------------------------------------------+
               |                   Capa de Dominio                     |
               |        (Entidades Puras / Reglas de Negocio)          |
               +---------------------------+---------------------------+
                                           ^
                                           |
               +-------------------------------------------------------+
               |                Capa de Infraestructura                |
               |    (JPA Repositories / Workers / Security / Redis)    |
               +-------------------------------------------------------+
```

### Principales Desafíos Técnicos e Implementación:

1. **Bloqueo Transaccional Concurrente:**
   * Uso de consultas pesimistas (`SELECT FOR UPDATE`) a nivel de base de datos durante la reserva de inventario para evitar condiciones de carrera (*race conditions*).
2. **Expiración Automática (TTL) y Reversión Atómica:**
   * Programación de tareas asíncronas vía `@Scheduled` / Redis Workers para liberar reservas caducadas mediante transacciones aisladas con anotación `@Transactional`.
3. **Seguridad con Doble Token:**
   * Autenticación basada en *Access Token* (almacenado estrictamente en memoria en el cliente) y *Refresh Token* rotativo seguro enviado en cookies `HttpOnly`, `SameSite=Strict` y `Secure`.
4. **Tipado Estricto de Punta a Punta:**
   * Contratos fuertemente tipados utilizando DTOs con validación de esquemas en Spring Boot y TypeScript sin el uso de `any` en la aplicación cliente Vue 3.

---

## 📂 Estructura del Proyecto

### 1. Backend (`mini-ecommerce-backend`) - Spring Boot

```text
src/main/java/com/alejandro/ecommerce/
│
├── shared/                         # Transversal: Excepciones globales, utilidades, seguridad
│   ├── exception/                  # GlobalExceptionHandler, CustomExceptions
│   └── security/                   # Filtros JWT, Argon2id, RBAC / ABAC
│
├── domain/                         # Capa de Dominio (Entidades puras y Contratos)
│   ├── model/                      # Product, Order, OrderItem, StockReservation
│   └── repository/                 # Interfaces de repositorios (Puertos de salida)
│
├── application/                    # Capa de Aplicación (Casos de uso)
│   ├── usecase/                    # ReserveStockUseCase, ConfirmPaymentUseCase, ExpireOrderUseCase
│   └── dto/                        # Data Transfer Objects para request/response
│
└── infrastructure/                 # Capa de Infraestructura (Adaptadores externos)
    ├── persistence/                # Entidades JPA, Spring Data Repositories y Migraciones Flyway
    │   ├── entity/
    │   ├── repository/
    │   └── mapper/
    ├── rest/                       # Controladores REST API (Endpoints)
    └── scheduler/                  # StockExpirationWorker (@Scheduled / Redis Keyspace Listeners)
```

---

### 2. Frontend (`mini-ecommerce-frontend`) - Vue 3 + Pinia + TS

```text
src/
├── api/                            # Clientes HTTP tipados (Axios con Interceptores)
│   ├── axiosInstance.ts
│   ├── productApi.ts
│   └── orderApi.ts
│
├── components/                     # Componentes de interfaz reutilizables
│   ├── ui/                         # Botones, modales, alertas, loaders
│   └── product/                    # Tarjetas de productos, selector de cantidad
│
├── router/                         # Vue Router
│   └── index.ts                    # Navigation Guards asíncronos para control de accesos
│
├── store/                          # Tiendas Pinia normalizadas
│   ├── authStore.ts                # Gestión de sesión, tokens y perfil de usuario
│   ├── cartStore.ts                # Carrito de compras cliente
│   └── orderStore.ts               # Órdenes activas y temporizador de expiración (TTL)
│
├── types/                          # Interfaces y tipos TypeScript estrictos (Cero 'any')
│   ├── product.d.ts
│   ├── order.d.ts
│   └── user.d.ts
│
├── views/                          # Páginas / Vistas principales
│   ├── HomeView.vue                # Catálogo de productos
│   ├── CheckoutView.vue            # Pantalla con temporizador de reserva
│   └── LoginView.vue               # Autenticación
│
├── App.vue                         # Componente raíz
└── main.ts                         # Punto de entrada de la aplicación
```

---

## 🛠️ Tecnologías Utilizadas

### Backend
* **Lenguaje:** Java 17+
* **Framework:** Spring Boot 3.x (Spring Web, Spring Data JPA, Spring Security, Validation)
* **Base de Datos:** PostgreSQL
* **Migraciones:** Flyway
* **Caché / Tareas:** Redis (Opcional para TTL mediante estructuras de datos en memoria)
* **Herramientas:** Lombok, Spring Boot DevTools

### Frontend
* **Framework:** Vue 3 (Composition API con `<script setup>`)
* **Lenguaje:** TypeScript (Tipado estricto)
* **Gestor de Estado:** Pinia
* **Enrutamiento:** Vue Router (Navigation Guards)
* **Cliente HTTP:** Axios (Con interceptores para refresco automático de tokens)

---

## 🚀 Instalación y Configuración Local

### Prerrequisitos
* Java 17 o superior
* Node.js 18+ y npm/pnpm
* PostgreSQL 15+ instalada y ejecutándose
* Redis (opcional si se utiliza la estrategia con `@Scheduled`)

### 1. Clonar el repositorio
```bash
git clone https://github.com/tu-usuario/mini-ecommerce-backend.git
cd mini-ecommerce-backend
```

### 2. Configuración de Base de Datos
Crea una base de datos PostgreSQL llamada `ecommerce_db` y configura las credenciales en `src/main/resources/application.yml`:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/ecommerce_db
    username: tu_usuario
    password: tu_password
  jpa:
    hibernate:
      ddl-auto: validate
  flyway:
    enabled: true
```

### 3. Ejecutar el Backend
```bash
./gradlew bootRun
# O con Maven: ./mvnw spring-boot:run
```

---

## 📄 Licencia

Este proyecto se desarrolló bajo los estándares técnicos de la Guía de Proyectos de Grado en Ingeniería de Software.