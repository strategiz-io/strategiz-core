#!/bin/bash

###############################################################################
# GCP Billing Setup Script for Strategiz
#
# This script automates the setup of GCP Billing API credentials
# for the Strategiz operating costs dashboard.
#
# Prerequisites:
# - gcloud CLI installed and authenticated
# - Project: strategiz-io
# - Billing admin permissions
# - Vault running and accessible
###############################################################################

set -e  # Exit on error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
PROJECT_ID="strategiz-io"
SERVICE_ACCOUNT_NAME="strategiz-billing"
SERVICE_ACCOUNT_DISPLAY_NAME="Strategiz Billing API Access"
KEY_FILE="$HOME/strategiz-billing-key.json"

###############################################################################
# Helper Functions
###############################################################################

print_header() {
    echo -e "\n${BLUE}========================================${NC}"
    echo -e "${BLUE}$1${NC}"
    echo -e "${BLUE}========================================${NC}\n"
}

print_success() {
    echo -e "${GREEN}✓ $1${NC}"
}

print_error() {
    echo -e "${RED}✗ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠ $1${NC}"
}

print_info() {
    echo -e "${BLUE}ℹ $1${NC}"
}

confirm() {
    read -p "$1 (y/n): " -n 1 -r
    echo
    [[ $REPLY =~ ^[Yy]$ ]]
}

###############################################################################
# Main Setup Functions
###############################################################################

check_prerequisites() {
    print_header "Checking Prerequisites"

    # Check gcloud
    if ! command -v gcloud &> /dev/null; then
        print_error "gcloud CLI not found. Install from: https://cloud.google.com/sdk/docs/install"
        exit 1
    fi
    print_success "gcloud CLI installed"

    # Check current project
    CURRENT_PROJECT=$(gcloud config get-value project 2>/dev/null)
    if [ "$CURRENT_PROJECT" != "$PROJECT_ID" ]; then
        print_warning "Current project is $CURRENT_PROJECT, not $PROJECT_ID"
        if confirm "Switch to $PROJECT_ID?"; then
            gcloud config set project $PROJECT_ID
            print_success "Switched to project $PROJECT_ID"
        else
            print_error "Aborted. Please set project to $PROJECT_ID manually."
            exit 1
        fi
    else
        print_success "Project is set to $PROJECT_ID"
    fi

    # Check authentication
    if ! gcloud auth list --filter=status:ACTIVE --format="value(account)" &> /dev/null; then
        print_error "Not authenticated. Run: gcloud auth login"
        exit 1
    fi
    print_success "Authenticated with gcloud"
}

enable_apis() {
    print_header "Enabling Required APIs"

    APIS=(
        "cloudbilling.googleapis.com"
        "cloudresourcemanager.googleapis.com"
        "cloudasset.googleapis.com"
        "monitoring.googleapis.com"
    )

    for api in "${APIS[@]}"; do
        print_info "Enabling $api..."
        if gcloud services enable $api --project=$PROJECT_ID 2>/dev/null; then
            print_success "Enabled $api"
        else
            print_warning "$api might already be enabled or enable failed"
        fi
    done
}

setup_billing_export() {
    print_header "Setting Up Billing Export (Optional)"

    print_info "Billing export to BigQuery provides detailed cost analysis."
    print_info "You can also skip this and use the Cloud Billing API directly."

    if ! confirm "Set up BigQuery billing export?"; then
        print_info "Skipping BigQuery export. Will use Cloud Billing API only."
        USE_BIGQUERY=false
        return
    fi

    USE_BIGQUERY=true

    print_info "Please complete these manual steps:"
    echo "1. Go to: https://console.cloud.google.com/billing"
    echo "2. Select your billing account"
    echo "3. Click 'Billing export' → 'EDIT SETTINGS'"
    echo "4. Create or select dataset: billing_export"
    echo "5. Location: US (multi-region)"
    echo "6. Click 'SAVE'"
    echo ""

    read -p "Press Enter when completed..."

    print_info "Note: Billing data export has a 24-hour delay for the first export."
}

create_service_account() {
    print_header "Creating Service Account"

    # Check if service account already exists
    if gcloud iam service-accounts describe ${SERVICE_ACCOUNT_NAME}@${PROJECT_ID}.iam.gserviceaccount.com &>/dev/null; then
        print_warning "Service account already exists: ${SERVICE_ACCOUNT_NAME}@${PROJECT_ID}.iam.gserviceaccount.com"
        if ! confirm "Use existing service account?"; then
            print_error "Aborted. Delete existing service account or use a different name."
            exit 1
        fi
    else
        print_info "Creating service account: $SERVICE_ACCOUNT_NAME"
        gcloud iam service-accounts create $SERVICE_ACCOUNT_NAME \
            --display-name="$SERVICE_ACCOUNT_DISPLAY_NAME" \
            --description="Service account for accessing GCP billing data and infrastructure costs" \
            --project=$PROJECT_ID
        print_success "Service account created"
    fi

    SERVICE_ACCOUNT_EMAIL="${SERVICE_ACCOUNT_NAME}@${PROJECT_ID}.iam.gserviceaccount.com"
    print_success "Service Account: $SERVICE_ACCOUNT_EMAIL"
}

grant_permissions() {
    print_header "Granting IAM Permissions"

    ROLES=(
        "roles/billing.viewer"
        "roles/cloudasset.viewer"
        "roles/monitoring.viewer"
    )

    # Add BigQuery roles if using BigQuery export
    if [ "$USE_BIGQUERY" = true ]; then
        ROLES+=("roles/bigquery.dataViewer" "roles/bigquery.jobUser")
    fi

    for role in "${ROLES[@]}"; do
        print_info "Granting $role..."
        gcloud projects add-iam-policy-binding $PROJECT_ID \
            --member="serviceAccount:$SERVICE_ACCOUNT_EMAIL" \
            --role="$role" \
            --condition=None \
            >/dev/null 2>&1
        print_success "Granted $role"
    done
}

create_service_account_key() {
    print_header "Creating Service Account Key"

    if [ -f "$KEY_FILE" ]; then
        print_warning "Key file already exists: $KEY_FILE"
        if ! confirm "Overwrite existing key?"; then
            print_info "Using existing key file"
            return
        fi
        rm -f "$KEY_FILE"
    fi

    print_info "Creating and downloading key..."
    gcloud iam service-accounts keys create "$KEY_FILE" \
        --iam-account="$SERVICE_ACCOUNT_EMAIL" \
        --project=$PROJECT_ID

    print_success "Key saved to: $KEY_FILE"
    print_warning "SECURITY: Keep this key secure! It grants access to billing data."
}

get_billing_account() {
    print_header "Getting Billing Account ID"

    print_info "Available billing accounts:"
    gcloud billing accounts list

    echo ""
    read -p "Enter your Billing Account ID (format: XXXXXX-XXXXXX-XXXXXX): " BILLING_ACCOUNT_ID

    if [ -z "$BILLING_ACCOUNT_ID" ]; then
        print_error "Billing Account ID is required"
        exit 1
    fi

    print_success "Using Billing Account: $BILLING_ACCOUNT_ID"
}

get_bigquery_table() {
    if [ "$USE_BIGQUERY" != true ]; then
        BIGQUERY_DATASET=""
        BIGQUERY_TABLE=""
        return
    fi

    print_header "Getting BigQuery Table Name"

    print_info "Finding billing export table..."

    # Try to find the table automatically
    TABLES=$(bq ls -n 100 billing_export 2>/dev/null | grep gcp_billing_export | awk '{print $1}' || true)

    if [ -z "$TABLES" ]; then
        print_warning "Could not find billing export table automatically."
        print_info "Common format: gcp_billing_export_v1_XXXXXX_XXXXXX_XXXXXX"
        read -p "Enter BigQuery table name: " BIGQUERY_TABLE
    else
        echo "$TABLES"
        read -p "Enter table name from above (or press Enter for first one): " BIGQUERY_TABLE
        if [ -z "$BIGQUERY_TABLE" ]; then
            BIGQUERY_TABLE=$(echo "$TABLES" | head -n 1)
        fi
    fi

    BIGQUERY_DATASET="billing_export"
    print_success "Using table: $BIGQUERY_DATASET.$BIGQUERY_TABLE"
}

store_in_vault() {
    print_header "Storing Credentials in Vault"

    print_info "Choose Vault environment:"
    echo "1) Production (https://strategiz-vault-43628135674.us-east1.run.app)"
    echo "2) Local Development (http://localhost:8200)"
    echo "3) Both"
    read -p "Choice (1/2/3): " vault_choice

    case $vault_choice in
        1|3)
            print_info "Configuring Production Vault..."
            export VAULT_ADDR="https://strategiz-vault-43628135674.us-east1.run.app"
            read -p "Enter Production Vault Token: " VAULT_TOKEN
            export VAULT_TOKEN

            store_vault_secrets "production"
            ;;
    esac

    case $vault_choice in
        2|3)
            print_info "Configuring Local Vault..."
            export VAULT_ADDR="http://localhost:8200"
            export VAULT_TOKEN="root"

            store_vault_secrets "local"
            ;;
    esac
}

store_vault_secrets() {
    local env=$1

    # Encode service account key as base64
    CREDENTIALS_BASE64=$(cat "$KEY_FILE" | base64)

    print_info "Storing secrets in $env Vault..."

    if [ "$USE_BIGQUERY" = true ]; then
        vault kv put secret/strategiz/gcp-billing \
            project-id="$PROJECT_ID" \
            billing-account-id="$BILLING_ACCOUNT_ID" \
            credentials="$CREDENTIALS_BASE64" \
            bigquery-dataset="$BIGQUERY_DATASET" \
            bigquery-table="$BIGQUERY_TABLE" \
            use-bigquery=true \
            use-billing-api=true
    else
        vault kv put secret/strategiz/gcp-billing \
            project-id="$PROJECT_ID" \
            billing-account-id="$BILLING_ACCOUNT_ID" \
            credentials="$CREDENTIALS_BASE64" \
            use-bigquery=false \
            use-billing-api=true
    fi

    print_success "Credentials stored in $env Vault"

    # Verify
    print_info "Verifying secrets..."
    if vault kv get secret/strategiz/gcp-billing >/dev/null 2>&1; then
        print_success "Secrets verified successfully"
    else
        print_error "Failed to verify secrets"
    fi
}

print_summary() {
    print_header "Setup Complete!"

    echo -e "${GREEN}✓ GCP Billing API setup completed successfully!${NC}\n"

    echo "Summary:"
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo "Project:              $PROJECT_ID"
    echo "Service Account:      $SERVICE_ACCOUNT_EMAIL"
    echo "Key File:             $KEY_FILE"
    echo "Billing Account:      $BILLING_ACCOUNT_ID"
    if [ "$USE_BIGQUERY" = true ]; then
        echo "BigQuery Dataset:     $BIGQUERY_DATASET"
        echo "BigQuery Table:       $BIGQUERY_TABLE"
    else
        echo "BigQuery Export:      Not configured (using Billing API only)"
    fi
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

    echo -e "\n${BLUE}Next Steps:${NC}"
    echo "1. Restart your backend application"
    echo "2. Navigate to: https://console.strategiz.io/costs"
    echo "3. You should see real cost data instead of demo data"
    echo ""
    echo "For local testing:"
    echo "  cd /Users/cuztomizer/Documents/GitHub/strategiz-core"
    echo "  export VAULT_TOKEN=root"
    echo "  mvn spring-boot:run -pl application-api -Dspring.profiles.active=dev"
    echo ""

    print_warning "SECURITY REMINDER:"
    echo "  - Keep $KEY_FILE secure"
    echo "  - Rotate keys every 90 days"
    echo "  - Never commit keys to git"
    echo ""

    if [ "$USE_BIGQUERY" = true ]; then
        print_info "Note: BigQuery billing export has a 24-hour delay for initial data."
        print_info "      You may see limited data for the first day."
    fi
}

###############################################################################
# Main Execution
###############################################################################

main() {
    print_header "Strategiz GCP Billing Setup"

    check_prerequisites
    enable_apis
    setup_billing_export
    create_service_account
    grant_permissions
    create_service_account_key
    get_billing_account
    get_bigquery_table
    store_in_vault
    print_summary

    print_success "All done! 🎉"
}

# Run main function
main
