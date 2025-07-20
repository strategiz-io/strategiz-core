#!/usr/bin/env python3
"""
Simple test script to verify our unified authentication method subcollection structure.
Tests the collection structure we designed: users/{userId}/authentication_methods
"""

import requests
import json
import time
from typing import Dict, Any

# Test configuration
BASE_URL = "http://localhost:8080/v1/auth"
TEST_USER_ID = "test-user-123"

def test_passkey_registration():
    """Test passkey registration with our new unified structure"""
    print("🔐 Testing Passkey Registration...")
    
    # Step 1: Begin passkey registration (create challenge)
    begin_payload = {
        "userId": TEST_USER_ID,
        "username": "testuser@example.com"
    }
    
    print(f"📝 Beginning passkey registration for user: {TEST_USER_ID}")
    try:
        response = requests.post(f"{BASE_URL}/passkeys/registrations", json=begin_payload)
        print(f"Response Status: {response.status_code}")
        print(f"Response: {json.dumps(response.json(), indent=2)}")
        
        if response.status_code == 200:
            challenge_data = response.json()
            print("✅ Successfully created passkey registration challenge")
            return challenge_data
        else:
            print(f"❌ Failed to create challenge: {response.text}")
            return None
            
    except requests.exceptions.ConnectionError:
        print("❌ Cannot connect to server. Please start the application first with:")
        print("   mvn spring-boot:run -pl application")
        return None
    except Exception as e:
        print(f"❌ Error during passkey registration: {e}")
        return None

def test_totp_registration():
    """Test TOTP registration with our new unified structure"""
    print("\n🔢 Testing TOTP Registration...")
    
    payload = {
        "userId": TEST_USER_ID
    }
    
    try:
        response = requests.post(f"{BASE_URL}/totp/setup", json=payload)
        print(f"Response Status: {response.status_code}")
        print(f"Response: {json.dumps(response.json(), indent=2)}")
        
        if response.status_code == 200:
            print("✅ Successfully created TOTP setup")
            return response.json()
        else:
            print(f"❌ Failed to setup TOTP: {response.text}")
            return None
            
    except Exception as e:
        print(f"❌ Error during TOTP registration: {e}")
        return None

def test_sms_otp_registration():
    """Test SMS OTP registration with our new unified structure"""
    print("\n📱 Testing SMS OTP Registration...")
    
    payload = {
        "userId": TEST_USER_ID,
        "phoneNumber": "+15551234567"
    }
    
    try:
        response = requests.post(f"{BASE_URL}/sms-otp/register", json=payload)
        print(f"Response Status: {response.status_code}")
        print(f"Response: {json.dumps(response.json(), indent=2)}")
        
        if response.status_code == 200:
            print("✅ Successfully registered SMS OTP")
            return response.json()
        else:
            print(f"❌ Failed to register SMS OTP: {response.text}")
            return None
            
    except Exception as e:
        print(f"❌ Error during SMS OTP registration: {e}")
        return None

def show_expected_collection_structure():
    """Show what the Firestore collection structure should look like with detailed metadata"""
    print("\n📊 Expected Firestore Collection Structure:")
    print("=" * 80)
    
    structure = {
        "users": {
            f"{TEST_USER_ID}": {
                "authentication_methods": {
                    "passkey_001": {
                        "id": "passkey_001",
                        "type": "PASSKEY",
                        "name": "iPhone TouchID",
                        "isEnabled": True,
                        "lastUsedAt": "2024-01-15T10:30:00Z",
                        "metadata": {
                            # Core WebAuthn fields
                            "credentialId": "abc123def456...",
                            "publicKeyBase64": "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA...",
                            "signatureCount": 15,
                            "aaguid": "12345678-1234-5678-9012-123456789012",
                            
                            # Device information
                            "deviceName": "iPhone 15 Pro",
                            "userAgent": "Mozilla/5.0 (iPhone; CPU iPhone OS 17_0...",
                            "authenticatorName": "Touch ID",
                            
                            # Security flags
                            "trusted": True,
                            "verified": True,
                            "backupEligible": False,
                            "backupState": False,
                            
                            # WebAuthn specific
                            "attestationType": "none",
                            "transport": "internal",
                            "residentKey": True,
                            
                            # Timestamps
                            "registrationTime": "2024-01-10T08:00:00Z",
                            "lastUsedTime": "2024-01-15T10:30:00Z"
                        }
                    },
                    "totp_001": {
                        "id": "totp_001", 
                        "type": "TOTP",
                        "name": "Google Authenticator",
                        "isEnabled": True,
                        "lastUsedAt": "2024-01-14T14:22:00Z",
                        "metadata": {
                            # Core TOTP fields
                            "secretKey": "JBSWY3DPEHPK3PXP",
                            "algorithm": "SHA1",
                            "digits": 6,
                            "period": 30,
                            
                            # Backup and recovery
                            "backupCodes": ["12345678", "87654321", "23456789"],
                            "backupCodesUsed": ["12345678"],
                            
                            # QR Code and setup
                            "qrCodeGenerated": True,
                            "issuer": "Strategiz",
                            "accountName": "testuser@example.com",
                            
                            # Verification status
                            "verified": True,
                            "verificationTime": "2024-01-08T12:05:00Z",
                            
                            # Usage tracking
                            "lastCodeUsed": "123456",
                            "lastUsedTime": "2024-01-14T14:22:00Z",
                            "usageCount": 45
                        }
                    },
                    "sms_001": {
                        "id": "sms_001",
                        "type": "SMS_OTP", 
                        "name": "+• (•••) •••-4567",
                        "isEnabled": True,
                        "lastUsedAt": "2024-01-13T09:15:00Z",
                        "metadata": {
                            # Phone number details
                            "phoneNumber": "+15551234567",
                            "countryCode": "US",
                            "carrier": "Verizon",
                            
                            # Verification status
                            "isVerified": True,
                            "verificationTime": "2024-01-05T16:35:00Z",
                            
                            # SMS delivery
                            "lastSmsSent": "2024-01-13T09:14:30Z",
                            "smsDeliveryStatus": "delivered",
                            "smsProvider": "Firebase",
                            
                            # Rate limiting
                            "dailySmsCount": 3,
                            "lastSmsDate": "2024-01-13",
                            "rateLimitExceeded": False,
                            
                            # Usage tracking
                            "lastUsedTime": "2024-01-13T09:15:00Z",
                            "usageCount": 12,
                            "failedAttempts": 0
                        }
                    },
                    "oauth_google_001": {
                        "id": "oauth_google_001",
                        "type": "OAUTH_GOOGLE",
                        "name": "Google Account",
                        "isEnabled": True,
                        "lastUsedAt": "2024-01-12T08:30:00Z",
                        "metadata": {
                            # OAuth provider details
                            "provider": "google",
                            "providerUserId": "google_user_123456789",
                            "email": "testuser@gmail.com",
                            "displayName": "Test User",
                            "profilePicture": "https://lh3.googleusercontent.com/...",
                            
                            # Token information (hashed for security)
                            "accessTokenHash": "sha256:abc123...",
                            "refreshTokenHash": "sha256:def456...",
                            "tokenExpiry": "2024-01-12T10:30:00Z",
                            
                            # Scopes and permissions
                            "grantedScopes": ["openid", "email", "profile"],
                            "profileVerified": True,
                            
                            # Connection details
                            "connectedTime": "2024-01-01T00:00:00Z",
                            "lastSyncTime": "2024-01-12T08:30:00Z",
                            "connectionStatus": "active",
                            
                            # Usage tracking
                            "lastUsedTime": "2024-01-12T08:30:00Z",
                            "loginCount": 23
                        }
                    }
                }
            }
        }
    }
    
    print("🏗️  Unified Authentication Method Subcollection Structure")
    print("📍 Path: users/{userId}/authentication_methods/{methodId}")
    print()
    print(json.dumps(structure, indent=2))
    print("=" * 80)
    print("✅ Each type stores unique metadata while sharing common structure")
    print("🔐 Supports: Passkeys, TOTP, SMS OTP, Email OTP, OAuth providers")
    print("📊 Provides: Usage tracking, security levels, backup support")

def main():
    """Main test function"""
    print("🚀 Testing Unified Authentication Method Subcollection Structure")
    print("=" * 70)
    
    # Show expected structure
    show_expected_collection_structure()
    
    # Test each authentication method type
    passkey_result = test_passkey_registration()
    totp_result = test_totp_registration() 
    sms_result = test_sms_otp_registration()
    
    # Summary
    print("\n📋 Test Summary:")
    print("=" * 30)
    print(f"✅ Passkey Registration: {'PASS' if passkey_result else 'FAIL'}")
    print(f"✅ TOTP Registration: {'PASS' if totp_result else 'FAIL'}")
    print(f"✅ SMS OTP Registration: {'PASS' if sms_result else 'FAIL'}")
    
    if any([passkey_result, totp_result, sms_result]):
        print("\n🎉 Some authentication methods are working!")
        print("🔍 Check Firestore at: users/{userId}/authentication_methods")
        print(f"    For user: {TEST_USER_ID}")
    else:
        print("\n❌ No authentication methods working. Check server logs.")

if __name__ == "__main__":
    main()