# AGENTS.md

## Project Context
This repository is the backend API server for `Best Offer`, a rare-item auction platform.

- **Backend Stack**: Java 21, Spring Boot 3.2.5, Spring Data JPA, MySQL, Redis
- **Infrastructure**: AWS EC2, Docker
- **CI/CD**: GitHub Actions (Build -> Test -> EC2 Deploy)
- **API Documentation**: Springdoc OpenAPI (Swagger UI at `/swagger-ui/index.html`)

## Core Architecture & Design Rules

When implementing or modifying features, you MUST follow these architectural constraints:

1.  **Layered Architecture**: 
    - Strictly separate concerns: `Controller` -> `Facade` (Optional) -> `Service` -> `Repository`.
    - Controllers must only handle HTTP requests/responses. Business logic belongs in Services.
2.  **Concurrency & Transaction Management**:
    - High-traffic domains (like Bidding) use Redis Distributed Locks.
    - **CRITICAL**: Always use the Facade pattern (e.g., `BidLockFacade`) to separate lock acquisition from `@Transactional` boundaries. Do not hold DB connections while waiting for a Redis lock.
3.  **Authentication & Security**:
The system relies purely on **Session-Based Authentication** (`HttpSession`).
    - Post-login, the authenticated user's ID is stored in the session as `LOGIN_USER`.
    - Use the custom `@RequireLogin` annotation to protect endpoints.
    - Retrieve the logged-in user's ID within controllers like so: `Long userId = (Long) session.getAttribute("LOGIN_USER");`
4.  **Performance Optimization**:
    - Be mindful of Full Table Scans. Always consider Composite Indexes for frequent read queries (e.g., Auction status + end_time).
    - Use Redis for Fail-Fast caching mechanisms (e.g., validating minimum bid prices) before hitting the DB.
    - Use Bulk Updates to minimize network I/O overhead when updating multiple records.

## Exception Handling Rules

- The project uses a global `@RestControllerAdvice` (`GlobalExceptionHandler`) to handle exceptions. 
- Do NOT manually return error `ResponseEntity` objects (e.g., `ResponseEntity.status(400).body(...)`) inside Controllers or Services. Instead, **throw the corresponding custom exception** from the Service layer, and let the `GlobalExceptionHandler` format the HTTP response.

### Available Custom Exceptions

**[400 Bad Request] - Validation & Business Logic Errors**
- `InvalidEndTimeException`: Auction end time is set in the past.
- `InvalidPriceException`: Price is less than 0.
- `SelfBidException`: User tries to bid on their own auction.
- `ConsecutiveBidException`: User tries to bid when they are already the highest bidder.
- `LowBidPriceException`: Bid price is less than or equal to the current highest price.
- `AuctionClosedException`: Trying to bid on or modify an already closed auction.

**[401 Unauthorized] - Authentication Errors**
- `LoginFailedException`: Incorrect email or password.
- `LoginRequiredException`: Trying to access a protected resource without a session.

**[403 Forbidden] - Authorization Errors**
- `AdminRequiredException`: Non-admin trying to access admin endpoints.
- `UnauthorizedAccessException`: User trying to modify/delete an auction they do not own.

**[404 Not Found] - Resource Not Found**
- `UserNotFoundException`: User ID or Email does not exist in DB.
- `AuctionNotFoundException`: Auction ID does not exist in DB.

**[409 Conflict] - Resource Duplication**
- `DuplicateEmailException`: Email is already registered during signup.

**[500 Internal Server Error]**
- Fallback for all other unhandled `Exception.class`.

## Testing Expectations

The project has a strict CI/CD pipeline. Deployment will fail if any test breaks.

- Write unit tests for all new business logic using `JUnit5` and `Mockito`.
- When modifying existing endpoints, you must also update the corresponding Controller/Service tests.
- Maintain existing test performance and isolate DB testing environments appropriately.
- `BestOfferApplicationTests.contextLoads` mocks `RedissonClient` so the smoke test can load the Spring context without requiring local Redis. If a future local test explicitly exercises Redis behavior and Redis is unavailable, ask the developer to manually start Redis through Docker Engine before rerunning that test.

## Current Domain Models (Key Entities)

- `User`: Handles account information.
- `Auction`: The main item being sold (`status`, `startPrice`, `endTime`, etc.).
- `Bid`: Records of user bids on an auction.
- `Trade`: Created when an auction with a highest bidder is completed; stores `auctionId`, seller, buyer, final price, payment status, and shipping status.

## Current Implementation Notes

- Base Java package is `me.anjsk.bestoffer`.
- Public API base path is `/api/v1`.
- Response bodies for command endpoints are currently plain text strings, not JSON objects.
- Pageable endpoints use Spring Data's default `Page<T>` JSON shape with `content`, `pageable`, `totalElements`, `totalPages`, `size`, `number`, `sort`, `first`, `last`, `numberOfElements`, and `empty`.
- Date/time fields are serialized from `LocalDateTime` values. Frontend should send and expect ISO-8601 local datetime strings such as `2026-06-01T18:00:00` unless the backend contract changes.
- Current auction statuses are `ON_SALE`, `COMPLETED`, and `DELETED`.
- Current user roles are `ROLE_USER` and `ROLE_ADMIN`.
- `BidController` is currently in TEST MODE: `@RequireLogin` is commented out and `bidderId` is read from `BidRequest`. Before production, restore session authentication and remove client-supplied `bidderId` from the request contract.
- `BidLockFacade` uses Redis key `auction:{auctionId}:info` for fail-fast validation and Redisson lock key `auction_lock:{auctionId}` before calling transactional bid persistence.
- `AuctionScheduler` completes expired auctions in bulk and publishes `AuctionCompletedEvent` for successful auctions with a highest bidder.

## Branch Management & Workflow

**NEVER commit or push directly to the `develop` or `main` branches.**

1. **Branching**: Always branch off from `develop`.
   - Command: `git checkout -b feature/<feature-name>` (e.g., `feature/auction-update`)
2. **Commit & Push**: Work on your feature branch, commit your changes, and push the branch to the remote repository.
   - Command: `git push origin feature/<feature-name>`
3. **Pull Request (PR)**: Do NOT merge your own branch. The human developer (Owner) will review the code and manually merge the PR into `develop`.
4. **After PR Completion**: When the developer says the PR is complete/merged, switch back to `develop` and pull the latest changes.
   - Commands: `git checkout develop` then `git pull origin develop`

## Commit Message Convention

Use one of these exact commit type prefixes:
- `feat`: new feature
- `fix`: bug fix
- `refactor`: code refactoring
- `test`: test additions or updates
- `docs`: documentation changes
- `chore`: build tasks, dependencies, infra configs

Example: `refactor: extract lock logic to BidLockFacade`

## Documentation Maintenance (Self-Updating)

`AGENTS.md` is a living document. As you develop, you MUST proactively update this file when you encounter or establish new architectural patterns, workarounds, or important domain knowledge that future AI agents (or human developers) need to know.

For example, update `AGENTS.md` immediately in the same commit when you:
- Create a new Custom Exception or modify the global exception handling strategy.
- Discover a recurring bug/pitfall and establish a workaround.
- Change deployment configurations or CI/CD pipeline steps.

## Frontend Synchronization 
To facilitate seamless collaboration with the Frontend AI Agent, you MUST proactively maintain the `FRONTEND_SYNC.md` file located in the root directory.

Whenever you add, modify, or delete an API endpoint, or change a Request/Response DTO, you MUST update `FRONTEND_SYNC.md` in the same commit.

**Format Requirements for `FRONTEND_SYNC.md`:**
1.  **Endpoint Summary**: Method, URL, and a brief description.
2.  **Authentication**: Clearly state if the endpoint requires a session (`@RequireLogin`).
3.  **JSON Schema**: Provide a clear example of the Request Body (if any) and the expected Response Body in JSON format. Do not use Java class names; use raw JSON.
4.  **Error Codes**: List the specific HTTP status codes and custom exceptions this endpoint might throw (e.g., 400 Bad Request: "LowBidPriceException").
5.  **Changelog**: Any new information Frontend needs to know
