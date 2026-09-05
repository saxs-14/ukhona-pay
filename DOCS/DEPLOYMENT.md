# Deployment: Vercel (frontend) + Google Cloud Run (backend)

Same model as Vercel already uses: GitHub is the source of truth, a push to
`main` triggers a build and deploy, no manual upload step on either side.

## What's already done

- **GCP project** `ukhona-pay-backend` created, linked to the same Google
  account already used for other projects (`mamagauphathu@gmail.com`).
- **Service account** `github-deployer@ukhona-pay-backend.iam.gserviceaccount.com`
  created with `run.admin`, `iam.serviceAccountUser`, `artifactregistry.writer`,
  and `storage.admin` - just enough to build and deploy, nothing broader.
- **GitHub Actions workflow** at `.github/workflows/deploy-backend.yml` -
  builds the backend via Cloud Build, pushes the image to Artifact Registry,
  deploys to Cloud Run. Triggers only on pushes that touch `BACKEND/**`, so a
  frontend-only push doesn't redeploy the backend for nothing.
- **`BACKEND/Dockerfile`** - multi-stage build (Maven build stage, slim JRE
  runtime stage).
- **`application.yml`** - `server.port` and the datasource now read from
  `$PORT` / `$DATABASE_URL` / `$DB_USERNAME` / `$DB_PASSWORD` env vars, with
  the exact current local-dev values as defaults, so nothing about local
  `mvn spring-boot:run` changed.
- **`FRONTEND/src/api/client.js`** - API base URL now reads
  `VITE_API_BASE_URL` at build time, falling back to the existing relative
  `/api` (used by the Vite dev proxy locally). Needs setting in Vercel once
  the backend has a real URL (see below).
- **CORS** (`SecurityConfig.java`) already allows `https://*.vercel.app` -
  no change needed there.
- **GitHub secrets already set** on `saxs-14/ukhona-pay`: `GCP_SA_KEY`,
  `GCP_PROJECT_ID`, `JWT_SECRET` (freshly generated, not the demo default),
  `FRONTEND_BASE_URL` (`https://ukhona-pay.vercel.app`).
- **Database: Neon**, project region us-east-2. `schema.sql` already loaded
  (8 tables, verified via `\dt`). `DATABASE_URL` (JDBC form, pointed at
  Neon's pooled endpoint since Cloud Run can run several instances at once,
  each opening its own connection pool - the pooler avoids exhausting
  Neon's connection limit), `DB_USERNAME`, `DB_PASSWORD` are set as GitHub
  secrets. The raw connection string only ever touched a local temp file
  used to test the connection and load the schema, then got deleted -
  never printed anywhere it'd stick around.

## What's left - one thing only you can do

### Link billing to the new project (one click, ~30 seconds)

Every API this needs (Cloud Run, Artifact Registry, Cloud Build) is blocked
until a billing account is attached to `ukhona-pay-backend` - this is a GCP
requirement even for usage that stays entirely within the free tier, as an
anti-abuse measure. Your Google account already has a billing account
attached to other projects (EduConnectZA and others), so this is picking it
from a dropdown, not entering a new card:

**https://console.cloud.google.com/billing/linkedaccount?project=ukhona-pay-backend**

Cloud Run's free tier (2 million requests/month, 360,000 GB-seconds memory,
180,000 vCPU-seconds/month) comfortably covers hackathon-level traffic - this
should not generate a real charge.

## After that's done

1. Push anything touching `BACKEND/**` to `main` (or run the workflow
   manually from the Actions tab) - this builds and deploys automatically.
2. Note the printed Cloud Run URL (last step of the workflow, also visible
   via `gcloud run services describe ukhona-pay-backend --region=us-central1
   --format="value(status.url)"`).
3. Set `VITE_API_BASE_URL` in the Vercel project's environment variables to
   `<that Cloud Run URL>/api`, then redeploy the frontend
   (`vercel --prod` from the repo root, or push to `main` if Vercel's Git
   integration is later fixed to auto-promote - see the note in git history
   about the Preview-vs-Production issue that was already fixed once).
4. Verify: `curl https://<cloud-run-url>/api/taxi-associations` should
   return `[]` (or real data), not a connection error.

## Rollback / cost safety

- Cloud Run scales to zero when idle (`--min-instances=0` in the workflow) -
  no cost while nobody's using it, beyond negligible storage for the pushed
  container images.
- To tear everything down: `gcloud projects delete ukhona-pay-backend`
  removes the project, the Cloud Run service, and stops any further charges
  from it. The GitHub secrets can be removed independently at any time via
  `gh secret remove <name> --repo saxs-14/ukhona-pay`.
