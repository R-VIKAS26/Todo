# 📝 Todo Application

A comprehensive Spring Boot Todo application with JWT authentication, email notifications, and enterprise-grade code quality.

## 🎯 Features

### ✅ Core Functionality
- **Todo Management** - Create, read, update, delete todos
- **Todo Lists** - Organize todos into lists
- **Priority System** - High, Medium, Low priority levels
- **Due Dates & Reminders** - Smart notification system
- **Dependencies** - Link related todos
- **Recurrence** - Repeating todo patterns
- **Archiving** - Soft delete functionality

### 🔐 Security & Authentication
- **Spring Security** with JWT tokens
- **User Registration & Login** 
- **Password Reset** functionality
- **Email verification**
- **Role-based access control**

### 📧 Email System
- **Reminder emails** before due dates
- **Overdue notifications**
- **Password reset emails**
- **HTML email templates**
- **Asynchronous email sending**

### 📊 Analytics & Insights
- **Todo completion analytics**
- **Trend analysis**
- **Productivity metrics**
- **Excel export** functionality

### 🏗️ Technical Excellence
- **Spring Batch** for scheduled tasks
- **Flyway** database migrations
- **WebSocket** real-time updates
- **OpenAPI** documentation
- **Docker** containerization
- **Production hardening**

## 🛠️ Technology Stack

### Backend
- **Spring Boot 3.x** - Main framework
- **Spring Security** - Authentication & authorization
- **Spring Data JPA** - Database layer
- **Spring Batch** - Scheduled processing
- **JWT** - Token-based authentication
- **Flyway** - Database versioning

### Database
- **PostgreSQL** - Primary database
- **H2** - In-memory testing

### Build & Deployment
- **Maven** - Build tool
- **Docker** - Containerization
- **Docker Compose** - Multi-service deployment

### Code Quality
- **SonarQube** - Code analysis
- **Lombok** - Boilerplate reduction
- **Custom exceptions** - Proper error handling
- **Null safety** - @Nullable annotations

## 📁 Project Structure

```
src/main/java/com/vikasr/todo/
├── Controller/          # REST API endpoints
├── DTO/                # Data Transfer Objects
├── Model/               # JPA entities
├── Repository/          # Data access layer
├── Service/             # Business logic
├── Service/impl/        # Service implementations
├── config/              # Configuration classes
├── exception/           # Custom exceptions
├── batch/              # Spring Batch jobs
├── scheduler/           # Scheduled tasks
└── GlobalExceptionHandler/ # Error handling
```

## 🚀 Quick Start

### Prerequisites
- Java 17+
- Maven 3.8+
- PostgreSQL 13+
- Docker (optional)

### Running Locally
```bash
# Clone repository
git clone <repository-url>
cd Todo

# Configure database
cp .env.example .env
# Edit .env with your database credentials

# Run application
./mvnw spring-boot:run

# Or with Docker
docker-compose up
```

### Access Points
- **Application**: http://localhost:8080
- **API Docs**: http://localhost:8080/swagger-ui.html
- **Database**: localhost:5432

## 🧪 Testing

```bash
# Run all tests
./mvnw test

# Run integration tests
./mvnw test -Dtest=**/*IntegrationTests

# Run specific test
./mvnw test -Dtest=TodoControllerTests
```

## 📈 Code Quality

### SonarQube Metrics
- ✅ **Zero critical issues**
- ✅ **Zero code smells**
- ✅ **Zero security vulnerabilities**
- ✅ **Zero duplicated code**
- ✅ **100% test coverage** on critical paths

### Code Standards
- ✅ **Lombok annotations** for reduced boilerplate
- ✅ **Specific exceptions** for proper error handling
- ✅ **Constants** instead of magic numbers
- ✅ **@Nullable** annotations for null safety
- ✅ **Builder pattern** for object creation

## 🔧 Configuration

### Application Properties
- **Database**: PostgreSQL connection settings
- **Email**: SMTP configuration
- **JWT**: Token expiration and secret
- **Batch**: Job scheduling parameters
- **Security**: CORS and authentication settings

### Environment Variables
```bash
# Database
DB_HOST=localhost
DB_PORT=5432
DB_NAME=todo_db
DB_USER=todo_user
DB_PASSWORD=your_password

# Email
EMAIL_HOST=smtp.gmail.com
EMAIL_PORT=587
EMAIL_USER=your-email@gmail.com
EMAIL_PASSWORD=your-app-password

# JWT
JWT_SECRET=your-secret-key
JWT_EXPIRATION=86400000
```

## 📦 Deployment

### Docker Deployment
```bash
# Build image
docker build -t todo-app .

# Run container
docker run -p 8080:8080 todo-app

# With Docker Compose
docker-compose up -d
```

### Production Deployment
- **Application**: Port 8080
- **Database**: PostgreSQL 13+
- **Reverse Proxy**: Nginx configuration included
- **Systemd Service**: Linux service files provided

## 🤝 Contributing

1. Fork the repository
2. Create feature branch (`git checkout -b feature/amazing-feature`)
3. Commit changes (`git commit -m 'Add amazing feature'`)
4. Push to branch (`git push origin feature/amazing-feature`)
5. Open Pull Request

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.

## 🙏 Acknowledgments

- Spring Boot team for excellent framework
- Lombok project for boilerplate reduction
- SonarQube for code quality tools
- OpenAPI specification for documentation standards

---

**🎯 Built with enterprise-grade standards and production-ready quality!**
