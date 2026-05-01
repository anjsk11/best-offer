# FRONTEND_SYNC.md

This file reflects the current backend API contract for the Best Offer frontend.

## General Notes

- Base URL prefix: `/api/v1`
- Authentication: session cookie based. After login, the backend stores `LOGIN_USER` and `USER_ROLE` in `HttpSession`.
- Protected endpoints are marked with `@RequireLogin`.
- Command endpoints currently return `text/plain` response messages, not JSON.
- Date/time values use ISO-8601 local datetime strings, for example `2026-06-01T18:00:00`.
- Auction status values: `ON_SALE`, `COMPLETED`, `DELETED`.
- Pageable endpoints accept `page`, `size`, and `sort` query parameters.

## Endpoint: POST /api/v1/users/signup

### Endpoint Summary

Creates a new user account.

### Authentication

No session required.

### Request Body

```json
{
  "email": "seller@example.com",
  "password": "password1234",
  "nickname": "rareSeller"
}
```

### Response Body

HTTP 200, plain text:

```text
회원가입이 완료되었습니다.
```

### Error Codes

- 409 Conflict: `DuplicateEmailException`
- 500 Internal Server Error: unhandled exception

### Changelog

- Current response body is plain text.

## Endpoint: POST /api/v1/users/login

### Endpoint Summary

Logs in a user and creates a server-side session.

### Authentication

No existing session required.

### Request Body

```json
{
  "email": "seller@example.com",
  "password": "password1234"
}
```

### Response Body

HTTP 200, plain text:

```text
로그인이 성공적으로 완료되었습니다.
```

### Error Codes

- 401 Unauthorized: `LoginFailedException`
- 500 Internal Server Error: unhandled exception

### Changelog

- Frontend must keep and send the session cookie for protected endpoints.

## Endpoint: POST /api/v1/auctions

### Endpoint Summary

Creates an auction for the logged-in user.

### Authentication

Requires session (`@RequireLogin`).

### Request Body

```json
{
  "title": "Vintage Camera",
  "description": "Limited production camera in excellent condition.",
  "startPrice": 100000,
  "endTime": "2026-06-01T18:00:00"
}
```

### Response Body

HTTP 201, plain text:

```text
경매가 성공적으로 등록되었습니다.
```

### Error Codes

- 400 Bad Request: `InvalidEndTimeException`
- 400 Bad Request: `InvalidPriceException`
- 401 Unauthorized: `LoginRequiredException`
- 404 Not Found: `UserNotFoundException`
- 500 Internal Server Error: unhandled exception

### Changelog

- Seller ID is read from session attribute `LOGIN_USER`; do not send it in the body.

## Endpoint: GET /api/v1/auctions

### Endpoint Summary

Lists auctions with pagination.

### Authentication

No session required.

### Query Parameters

- `page`: zero-based page number, default `0`
- `size`: page size, default `10`
- `sort`: optional Spring sort expression. Default is `id,DESC`.

### Request Body

None.

### Response Body

HTTP 200:

```json
{
  "content": [
    {
      "id": 1,
      "title": "Vintage Camera",
      "currentPrice": 120000,
      "endTime": "2026-06-01T18:00:00",
      "status": "ON_SALE"
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 10,
    "sort": {
      "empty": false,
      "sorted": true,
      "unsorted": false
    },
    "offset": 0,
    "paged": true,
    "unpaged": false
  },
  "last": true,
  "totalElements": 1,
  "totalPages": 1,
  "size": 10,
  "number": 0,
  "sort": {
    "empty": false,
    "sorted": true,
    "unsorted": false
  },
  "first": true,
  "numberOfElements": 1,
  "empty": false
}
```

### Error Codes

- 500 Internal Server Error: unhandled exception

### Changelog

- Uses Spring Data `Page` response shape.

## Endpoint: GET /api/v1/auctions/{auctionId}

### Endpoint Summary

Gets one auction's detail.

### Authentication

No session required.

### Request Body

None.

### Response Body

HTTP 200:

```json
{
  "id": 1,
  "title": "Vintage Camera",
  "description": "Limited production camera in excellent condition.",
  "startPrice": 100000,
  "currentPrice": 120000,
  "endTime": "2026-06-01T18:00:00",
  "status": "ON_SALE",
  "sellerNickname": "rareSeller"
}
```

### Error Codes

- 404 Not Found: `AuctionNotFoundException`
- 500 Internal Server Error: unhandled exception

### Changelog

- Does not include seller ID or highest bidder details.

## Endpoint: PUT /api/v1/auctions/{auctionId}

### Endpoint Summary

Updates an auction's title and description.

### Authentication

Requires session (`@RequireLogin`).

### Request Body

```json
{
  "title": "Vintage Camera - Updated",
  "description": "Updated item description."
}
```

### Response Body

HTTP 200, plain text:

```text
경매글이 수정되었습니다.
```

### Error Codes

- 401 Unauthorized: `LoginRequiredException`
- 403 Forbidden: `UnauthorizedAccessException`
- 404 Not Found: `AuctionNotFoundException`
- 500 Internal Server Error: unhandled exception

### Changelog

- Only `title` and `description` are mutable through this endpoint.

## Endpoint: DELETE /api/v1/auctions/{auctionId}

### Endpoint Summary

Soft-deletes an auction by changing its status to `DELETED`.

### Authentication

Requires session (`@RequireLogin`).

### Request Body

None.

### Response Body

HTTP 200, plain text:

```text
경매글이 삭제 처리되었습니다.
```

### Error Codes

- 401 Unauthorized: `LoginRequiredException`
- 403 Forbidden: `UnauthorizedAccessException`
- 404 Not Found: `AuctionNotFoundException`
- 500 Internal Server Error: unhandled exception

### Changelog

- This is a soft delete; deleted auctions remain in the database with status `DELETED`.

## Endpoint: GET /api/v1/auctions/{auctionId}/bids

### Endpoint Summary

Lists bid history for an auction with pagination.

### Authentication

No session required.

### Query Parameters

- `page`: zero-based page number, default `0`
- `size`: page size, default `10`
- `sort`: optional Spring sort expression. Default is `bidPrice,DESC`.

### Request Body

None.

### Response Body

HTTP 200:

```json
{
  "content": [
    {
      "bidId": 10,
      "bidderNickname": "buyerOne",
      "bidPrice": 120000,
      "bidTime": "2026-05-01T14:30:00"
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 10,
    "sort": {
      "empty": false,
      "sorted": true,
      "unsorted": false
    },
    "offset": 0,
    "paged": true,
    "unpaged": false
  },
  "last": true,
  "totalElements": 1,
  "totalPages": 1,
  "size": 10,
  "number": 0,
  "sort": {
    "empty": false,
    "sorted": true,
    "unsorted": false
  },
  "first": true,
  "numberOfElements": 1,
  "empty": false
}
```

### Error Codes

- 404 Not Found: `AuctionNotFoundException`
- 500 Internal Server Error: unhandled exception

### Changelog

- Uses Spring Data `Page` response shape.

## Endpoint: POST /api/v1/auctions/{auctionId}/bids

### Endpoint Summary

Places a bid on an auction.

### Authentication

Currently no session required because `@RequireLogin` is commented out in `BidController` TEST MODE.

Production intent: requires session (`@RequireLogin`) and bidder ID should come from `LOGIN_USER`.

### Request Body

Current TEST MODE contract:

```json
{
  "bidPrice": 130000,
  "bidderId": 2
}
```

Production-intended body after restoring session auth:

```json
{
  "bidPrice": 130000
}
```

### Response Body

HTTP 200, plain text:

```text
입찰이 완료되었습니다.
```

### Error Codes

- 400 Bad Request: `AuctionClosedException`
- 400 Bad Request: `SelfBidException`
- 400 Bad Request: `ConsecutiveBidException`
- 400 Bad Request: `LowBidPriceException`
- 401 Unauthorized: `LoginRequiredException` after `@RequireLogin` is restored
- 404 Not Found: `AuctionNotFoundException`
- 404 Not Found: `UserNotFoundException`
- 500 Internal Server Error: unhandled exception or lock processing failure

### Changelog

- Current backend reads `bidderId` from the request body for testing.
- Frontend should treat `bidderId` as temporary and remove it once session-based bidding is restored.
