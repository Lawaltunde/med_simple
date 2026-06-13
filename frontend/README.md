# MedSimple — Frontend

React + TypeScript single-page app for patients and clinic staff. Built with Vite,
styled with Tailwind v4, served in production by the Spring Boot backend.

## Folder layout

```
frontend/
├── public/                  Static assets copied verbatim into the build
│   └── banners_logo/        Brand images (logo + banners, SVG + PNG) — SOURCE OF TRUTH
├── src/
│   ├── components/          Shared, reusable components
│   │   └── ui/              Design-system primitives (Button, Input, Card…)
│   ├── pages/               Route-level screens (LandingPage, LoginPage…)
│   ├── lib/                 Framework-agnostic helpers (utils, api client)
│   ├── App.tsx              Router
│   ├── main.tsx             Entry point (React root + providers)
│   └── index.css            Design tokens (@theme) + base styles
├── index.html              Vite HTML entry
├── vite.config.ts          Build → ../src/main/resources/static, /api dev proxy
└── package.json
```

As feature areas grow, group route screens and their local components/hooks under
`src/features/<area>/` (e.g. `features/patient/`, `features/clinician/`). Anything
shared across features stays in `src/components/` and `src/lib/`.

## Brand assets

The logo and banners live in `public/banners_logo/`. They are the **source of truth**
and are referenced by absolute path, e.g. `/banners_logo/medsimple-logo.svg`. Vite copies
`public/` into the build output, so the same paths work in production. **Do not** put brand
assets in the backend's `static/` directory — that folder is 100% generated.

## Design system

All color/spacing/radius/type tokens are defined once in `src/index.css` under `@theme`,
derived from the brand (emerald green `#16876A`, mint accent `#7FE3C4`). Components consume
tokens via Tailwind classes (`bg-brand-600`, `text-ink`…). Change a token in one place and
the whole app follows.

## Develop

```bash
# 1. Run the backend (separate terminal, from the repo root)
./mvnw spring-boot:run -Dspring-boot.run.profiles=local

# 2. Run the frontend dev server (hot reload; proxies /api to :8080)
cd frontend
npm install
npm run dev
```

## Production build

The SPA is bundled into the backend JAR via a Maven profile — no build artifacts are
committed to git:

```bash
# from the repo root — builds the SPA into src/main/resources/static, then the JAR
./mvnw clean package -Pfrontend
```

Or build the SPA on its own:

```bash
cd frontend && npm run build   # outputs to ../src/main/resources/static
```
