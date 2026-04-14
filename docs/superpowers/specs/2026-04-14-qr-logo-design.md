# QR Code Logo Feature — Design

**Status:** Approved
**Date:** 2026-04-14
**Author:** Neven Cvetkovic (with Claude)

## Summary

Add an optional centered logo to generated QR codes. Users enable the
feature via a collapsible *Branding* section on the generator page. When
enabled, they can upload a PNG logo, or fall back to a bundled Spring Boot
logo if no file is provided. The logo is composited onto the QR as a round
white cutout at ~22% of the QR width, and the encoder automatically uses
HIGH error correction whenever the logo is enabled so the QR stays
scannable.

## Goals

- Let users embed a brand logo in the center of a generated QR code.
- Ship a sensible default (Spring Boot logo) so the feature works with
  zero setup.
- Keep the existing `GET /qr` endpoint and its behavior completely
  unchanged — no regression risk for current callers.
- Stay within the project's existing stack (Spring Boot, Thymeleaf,
  vanilla JS, Nayuki `qrcodegen`). No new runtime dependencies.

## Non-Goals

- Logo sizing/shape controls exposed in the UI (hard-coded to round,
  ~22% width).
- Foreground/background color customization.
- Server-side persistence of uploaded logos across requests.
- Exposing the new endpoint as a documented public API (may come later).
- Non-PNG input formats (JPEG, SVG, WebP).

## Decisions

| Decision | Choice | Rationale |
|---|---|---|
| API scope | Web UI only; add `POST /qr` (multipart), keep existing `GET /qr` untouched | Lowest backward-compat risk |
| Logo shape & size | Round white cutout, ~22% of QR width | Recognizable without swamping the code |
| Error correction | Auto-bump to `Ecc.HIGH` whenever logo is enabled; `Ecc.MEDIUM` otherwise | HIGH tolerates ~30% obstruction, needed for a ~22% logo |
| Default logo | Bundled `src/main/resources/static/images/spring-boot-logo.png` | Works out-of-the-box, zero per-request I/O (loaded once at startup) |
| Upload constraints | PNG only; ≤ 1 MB; ≤ 1024×1024 pixels | Simple, safe limits; room to grow |
| Bad upload handling | `HTTP 400` with a plain-text error message; UI shows error inline next to the file field | Silent fallbacks hide bugs |
| UI placement | Collapsible `<details>` "Branding" section above the Generate button | Matches existing form style; progressive disclosure for power users |
| Frontend delivery | `fetch` POST multipart → blob → object URL → swap `<img>` src | Required to send binary upload; works offline from any CDN |

## Architecture

### Components

**`QrEncoder`** (existing class, extended)
Add a new overload that takes all current parameters **plus** a non-null
`java.awt.image.BufferedImage logo`. Existing overloads are untouched
and continue to produce logo-free QRs. The new overload is a pure
function — it does **not** decide the error-correction level; the caller
passes it in (same as the existing overloads). The controller is
responsible for choosing `Ecc.HIGH` when the logo is enabled.

Steps performed by the new overload:

1. Encodes the QR with the provided `Ecc` value.
2. Renders the QR with the existing pixel loop.
3. Draws a white filled circle of diameter ≈ 22% of the QR image width at
   the center using `Graphics2D` with antialiasing on.
4. Scales the supplied `BufferedImage` to fit a square inscribed in that
   circle and draws it centered on top.
5. Returns the composited `BufferedImage` or its PNG bytes.

**`QrController`** (existing class, extended)
Add a new handler:

```java
@PostMapping(value = "/qr", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
ResponseEntity<byte[]> generate(
    @RequestParam(defaultValue = DEFAULT_TEXT) String text,
    @RequestParam(defaultValue = "8") int scale,
    @RequestParam(defaultValue = "1") int border,
    @RequestParam(defaultValue = "false") boolean includeLogo,
    @RequestParam(required = false) MultipartFile logo)
```

Behavior:

- `includeLogo = false` → delegate to the existing no-logo code path
  (`Ecc.MEDIUM`, no composite). Behavior identical to GET today.
- `includeLogo = true` **and** no `logo` file → call the new logo
  overload with the pre-loaded default `BufferedImage` and `Ecc.HIGH`.
- `includeLogo = true` **and** `logo` file present → call
  `LogoValidator.validate(...)` then `ImageIO.read(...)` to obtain a
  `BufferedImage`, then call the new logo overload with it and `Ecc.HIGH`.

The default logo is loaded once during bean construction from
`classpath:static/images/spring-boot-logo.png` and cached in a final
field.

**`LogoValidator`** (new, small static-method class)
Runs the four checks in this order and throws `InvalidLogoException`
(new typed unchecked exception with a human-readable message) on the
first failure:

1. Content type is `image/png`.
2. `file.getSize() ≤ 1 MB` (1,048,576 bytes).
3. `ImageIO.read(...)` returns a non-null `BufferedImage` (guards against
   corrupt or mislabeled files).
4. Width ≤ 1024 and height ≤ 1024.

`QrController` catches `InvalidLogoException` in a `@ExceptionHandler` and
returns `400` with `Content-Type: text/plain` and the exception's message
as the body.

**Bundled asset** (new)
`src/main/resources/static/images/spring-boot-logo.png` — the official
Spring leaf logo. This file is not checked in by this plan; the
implementation plan will flag it as a manual step for the developer to
drop in a legally-sourced PNG before running the app.

### Frontend

**`generator.html`** — add a `<details>` block between the type-specific
fields and the Generate button:

```html
<details class="form-group branding">
  <summary>Branding</summary>
  <div class="branding-body">
    <label class="checkbox-row">
      <input type="checkbox" id="include-logo"> Include logo in center
    </label>
    <div id="logo-upload-row" class="upload-row" hidden>
      <input type="file" id="logo-file" accept="image/png">
      <div id="logo-error" class="error-text" hidden></div>
    </div>
  </div>
</details>
```

The upload row is hidden until the checkbox is checked.

**`generator.js`** — replace the current `img.src = "/qr?text=..."`
pattern with a multipart POST:

```js
async function generateQRCode() {
  const qrData = buildQrData();          // existing logic
  const form = new FormData();
  form.append('text', qrData);
  form.append('includeLogo', includeLogo.checked ? 'true' : 'false');
  if (includeLogo.checked && logoFile.files[0]) {
    form.append('logo', logoFile.files[0]);
  }
  const resp = await fetch('/qr', { method: 'POST', body: form });
  if (!resp.ok) {
    const msg = await resp.text();
    showLogoError(msg);
    return;
  }
  clearLogoError();
  const blob = await resp.blob();
  qrCodeImg.src = URL.createObjectURL(blob);
}
```

The checkbox `change` handler toggles the `hidden` attribute on
`#logo-upload-row` and clears any prior error.

**`styles.css`** — styles for `details.branding`, `.checkbox-row`,
`.upload-row`, `.error-text`. No layout changes elsewhere.

## Data Flow

### Logo enabled, user file

1. User opens Branding, ticks *Include logo*, picks a PNG.
2. JS builds `FormData{ text, scale=8, border=1, includeLogo=true, logo=File }`.
3. Browser POSTs to `/qr`.
4. Controller calls `LogoValidator.validate(file)` → `ImageIO.read(file)`
   → `BufferedImage`.
5. Controller calls `QrEncoder.generateQrCodeBytes(text, scale, border,
   LIGHT, DARK, Ecc.HIGH, logoImage)`.
6. Controller returns `200 image/png` with the PNG bytes.
7. JS converts response to blob, creates object URL, sets `<img>` src.

### Logo enabled, no file

Identical, except the controller uses its cached default
`BufferedImage` field instead of reading from the multipart request.
Zero disk I/O per request.

### Logo disabled

Controller short-circuits to the existing `QrEncoder.generateQrCodeBytes(
text, scale, border)` call. `Ecc.MEDIUM`, no composite. Behavior is
bit-for-bit identical to the current GET flow.

### Existing `GET /qr` callers

Completely unaffected. The handler, parameter list, and response bytes
are unchanged.

## Error Handling

| Failure | HTTP | Body | UI reaction |
|---|---|---|---|
| File > 1 MB | 400 | `Logo file must be 1 MB or smaller` | Error text below the file input; preview unchanged |
| Content-type not `image/png` or `ImageIO.read` returns null | 400 | `Logo must be a valid PNG image` | Same |
| Width or height > 1024 | 400 | `Logo must be 1024×1024 pixels or smaller` | Same |
| `text` missing / empty | 400 | `text is required` | Error near text field (reuse of existing div) |
| Unexpected server error | 500 | (existing behavior) | Generic error |

The JS reads non-OK responses via `resp.text()` and renders them in the
appropriate inline error div.

## Edge Cases

- **Transparent PNG:** the white circle is drawn before the logo, so
  transparent areas show white — matches QR background naturally.
- **Very long text + HIGH ECC + logo:** the QR still renders; `qrcodegen`
  picks a large enough version automatically. The ~22% logo cap keeps
  obstruction within HIGH's ~30% tolerance.
- **Checkbox on, file cleared mid-session:** `FormData` omits the file
  entry, server uses the default. Works without extra JS state.
- **Checkbox toggled off after upload:** JS does not append the file;
  server ignores `logo` entirely when `includeLogo=false`.
- **Multiple rapid clicks on Generate:** each call is independent; last
  successful response wins. No locking needed.

## Testing

- **`LogoValidatorTest`** — one accept case + one reject case per rule
  (wrong content type, oversized file, corrupt/mislabeled PNG,
  oversized dimensions).
- **`QrEncoderTest`** (new tests on the existing class):
  - Existing no-logo overloads still produce byte-identical output on a
    frozen input (locks in backward compatibility).
  - New logo overload, called with `Ecc.HIGH` and a fixed test PNG,
    produces an image whose center pixel is white (the cutout) and whose
    pixels at the finder pattern corners are still black (the QR itself
    survived).
  - New logo overload is a pure composite function: same text + same ECC
    + same logo always produces byte-identical output.
- **`QrControllerTest`** with `MockMvc`:
  - `GET /qr?text=foo` returns 200 `image/png` (backward compat).
  - `POST /qr` with `includeLogo=false` returns 200 `image/png` and the
    bytes match the GET response for the same text/scale/border.
  - `POST /qr` with `includeLogo=true` and no file returns 200 and the
    image differs from the no-logo version.
  - `POST /qr` with `includeLogo=true` and a valid PNG returns 200.
  - `POST /qr` with each invalid file case returns 400 and the expected
    error message body.

## Open Items for Implementation Plan

- Source the bundled Spring Boot logo PNG (manual step — licensing).
- Confirm `spring.servlet.multipart.max-file-size` in
  `application.properties` is ≥ 1 MB (Spring default is 1 MB — aligns
  with our limit).
- Decide whether to add a ZXing test dependency for scan verification, or
  stick to pixel assertions. Default: pixel assertions only.
