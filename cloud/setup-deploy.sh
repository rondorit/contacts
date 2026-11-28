#!/usr/bin/env bash
# One-time setup (run in Google Cloud Shell): lets GitHub Actions of
# rondorit/contacts deploy Firebase (functions + rules) without any key.
# Uses Workload Identity Federation, restricted to this one repository.
set -euo pipefail
PROJECT=bekis-e82cd
REPO=rondorit/contacts
SA=github-deploy
POOL=github
PROVIDER=github-oidc

gcloud config set project "$PROJECT" >/dev/null
NUM=$(gcloud projects describe "$PROJECT" --format='value(projectNumber)')
SA_EMAIL="$SA@$PROJECT.iam.gserviceaccount.com"

echo "== APIs"
gcloud services enable iam.googleapis.com iamcredentials.googleapis.com \
  sts.googleapis.com cloudresourcemanager.googleapis.com \
  cloudfunctions.googleapis.com cloudbuild.googleapis.com \
  artifactregistry.googleapis.com run.googleapis.com eventarc.googleapis.com \
  pubsub.googleapis.com firebaseextensions.googleapis.com \
  cloudscheduler.googleapis.com secretmanager.googleapis.com \
  fcm.googleapis.com firestore.googleapis.com

echo "== Service account"
gcloud iam service-accounts describe "$SA_EMAIL" >/dev/null 2>&1 || \
  gcloud iam service-accounts create "$SA" --display-name="GitHub deploy (On IT)"

for ROLE in roles/firebase.admin roles/cloudfunctions.admin roles/run.admin \
  roles/iam.serviceAccountUser roles/serviceusage.serviceUsageConsumer \
  roles/artifactregistry.admin roles/cloudbuild.builds.editor \
  roles/cloudscheduler.admin roles/secretmanager.viewer \
  roles/datastore.indexAdmin roles/firebaserules.admin; do
  gcloud projects add-iam-policy-binding "$PROJECT" \
    --member="serviceAccount:$SA_EMAIL" --role="$ROLE" \
    --condition=None --quiet >/dev/null
  echo "  $ROLE"
done

echo "== Workload identity pool"
gcloud iam workload-identity-pools describe "$POOL" --location=global >/dev/null 2>&1 || \
  gcloud iam workload-identity-pools create "$POOL" --location=global \
    --display-name="GitHub Actions"

gcloud iam workload-identity-pools providers describe "$PROVIDER" \
  --workload-identity-pool="$POOL" --location=global >/dev/null 2>&1 || \
  gcloud iam workload-identity-pools providers create-oidc "$PROVIDER" \
    --workload-identity-pool="$POOL" --location=global \
    --issuer-uri="https://token.actions.githubusercontent.com" \
    --attribute-mapping="google.subject=assertion.sub,attribute.repository=assertion.repository" \
    --attribute-condition="assertion.repository=='$REPO'"

gcloud iam service-accounts add-iam-policy-binding "$SA_EMAIL" \
  --role=roles/iam.workloadIdentityUser \
  --member="principalSet://iam.googleapis.com/projects/$NUM/locations/global/workloadIdentityPools/$POOL/attribute.repository/$REPO" \
  --quiet >/dev/null

echo
echo "DONE"
echo "provider: projects/$NUM/locations/global/workloadIdentityPools/$POOL/providers/$PROVIDER"
echo "account:  $SA_EMAIL"
