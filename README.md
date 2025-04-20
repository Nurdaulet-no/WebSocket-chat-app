# Real-Time Chat Application (Spring Boot + WebSocket) 🚀

A real-time chat application built with Spring Boot, WebSocket (STOMP), Spring Security (JWT), and PostgreSQL. Supports both group and private messaging.

## Features

- Secure authentication with JWT
- Real-time messaging via WebSockets/STOMP
- Group and private chats
- Message history persistence
- User management in chat rooms
- System notifications

## Tech Stack

- **Backend**: Java 17+, Spring Boot 3.x (Web, Security, WebSocket, Data JPA)
- **Database**: PostgreSQL (Flyway migrations)
- **Authentication**: JWT

## Setup

1. **Prerequisites**: JDK 17+, Maven, PostgreSQL

2. **Configure Database**:
    - Create a PostgreSQL database
    - Update `src/main/resources/application.properties` with your database details and JWT settings

3. **Run Application**:
   ```bash
   mvn clean package
   mvn spring-boot:run

   ```

4. Application runs at: `http://localhost:8080`

## API Overview

- **Auth**: `/api/auth/register`, `/api/auth/login`
- **Chats**:
    - List chats: `GET /api/chats`
    - Create group: `POST /api/chats/group`
    - Private chat: `POST /api/chats/private/{username}`
    - Messages: `GET /api/chats/{roomId}/messages`
    - Manage participants: `PUT|DELETE /api/chats/{roomId}/participants/{username}`

## WebSocket Usage

1. **Connect**: `ws://localhost:8080/ws` with JWT in Authorization header
2. **Send**: `/app/chat/{roomId}/sendMessage` with message content
3. **Subscribe**: `/topic/chats/{roomId}` to receive messages

## Future Enhancements

- Typing indicators
- Read receipts
- Message editing/deletion
- User search
- Docker support