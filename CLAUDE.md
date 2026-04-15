# CLAUDE.md

Guidance for Claude Code when working on this repository.

## Project

**QR Code Generator** — a small Spring Boot web app that generates QR codes
for common use cases (website, tel, SMS, email, WiFi, vCard, generic text)
and optionally embeds a logo in the center of the code.

## Tech Stack

- **Java 21** (target), built and run with the Java 25 toolchain in `.sdkmanrc`
- **Spring Boot 4.0.2** (web, thymeleaf, actuator)
- **Nayuki `qrcodegen` 1.8.0** for QR encoding
- **Thymeleaf** + vanilla JS for the UI (no build step for the frontend)
- **JUnit 5** + Spring MockMvc for tests (requires the `spring-boot-webmvc-test` artifact since SB4 relocated `AutoConfigureMockMvc`)
- **Maven wrapper** (`./mvnw`) — do not rely on a globally installed Maven

## Layout

```
src/main/java/com/nevenc/qrcodegenerator/
    App.java                  - Spring Boot entry point
    HomeController.java       - redirects / to /generator, serves the template
    QrController.java         - GET /qr and POST /qr (multipart, with logo)
    QrEncoder.java            - static helpers: QR generation + logo composite
    LogoValidator.java        - PNG / size / dimension checks for uploads
    InvalidLogoException.java - typed exception mapped to HTTP 400
src/main/resources/
    application.properties
    templates/generator.html  - single-page form
    static/js/generator.js    - form handling + fetch POST to /qr
    static/css/styles.css
    static/images/            - placeholder + optional spring-boot-logo.png
src/test/java/com/nevenc/qrcodegenerator/
    AppTest.java              - context-loads smoke test
    LogoValidatorTest.java
    QrEncoderTest.java
    QrControllerTest.java     - MockMvc tests for GET and POST /qr
```

## Key Design Choices

- **Two endpoints, not one.** `GET /qr?text=...&scale=...&border=...` is
  preserved exactly as it was for backward compatibility with any existing
  callers. The new logo flow uses `POST /qr` with multipart form data.
- **Controller picks ECC, not encoder.** `QrEncoder` is a pure function: it
  takes the `Ecc` level as a parameter. The controller uses `Ecc.HIGH`
  whenever a logo is enabled (~30% obstruction tolerance covers the ~22%
  round cutout) and `Ecc.MEDIUM` otherwise.
- **Logo sizing is hard-coded.** Round white cutout at ~22% of QR width,
  logo inscribed in that circle. Not exposed as a tunable — keeps the UI
  simple and keeps us inside HIGH ECC's scannable range.
- **Default logo is self-healing.** At startup `QrController` tries to load
  `classpath:static/images/spring-boot-logo.png`. If it's missing or
  unreadable, it generates a 128×128 green "S" placeholder in memory. Drop
  a real PNG at that path to override.
- **Validation failures are typed.** `LogoValidator` throws
  `InvalidLogoException` with a user-facing message; a `@ExceptionHandler`
  on `QrController` maps it to `400 text/plain;charset=UTF-8`. The JS reads
  `response.text()` and renders it inline.
- **UTF-8 charset is explicit** on the 400 response — the `×` character in
  the dimension error message is outside ISO-8859-1's printable range, so
  Spring needs the charset spelled out or it mangles the byte.
- **Frontend uses native `<details>`** for the Branding disclosure — no JS
  state machine for expand/collapse, and matches the existing vanilla-JS
  style of the project. The file input is always visible inside the open
  details element; the checkbox controls whether the logo is sent, not the
  visibility of the picker.

## Common Commands

```bash
./mvnw spring-boot:run          # run the app locally (http://localhost:8080)
./mvnw -q test                  # run all tests
./mvnw -q test -Dtest=QrControllerTest   # run a single test class
./mvnw clean package            # build the jar
```

## Spec and Plan

- Design: `docs/superpowers/specs/2026-04-14-qr-logo-design.md`
- Plan:   `docs/superpowers/plans/2026-04-14-qr-logo.md`
