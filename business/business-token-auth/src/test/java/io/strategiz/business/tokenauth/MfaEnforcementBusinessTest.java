package io.strategiz.business.tokenauth;

import io.strategiz.data.auth.entity.AuthenticationMethodEntity;
import io.strategiz.data.auth.entity.AuthenticationMethodType;
import io.strategiz.data.auth.repository.AuthenticationMethodRepository;
import io.strategiz.data.preferences.entity.SecurityPreferences;
import io.strategiz.data.preferences.repository.SecurityPreferencesRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for MfaEnforcementBusiness.
 *
 * Tests MFA enforcement logic including step-up authentication checks, validation for
 * enabling MFA, and ACR level calculations.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MfaEnforcementBusiness Tests")
class MfaEnforcementBusinessTest {

	@Mock
	private SecurityPreferencesRepository securityPreferencesRepository;

	@Mock
	private AuthenticationMethodRepository authMethodRepository;

	@InjectMocks
	private MfaEnforcementBusiness mfaEnforcementBusiness;

	private static final String TEST_USER_ID = "user-123";

	private AuthenticationMethodEntity createMethod(AuthenticationMethodType type, boolean active) {
		AuthenticationMethodEntity method = new AuthenticationMethodEntity();
		method.setId("method-" + type.name());
		method.setAuthenticationMethod(type);
		method.setIsActive(active);
		method.setName(type.name() + " Method");
		return method;
	}

	@Nested
	@DisplayName("checkStepUpRequired() Tests")
	class CheckStepUpRequiredTests {

		@Test
		@DisplayName("Should return not required when MFA is not enforced")
		void checkStepUpRequired_MfaNotEnforced_ReturnsNotRequired() {
			// Given
			SecurityPreferences prefs = new SecurityPreferences();
			prefs.setMfaEnforced(false);
			when(securityPreferencesRepository.getByUserId(TEST_USER_ID)).thenReturn(prefs);

			// When
			MfaEnforcementBusiness.StepUpCheckResult result = mfaEnforcementBusiness.checkStepUpRequired(TEST_USER_ID,
					"1");

			// Then
			assertFalse(result.required());
			assertEquals(0, result.minimumAcrLevel());
			assertTrue(result.availableMethods().isEmpty());
		}

		@Test
		@DisplayName("Should return not required when current ACR meets minimum")
		void checkStepUpRequired_AcrMeetsMinimum_ReturnsNotRequired() {
			// Given
			SecurityPreferences prefs = new SecurityPreferences();
			prefs.setMfaEnforced(true);
			prefs.setMinimumAcrLevel(2);
			when(securityPreferencesRepository.getByUserId(TEST_USER_ID)).thenReturn(prefs);

			// When
			MfaEnforcementBusiness.StepUpCheckResult result = mfaEnforcementBusiness.checkStepUpRequired(TEST_USER_ID,
					"2");

			// Then
			assertFalse(result.required());
		}

		@Test
		@DisplayName("Should return required when current ACR is below minimum")
		void checkStepUpRequired_AcrBelowMinimum_ReturnsRequired() {
			// Given
			SecurityPreferences prefs = new SecurityPreferences();
			prefs.setMfaEnforced(true);
			prefs.setMinimumAcrLevel(2);
			when(securityPreferencesRepository.getByUserId(TEST_USER_ID)).thenReturn(prefs);

			List<AuthenticationMethodEntity> methods = List.of(createMethod(AuthenticationMethodType.TOTP, true),
					createMethod(AuthenticationMethodType.PASSKEY, true));
			when(authMethodRepository.findByUserId(TEST_USER_ID)).thenReturn(methods);

			// When
			MfaEnforcementBusiness.StepUpCheckResult result = mfaEnforcementBusiness.checkStepUpRequired(TEST_USER_ID,
					"1");

			// Then
			assertTrue(result.required());
			assertEquals(2, result.minimumAcrLevel());
			assertEquals(2, result.availableMethods().size());
		}

		@Test
		@DisplayName("Should handle invalid ACR string gracefully")
		void checkStepUpRequired_InvalidAcr_DefaultsToOne() {
			// Given
			SecurityPreferences prefs = new SecurityPreferences();
			prefs.setMfaEnforced(true);
			prefs.setMinimumAcrLevel(2);
			when(securityPreferencesRepository.getByUserId(TEST_USER_ID)).thenReturn(prefs);

			List<AuthenticationMethodEntity> methods = List.of(createMethod(AuthenticationMethodType.TOTP, true));
			when(authMethodRepository.findByUserId(TEST_USER_ID)).thenReturn(methods);

			// When
			MfaEnforcementBusiness.StepUpCheckResult result = mfaEnforcementBusiness.checkStepUpRequired(TEST_USER_ID,
					"invalid");

			// Then
			assertTrue(result.required()); // Defaults to ACR 1, which is below 2
		}

	}

	@Nested
	@DisplayName("validateCanEnableMfa() Tests")
	class ValidateCanEnableMfaTests {

		@Test
		@DisplayName("Should return canEnable=false when no MFA methods exist")
		void validateCanEnableMfa_NoMethods_CannotEnable() {
			// Given
			when(authMethodRepository.findByUserId(TEST_USER_ID)).thenReturn(List.of());

			// When
			MfaEnforcementBusiness.ValidationResult result = mfaEnforcementBusiness.validateCanEnableMfa(TEST_USER_ID);

			// Then
			assertFalse(result.canEnable());
			assertNotNull(result.reason());
			assertTrue(result.reason().contains("No MFA methods configured"));
		}

		@Test
		@DisplayName("Should return canEnable=false when only password method exists")
		void validateCanEnableMfa_OnlyPassword_CannotEnable() {
			// Given
			List<AuthenticationMethodEntity> methods = List.of(createMethod(AuthenticationMethodType.EMAIL_OTP, true));
			when(authMethodRepository.findByUserId(TEST_USER_ID)).thenReturn(methods);

			// When
			MfaEnforcementBusiness.ValidationResult result = mfaEnforcementBusiness.validateCanEnableMfa(TEST_USER_ID);

			// Then
			assertFalse(result.canEnable());
		}

		@Test
		@DisplayName("Should return canEnable=true with TOTP method")
		void validateCanEnableMfa_WithTotp_CanEnable() {
			// Given
			List<AuthenticationMethodEntity> methods = List.of(createMethod(AuthenticationMethodType.TOTP, true));
			when(authMethodRepository.findByUserId(TEST_USER_ID)).thenReturn(methods);

			// When
			MfaEnforcementBusiness.ValidationResult result = mfaEnforcementBusiness.validateCanEnableMfa(TEST_USER_ID);

			// Then
			assertTrue(result.canEnable());
			assertEquals(2, result.maxAchievableAcr());
			assertTrue(result.configuredMethods().contains("totp"));
		}

		@Test
		@DisplayName("Should return canEnable=true with passkey method")
		void validateCanEnableMfa_WithPasskey_CanEnable() {
			// Given
			List<AuthenticationMethodEntity> methods = List.of(createMethod(AuthenticationMethodType.PASSKEY, true));
			when(authMethodRepository.findByUserId(TEST_USER_ID)).thenReturn(methods);

			// When
			MfaEnforcementBusiness.ValidationResult result = mfaEnforcementBusiness.validateCanEnableMfa(TEST_USER_ID);

			// Then
			assertTrue(result.canEnable());
			assertEquals(2, result.maxAchievableAcr()); // Passkey alone is ACR 2
		}

		@Test
		@DisplayName("Should calculate ACR 3 for passkey + TOTP")
		void validateCanEnableMfa_PasskeyAndTotp_AcrThree() {
			// Given
			List<AuthenticationMethodEntity> methods = List.of(createMethod(AuthenticationMethodType.PASSKEY, true),
					createMethod(AuthenticationMethodType.TOTP, true));
			when(authMethodRepository.findByUserId(TEST_USER_ID)).thenReturn(methods);

			// When
			MfaEnforcementBusiness.ValidationResult result = mfaEnforcementBusiness.validateCanEnableMfa(TEST_USER_ID);

			// Then
			assertTrue(result.canEnable());
			assertEquals(3, result.maxAchievableAcr()); // Passkey + another = ACR 3
			assertEquals(2, result.configuredMethods().size());
		}

		@Test
		@DisplayName("Should ignore inactive non-passkey methods")
		void validateCanEnableMfa_InactiveMethods_Ignored() {
			// Given - only inactive non-passkey methods (passkeys are always active)
			List<AuthenticationMethodEntity> methods = List.of(createMethod(AuthenticationMethodType.TOTP, false), // inactive
					createMethod(AuthenticationMethodType.SMS_OTP, false) // inactive
			);
			when(authMethodRepository.findByUserId(TEST_USER_ID)).thenReturn(methods);

			// When
			MfaEnforcementBusiness.ValidationResult result = mfaEnforcementBusiness.validateCanEnableMfa(TEST_USER_ID);

			// Then
			assertFalse(result.canEnable());
		}

	}

	@Nested
	@DisplayName("getEnforcementSettings() Tests")
	class GetEnforcementSettingsTests {

		@Test
		@DisplayName("Should return complete settings with all fields populated")
		void getEnforcementSettings_ReturnsCompleteSettings() {
			// Given
			SecurityPreferences prefs = new SecurityPreferences();
			prefs.setMfaEnforced(true);
			prefs.setMinimumAcrLevel(2);
			when(securityPreferencesRepository.getByUserId(TEST_USER_ID)).thenReturn(prefs);

			List<AuthenticationMethodEntity> methods = List.of(createMethod(AuthenticationMethodType.TOTP, true));
			when(authMethodRepository.findByUserId(TEST_USER_ID)).thenReturn(methods);

			// When
			MfaEnforcementBusiness.MfaEnforcementSettings settings = mfaEnforcementBusiness
				.getEnforcementSettings(TEST_USER_ID);

			// Then
			assertTrue(settings.enforced());
			assertEquals(2, settings.minimumAcrLevel());
			assertEquals(2, settings.currentAcrLevel());
			assertTrue(settings.canEnable());
			assertEquals("Strong", settings.strengthLabel());
			assertEquals(1, settings.configuredMethods().size());
		}

		@Test
		@DisplayName("Should return Maximum strength label for ACR 3")
		void getEnforcementSettings_AcrThree_MaximumStrength() {
			// Given
			SecurityPreferences prefs = new SecurityPreferences();
			prefs.setMfaEnforced(true);
			when(securityPreferencesRepository.getByUserId(TEST_USER_ID)).thenReturn(prefs);

			List<AuthenticationMethodEntity> methods = List.of(createMethod(AuthenticationMethodType.PASSKEY, true),
					createMethod(AuthenticationMethodType.TOTP, true));
			when(authMethodRepository.findByUserId(TEST_USER_ID)).thenReturn(methods);

			// When
			MfaEnforcementBusiness.MfaEnforcementSettings settings = mfaEnforcementBusiness
				.getEnforcementSettings(TEST_USER_ID);

			// Then
			assertEquals(3, settings.currentAcrLevel());
			assertEquals("Maximum", settings.strengthLabel());
			assertNull(settings.upgradeHint()); // No upgrade hint for maximum
		}

		@Test
		@DisplayName("Should provide upgrade hint for TOTP-only user")
		void getEnforcementSettings_TotpOnly_SuggestsPasskey() {
			// Given
			SecurityPreferences prefs = new SecurityPreferences();
			when(securityPreferencesRepository.getByUserId(TEST_USER_ID)).thenReturn(prefs);

			List<AuthenticationMethodEntity> methods = List.of(createMethod(AuthenticationMethodType.TOTP, true));
			when(authMethodRepository.findByUserId(TEST_USER_ID)).thenReturn(methods);

			// When
			MfaEnforcementBusiness.MfaEnforcementSettings settings = mfaEnforcementBusiness
				.getEnforcementSettings(TEST_USER_ID);

			// Then
			assertNotNull(settings.upgradeHint());
			assertTrue(settings.upgradeHint().toLowerCase().contains("passkey"));
		}

	}

	@Nested
	@DisplayName("updateMfaEnforcement() Tests")
	class UpdateMfaEnforcementTests {

		@Test
		@DisplayName("Should successfully enable MFA when methods exist")
		void updateMfaEnforcement_EnableWithMethods_Success() {
			// Given
			SecurityPreferences prefs = new SecurityPreferences();
			when(securityPreferencesRepository.getByUserId(TEST_USER_ID)).thenReturn(prefs);

			List<AuthenticationMethodEntity> methods = List.of(createMethod(AuthenticationMethodType.TOTP, true));
			when(authMethodRepository.findByUserId(TEST_USER_ID)).thenReturn(methods);

			// When
			MfaEnforcementBusiness.UpdateResult result = mfaEnforcementBusiness.updateMfaEnforcement(TEST_USER_ID,
					true);

			// Then
			assertTrue(result.success());
			assertNotNull(result.settings());
			verify(securityPreferencesRepository).save(eq(TEST_USER_ID), any(SecurityPreferences.class));
		}

		@Test
		@DisplayName("Should fail to enable MFA when no methods exist")
		void updateMfaEnforcement_EnableWithoutMethods_Failure() {
			// Given
			when(authMethodRepository.findByUserId(TEST_USER_ID)).thenReturn(List.of());

			// When
			MfaEnforcementBusiness.UpdateResult result = mfaEnforcementBusiness.updateMfaEnforcement(TEST_USER_ID,
					true);

			// Then
			assertFalse(result.success());
			assertNotNull(result.error());
			verify(securityPreferencesRepository, never()).save(anyString(), any());
		}

		@Test
		@DisplayName("Should successfully disable MFA without validation")
		void updateMfaEnforcement_Disable_Success() {
			// Given
			SecurityPreferences prefs = new SecurityPreferences();
			prefs.setMfaEnforced(true);
			when(securityPreferencesRepository.getByUserId(TEST_USER_ID)).thenReturn(prefs);

			List<AuthenticationMethodEntity> methods = List.of(); // Even with no methods
			when(authMethodRepository.findByUserId(TEST_USER_ID)).thenReturn(methods);

			// When
			MfaEnforcementBusiness.UpdateResult result = mfaEnforcementBusiness.updateMfaEnforcement(TEST_USER_ID,
					false);

			// Then
			assertTrue(result.success());
			verify(securityPreferencesRepository).save(eq(TEST_USER_ID), any(SecurityPreferences.class));
		}

	}

	@Nested
	@DisplayName("checkMfaMethodRemoval() Tests")
	class CheckMfaMethodRemovalTests {

		@Test
		@DisplayName("Should return safe when MFA not enforced")
		void checkMfaMethodRemoval_MfaNotEnforced_SafeToRemove() {
			// Given
			SecurityPreferences prefs = new SecurityPreferences();
			prefs.setMfaEnforced(false);
			when(securityPreferencesRepository.getByUserId(TEST_USER_ID)).thenReturn(prefs);

			// When
			MfaEnforcementBusiness.RemovalCheckResult result = mfaEnforcementBusiness
				.checkMfaMethodRemoval(TEST_USER_ID, AuthenticationMethodType.TOTP);

			// Then
			assertTrue(result.safeToRemove());
			assertFalse(result.willDisableMfaEnforcement());
		}

		@Test
		@DisplayName("Should warn when removing last MFA method with enforcement enabled")
		void checkMfaMethodRemoval_LastMethod_WillDisableEnforcement() {
			// Given
			SecurityPreferences prefs = new SecurityPreferences();
			prefs.setMfaEnforced(true);
			when(securityPreferencesRepository.getByUserId(TEST_USER_ID)).thenReturn(prefs);

			// Only TOTP exists, removing it leaves no MFA methods
			List<AuthenticationMethodEntity> methods = List.of(createMethod(AuthenticationMethodType.TOTP, true));
			when(authMethodRepository.findByUserId(TEST_USER_ID)).thenReturn(methods);

			// When
			MfaEnforcementBusiness.RemovalCheckResult result = mfaEnforcementBusiness
				.checkMfaMethodRemoval(TEST_USER_ID, AuthenticationMethodType.TOTP);

			// Then
			assertTrue(result.safeToRemove()); // Still safe, just with warning
			assertTrue(result.willDisableMfaEnforcement());
			assertTrue(result.enforcementWasActive());
		}

		@Test
		@DisplayName("Should be safe when other MFA methods remain")
		void checkMfaMethodRemoval_OtherMethodsRemain_Safe() {
			// Given
			SecurityPreferences prefs = new SecurityPreferences();
			prefs.setMfaEnforced(true);
			when(securityPreferencesRepository.getByUserId(TEST_USER_ID)).thenReturn(prefs);

			// Both TOTP and Passkey exist
			List<AuthenticationMethodEntity> methods = List.of(createMethod(AuthenticationMethodType.TOTP, true),
					createMethod(AuthenticationMethodType.PASSKEY, true));
			when(authMethodRepository.findByUserId(TEST_USER_ID)).thenReturn(methods);

			// When
			MfaEnforcementBusiness.RemovalCheckResult result = mfaEnforcementBusiness
				.checkMfaMethodRemoval(TEST_USER_ID, AuthenticationMethodType.TOTP);

			// Then
			assertTrue(result.safeToRemove());
			assertFalse(result.willDisableMfaEnforcement()); // Passkey remains
		}

	}

	@Nested
	@DisplayName("onMfaMethodRemoved() Tests")
	class OnMfaMethodRemovedTests {

		@Test
		@DisplayName("Should auto-disable enforcement when no MFA methods remain")
		void onMfaMethodRemoved_NoMethodsRemain_DisablesEnforcement() {
			// Given
			when(authMethodRepository.findByUserId(TEST_USER_ID)).thenReturn(List.of());

			// When
			mfaEnforcementBusiness.onMfaMethodRemoved(TEST_USER_ID);

			// Then
			verify(securityPreferencesRepository).disableMfaEnforcement(TEST_USER_ID);
		}

		@Test
		@DisplayName("Should not disable enforcement when MFA methods remain")
		void onMfaMethodRemoved_MethodsRemain_KeepsEnforcement() {
			// Given
			List<AuthenticationMethodEntity> methods = List.of(createMethod(AuthenticationMethodType.PASSKEY, true));
			when(authMethodRepository.findByUserId(TEST_USER_ID)).thenReturn(methods);

			// When
			mfaEnforcementBusiness.onMfaMethodRemoved(TEST_USER_ID);

			// Then
			verify(securityPreferencesRepository, never()).disableMfaEnforcement(anyString());
		}

	}

	@Nested
	@DisplayName("getAvailableMfaMethods() Tests")
	class GetAvailableMfaMethodsTests {

		@Test
		@DisplayName("Should return only active MFA methods")
		void getAvailableMfaMethods_ReturnsOnlyActiveMfa() {
			// Given
			List<AuthenticationMethodEntity> methods = List.of(createMethod(AuthenticationMethodType.EMAIL_OTP, true), // Not
																														// MFA
					createMethod(AuthenticationMethodType.TOTP, true), // Active MFA
					createMethod(AuthenticationMethodType.PASSKEY, false), // Inactive MFA
					createMethod(AuthenticationMethodType.SMS_OTP, true) // Active MFA
			);
			when(authMethodRepository.findByUserId(TEST_USER_ID)).thenReturn(methods);

			// When
			List<MfaEnforcementBusiness.MfaMethodInfo> result = mfaEnforcementBusiness
				.getAvailableMfaMethods(TEST_USER_ID);

			// Then
			// Passkeys are always considered active (verified during WebAuthn
			// registration)
			assertEquals(3, result.size());
			assertTrue(result.stream().anyMatch(m -> "totp".equals(m.type())));
			assertTrue(result.stream().anyMatch(m -> "sms_otp".equals(m.type())));
			assertTrue(result.stream().anyMatch(m -> "passkey".equals(m.type())));
		}

		@Test
		@DisplayName("Should mark passkey as Strong strength")
		void getAvailableMfaMethods_PasskeyIsStrong() {
			// Given
			List<AuthenticationMethodEntity> methods = List.of(createMethod(AuthenticationMethodType.PASSKEY, true));
			when(authMethodRepository.findByUserId(TEST_USER_ID)).thenReturn(methods);

			// When
			List<MfaEnforcementBusiness.MfaMethodInfo> result = mfaEnforcementBusiness
				.getAvailableMfaMethods(TEST_USER_ID);

			// Then
			assertEquals(1, result.size());
			assertEquals("Strong", result.get(0).strengthLabel());
		}

		@Test
		@DisplayName("Should mark TOTP as Standard strength")
		void getAvailableMfaMethods_TotpIsStandard() {
			// Given
			List<AuthenticationMethodEntity> methods = List.of(createMethod(AuthenticationMethodType.TOTP, true));
			when(authMethodRepository.findByUserId(TEST_USER_ID)).thenReturn(methods);

			// When
			List<MfaEnforcementBusiness.MfaMethodInfo> result = mfaEnforcementBusiness
				.getAvailableMfaMethods(TEST_USER_ID);

			// Then
			assertEquals(1, result.size());
			assertEquals("Standard", result.get(0).strengthLabel());
		}

	}

}
