@echo off
REM ── Local launcher. Contains secrets → gitignored, do NOT commit. ──
REM In production these come from a secret manager, not a file.
set "JAVA_HOME=C:\Program Files\Java\jdk-25.0.3"
set "MAVEN_HOME=%~dp0.maven\apache-maven-3.9.6"
set "PATH=%JAVA_HOME%\bin;%MAVEN_HOME%\bin;%PATH%"
set "DB_URL=jdbc:postgresql://db.vtcgcjtroxvtsjjfattt.supabase.co:6543/postgres"
set "DB_USERNAME=finbridge_svc"
set "DB_PASSWORD=FwSj3auZDzKx4h_l1lLPjwhuQMwcdSBo"
set "JWT_SECRET=uHkL6kNLU269MVzZ54JfUgzJjO_gVViezr9nkrGfFCdpYKBqWfKM2038J8b_8rZc"
set "MAIL_USERNAME=manumanohar1027@gmail.com"
set "MAIL_PASSWORD=egkzkxmwjtsndqqn"
echo Starting FinBridge Spring Boot Backend on port 5000 (role: finbridge_svc)...
mvn clean spring-boot:run -DskipTests
