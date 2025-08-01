#!/bin/bash

# Pool Assistant - Keystore Setup Script
# This script helps you create and configure a release keystore

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

print_header() {
    echo -e "${BLUE}🔐 Pool Assistant Keystore Setup${NC}"
    echo "=================================="
    echo ""
}

print_step() {
    echo -e "${BLUE}🔹 $1${NC}"
}

print_success() {
    echo -e "${GREEN}✅ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

print_error() {
    echo -e "${RED}❌ $1${NC}"
}

print_info() {
    echo -e "${YELLOW}ℹ️  $1${NC}"
}

# Check if keytool is available
check_keytool() {
    if ! command -v keytool &> /dev/null; then
        print_error "keytool not found. Please install Java JDK."
        exit 1
    fi
}

# Generate new keystore
generate_keystore() {
    print_step "Creating new release keystore..."
    
    # Collect information
    echo "Please provide the following information for your keystore:"
    echo ""
    
    read -p "Keystore password (minimum 6 characters): " -s STORE_PASSWORD
    echo ""
    read -p "Confirm keystore password: " -s STORE_PASSWORD_CONFIRM
    echo ""
    
    if [ "$STORE_PASSWORD" != "$STORE_PASSWORD_CONFIRM" ]; then
        print_error "Passwords don't match!"
        exit 1
    fi
    
    if [ ${#STORE_PASSWORD} -lt 6 ]; then
        print_error "Password must be at least 6 characters long!"
        exit 1
    fi
    
    read -p "Key alias name (e.g., pool_assistant): " KEY_ALIAS
    read -p "Key password (can be same as store password): " -s KEY_PASSWORD
    echo ""
    
    if [ -z "$KEY_PASSWORD" ]; then
        KEY_PASSWORD="$STORE_PASSWORD"
    fi
    
    echo "Certificate information:"
    read -p "Your name (CN): " CERT_NAME
    read -p "Organization Unit (OU, e.g., Development): " CERT_OU
    read -p "Organization (O, e.g., Pool Assistant): " CERT_O
    read -p "City (L): " CERT_L
    read -p "State (ST): " CERT_ST
    read -p "Country code (C, e.g., ID): " CERT_C
    
    # Create the keystore
    KEYSTORE_FILE="app/release.keystore"
    
    print_step "Generating keystore..."
    
    keytool -genkey -v -keystore "$KEYSTORE_FILE" \
        -alias "$KEY_ALIAS" \
        -keyalg RSA \
        -keysize 2048 \
        -validity 10000 \
        -storepass "$STORE_PASSWORD" \
        -keypass "$KEY_PASSWORD" \
        -dname "CN=$CERT_NAME, OU=$CERT_OU, O=$CERT_O, L=$CERT_L, ST=$CERT_ST, C=$CERT_C"
    
    if [ $? -eq 0 ]; then
        print_success "Keystore created successfully: $KEYSTORE_FILE"
    else
        print_error "Failed to create keystore"
        exit 1
    fi
    
    # Create base64 version for GitHub secrets
    print_step "Creating base64 encoded version for GitHub..."
    base64 -i "$KEYSTORE_FILE" > keystore_base64.txt
    print_success "Base64 keystore saved to: keystore_base64.txt"
    
    # Create gradle.properties template
    print_step "Creating gradle.properties template..."
    cat > gradle.properties.template << EOF
# Keystore configuration (DO NOT COMMIT THESE VALUES!)
RELEASE_STORE_FILE=release.keystore
RELEASE_STORE_PASSWORD=$STORE_PASSWORD
RELEASE_KEY_ALIAS=$KEY_ALIAS
RELEASE_KEY_PASSWORD=$KEY_PASSWORD
EOF
    
    print_success "Gradle properties template created: gradle.properties.template"
    
    # Show GitHub secrets setup
    echo ""
    print_step "GitHub Secrets Setup"
    echo "Add these secrets to your GitHub repository:"
    echo "Settings > Secrets and variables > Actions > New repository secret"
    echo ""
    echo "Secret Name: KEYSTORE_BASE64"
    echo "Secret Value: (content of keystore_base64.txt file)"
    echo ""
    echo "Secret Name: KEYSTORE_PASSWORD"
    echo "Secret Value: $STORE_PASSWORD"
    echo ""
    echo "Secret Name: KEY_ALIAS"
    echo "Secret Value: $KEY_ALIAS"
    echo ""
    echo "Secret Name: KEY_PASSWORD"
    echo "Secret Value: $KEY_PASSWORD"
    echo ""
    
    # Security warnings
    print_warning "IMPORTANT SECURITY NOTES:"
    echo "1. Never commit the keystore file or passwords to git"
    echo "2. Keep the keystore file in a secure location"
    echo "3. Make a backup of your keystore - you cannot regenerate it!"
    echo "4. Add release.keystore to .gitignore"
    echo "5. Delete keystore_base64.txt after uploading to GitHub secrets"
    echo ""
    
    # Add to gitignore
    if [ -f ".gitignore" ]; then
        if ! grep -q "release.keystore" .gitignore; then
            echo "release.keystore" >> .gitignore
            echo "keystore_base64.txt" >> .gitignore
            echo "gradle.properties.template" >> .gitignore
            print_success "Added keystore files to .gitignore"
        fi
    fi
}

# Verify existing keystore
verify_keystore() {
    local keystore_file="$1"
    
    if [ ! -f "$keystore_file" ]; then
        print_error "Keystore file not found: $keystore_file"
        return 1
    fi
    
    print_step "Verifying keystore: $keystore_file"
    
    read -p "Enter keystore password: " -s STORE_PASSWORD
    echo ""
    
    if keytool -list -v -keystore "$keystore_file" -storepass "$STORE_PASSWORD"; then
        print_success "Keystore verified successfully"
        return 0
    else
        print_error "Failed to verify keystore"
        return 1
    fi
}

# Convert existing keystore to base64
convert_to_base64() {
    local keystore_file="$1"
    
    if [ ! -f "$keystore_file" ]; then
        print_error "Keystore file not found: $keystore_file"
        return 1
    fi
    
    print_step "Converting keystore to base64..."
    base64 -i "$keystore_file" > keystore_base64.txt
    print_success "Base64 keystore saved to: keystore_base64.txt"
    
    echo ""
    print_info "Upload the content of keystore_base64.txt to GitHub secret: KEYSTORE_BASE64"
    print_warning "Delete keystore_base64.txt after uploading to GitHub!"
}

# Main menu
show_menu() {
    echo "What would you like to do?"
    echo ""
    echo "1. Generate new release keystore"
    echo "2. Verify existing keystore"
    echo "3. Convert existing keystore to base64"
    echo "4. Show keystore information"
    echo "5. Exit"
    echo ""
    read -p "Select option (1-5): " choice
    
    case $choice in
        1)
            generate_keystore
            ;;
        2)
            read -p "Enter keystore file path (default: app/release.keystore): " keystore_path
            keystore_path=${keystore_path:-"app/release.keystore"}
            verify_keystore "$keystore_path"
            ;;
        3)
            read -p "Enter keystore file path (default: app/release.keystore): " keystore_path
            keystore_path=${keystore_path:-"app/release.keystore"}
            convert_to_base64 "$keystore_path"
            ;;
        4)
            show_keystore_info
            ;;
        5)
            print_info "Goodbye!"
            exit 0
            ;;
        *)
            print_error "Invalid option. Please select 1-5."
            show_menu
            ;;
    esac
}

# Show keystore information
show_keystore_info() {
    print_step "Keystore Information Guide"
    echo ""
    echo "🔐 What is a keystore?"
    echo "A keystore is a file that contains your app's signing certificate."
    echo "It's required to publish your app and for updates."
    echo ""
    echo "📋 Key components:"
    echo "• Keystore file (.keystore or .jks)"
    echo "• Keystore password (protects the file)"
    echo "• Key alias (identifies the certificate)"
    echo "• Key password (protects the certificate)"
    echo ""
    echo "⚠️  Important notes:"
    echo "• Never lose your keystore - you cannot regenerate it!"
    echo "• Keep it secure and make backups"
    echo "• Use the same keystore for all app updates"
    echo "• Never commit keystore files to version control"
    echo ""
    echo "🚀 For GitHub Actions:"
    echo "• Convert keystore to base64 format"
    echo "• Store as GitHub repository secret"
    echo "• GitHub Actions will decode and use it automatically"
    echo ""
}

# Cleanup function
cleanup() {
    if [ -f "keystore_base64.txt" ]; then
        read -p "Delete keystore_base64.txt file? (y/N): " delete_base64
        if [[ $delete_base64 =~ ^[Yy]$ ]]; then
            rm keystore_base64.txt
            print_success "keystore_base64.txt deleted"
        else
            print_warning "Remember to delete keystore_base64.txt after uploading to GitHub!"
        fi
    fi
}

# Trap cleanup on exit
trap cleanup EXIT

# Main script
print_header

# Check prerequisites
check_keytool

# Show menu
show_menu

print_success "Keystore setup completed!"
echo ""
print_info "Next steps:"
echo "1. Add keystore secrets to GitHub repository"
echo "2. Test local builds with: ./scripts/build-local.sh pro release"
echo "3. Commit your code and trigger GitHub Actions build"
echo ""