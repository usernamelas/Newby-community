#!/bin/bash

# Pool Assistant - Local Build Script
# Usage: ./scripts/build-local.sh [flavor] [build_type] [options]

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
NC='\033[0m' # No Color

# Default values
FLAVOR="standard"
BUILD_TYPE="debug"
CLEAN_BUILD=false
SKIP_TESTS=false
ANALYZE_APK=false
DUAL_BUILD=false

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        standard|pro)
            FLAVOR="$1"
            shift
            ;;
        debug|beta|release|releaseOpen)
            BUILD_TYPE="$1"
            shift
            ;;
        --clean)
            CLEAN_BUILD=true
            shift
            ;;
        --skip-tests)
            SKIP_TESTS=true
            shift
            ;;
        --analyze)
            ANALYZE_APK=true
            shift
            ;;
        --dual)
            DUAL_BUILD=true
            shift
            ;;
        --help|-h)
            echo "Pool Assistant Build Script"
            echo ""
            echo "Usage: $0 [flavor] [build_type] [options]"
            echo ""
            echo "Flavors:"
            echo "  standard  - Standard version (default)"
            echo "  pro       - Pro version with advanced features"
            echo ""
            echo "Build Types:"
            echo "  debug     - Debug build (default)"
            echo "  beta      - Beta build with light obfuscation"
            echo "  release   - Release build with full obfuscation"
            echo "  releaseOpen - Release build without obfuscation"
            echo ""
            echo "Options:"
            echo "  --clean      Clean build (./gradlew clean first)"
            echo "  --skip-tests Skip running tests"
            echo "  --analyze    Analyze APK after build"
            echo "  --dual       Build both protected and open release"
            echo "  --help, -h   Show this help message"
            echo ""
            echo "Examples:"
            echo "  $0 pro release --clean --analyze"
            echo "  $0 standard debug"
            echo "  $0 --dual --clean"
            exit 0
            ;;
        *)
            echo -e "${RED}Unknown option: $1${NC}"
            echo "Use --help for usage information"
            exit 1
            ;;
    esac
done

# Function to print colored output
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
    echo -e "${PURPLE}ℹ️  $1${NC}"
}

# Check if we're in the right directory
if [ ! -f "gradlew" ]; then
    print_error "gradlew not found. Please run this script from the project root directory."
    exit 1
fi

# Make gradlew executable
chmod +x gradlew

# Print build configuration
echo -e "${PURPLE}🎱 Pool Assistant Build Script${NC}"
echo "=================================="
print_info "Flavor: $FLAVOR"
print_info "Build Type: $BUILD_TYPE"
print_info "Clean Build: $CLEAN_BUILD"
print_info "Skip Tests: $SKIP_TESTS"
print_info "Analyze APK: $ANALYZE_APK"
print_info "Dual Build: $DUAL_BUILD"
echo ""

# Clean build if requested
if [ "$CLEAN_BUILD" = true ]; then
    print_step "Cleaning previous builds..."
    ./gradlew clean
    print_success "Clean completed"
fi

# Create output directory
mkdir -p output/

# Function to build APK
build_apk() {
    local flavor=$1
    local build_type=$2
    local build_name=$3
    
    print_step "Building $build_name APK..."
    
    # Capitalize first letter for Gradle task
    local flavor_cap="$(tr '[:lower:]' '[:upper:]' <<< ${flavor:0:1})${flavor:1}"
    local build_type_cap="$(tr '[:lower:]' '[:upper:]' <<< ${build_type:0:1})${build_type:1}"
    
    # Run the build
    if ./gradlew assemble${flavor_cap}${build_type_cap} --stacktrace; then
        print_success "$build_name APK build completed"
        
        # Find and copy APK
        APK_PATH="app/build/outputs/apk/$flavor/$build_type"
        APK_FILE=$(find "$APK_PATH" -name "*.apk" 2>/dev/null | head -1)
        
        if [ -f "$APK_FILE" ]; then
            cp "$APK_FILE" output/
            print_success "APK copied to output/ directory"
            
            # Get APK info
            APK_SIZE=$(du -h "$APK_FILE" | cut -f1)
            APK_NAME=$(basename "$APK_FILE")
            print_info "APK: $APK_NAME ($APK_SIZE)"
            
            return 0
        else
            print_error "APK file not found in $APK_PATH"
            return 1
        fi
    else
        print_error "$build_name APK build failed"
        return 1
    fi
}

# Function to run tests
run_tests() {
    if [ "$SKIP_TESTS" = false ]; then
        print_step "Running tests..."
        
        local flavor_cap="$(tr '[:lower:]' '[:upper:]' <<< ${FLAVOR:0:1})${FLAVOR:1}"
        
        if ./gradlew test${flavor_cap}DebugUnitTest --continue; then
            print_success "Tests completed"
        else
            print_warning "Some tests failed, but continuing build"
        fi
    else
        print_info "Skipping tests"
    fi
}

# Function to analyze APK
analyze_apk() {
    if [ "$ANALYZE_APK" = true ]; then
        print_step "Analyzing APK..."
        
        for apk in output/*.apk; do
            if [ -f "$apk" ]; then
                echo ""
                print_info "Analyzing: $(basename "$apk")"
                
                # APK size
                APK_SIZE=$(du -h "$apk" | cut -f1)
                APK_SIZE_BYTES=$(du -b "$apk" | cut -f1)
                echo "  📦 Size: $APK_SIZE ($APK_SIZE_BYTES bytes)"
                
                # APK signature verification
                if jarsigner -verify "$apk" &>/dev/null; then
                    echo "  ✅ Signature: Valid"
                else
                    echo "  ❌ Signature: Invalid or missing"
                fi
                
                # Check for debug symbols
                if strings "$apk" | grep -i "debug\|test" > /dev/null 2>&1; then
                    DEBUG_COUNT=$(strings "$apk" | grep -i "debug\|test" | wc -l)
                    echo "  🐛 Debug symbols: $DEBUG_COUNT found"
                else
                    echo "  ✅ Debug symbols: None found"
                fi
                
                # Permissions count
                if command -v aapt >/dev/null 2>&1; then
                    PERM_COUNT=$(aapt dump permissions "$apk" 2>/dev/null | wc -l)
                    echo "  🔐 Permissions: $PERM_COUNT"
                fi
            fi
        done
        
        print_success "APK analysis completed"
    fi
}

# Main build logic
if [ "$DUAL_BUILD" = true ]; then
    print_step "Building dual APKs (Protected + Open)..."
    
    # Run tests first
    run_tests
    
    # Build protected release
    if build_apk "$FLAVOR" "release" "Protected Release"; then
        PROTECTED_SUCCESS=true
    else
        PROTECTED_SUCCESS=false
    fi
    
    # Build open release
    if build_apk "$FLAVOR" "releaseOpen" "Open Release"; then
        OPEN_SUCCESS=true
    else
        OPEN_SUCCESS=false
    fi
    
    # Summary
    echo ""
    print_step "Dual build summary:"
    if [ "$PROTECTED_SUCCESS" = true ]; then
        print_success "Protected APK: Built successfully"
    else
        print_error "Protected APK: Build failed"
    fi
    
    if [ "$OPEN_SUCCESS" = true ]; then
        print_success "Open APK: Built successfully"
    else
        print_error "Open APK: Build failed"
    fi
    
else
    # Single build
    run_tests
    build_apk "$FLAVOR" "$BUILD_TYPE" "$(echo $BUILD_TYPE | sed 's/.*/\u&/') Build"
fi

# Analyze APKs
analyze_apk

# Final summary
echo ""
echo "=================================="
print_step "Build Summary"

# List all APKs in output directory
if ls output/*.apk 1> /dev/null 2>&1; then
    echo ""
    print_info "Generated APKs:"
    for apk in output/*.apk; do
        if [ -f "$apk" ]; then
            APK_SIZE=$(du -h "$apk" | cut -f1)
            APK_NAME=$(basename "$apk")
            echo "  📱 $APK_NAME ($APK_SIZE)"
        fi
    done
    echo ""
    print_success "All APKs are available in the output/ directory"
else
    print_error "No APKs were generated"
    exit 1
fi

# Build completion message
BUILD_TIME=$(date "+%Y-%m-%d %H:%M:%S")
print_success "Build completed at $BUILD_TIME"

# Usage suggestions
echo ""
print_info "Next steps:"
echo "  • Test the APK on your device"
echo "  • Check the output/ directory for generated files"
if [ "$BUILD_TYPE" = "release" ] || [ "$DUAL_BUILD" = true ]; then
    echo "  • Upload to GitHub releases or distribute as needed"
fi
echo ""
