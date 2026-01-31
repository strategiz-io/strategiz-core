package io.strategiz.service.auth.service.passkey;

import java.util.Map;
import java.util.HashMap;

/**
 * Registry mapping AAGUID (Authenticator Attestation GUID) to known authenticators.
 *
 * AAGUIDs are 128-bit identifiers that uniquely identify authenticator models. This
 * allows us to display friendly names and logos for password managers.
 *
 * Reference: https://github.com/nicholasrusso/passkey-authenticator-aaguids
 */
public class AuthenticatorRegistry {

	/**
	 * Information about an authenticator/password manager
	 */
	public record AuthenticatorInfo(String name, // Human-readable name (e.g., "iCloud
													// Keychain")
			String logoId, // Icon identifier for frontend (e.g., "apple")
			String provider // Provider category (e.g., "apple", "google", "1password")
	) {
	}

	private static final Map<String, AuthenticatorInfo> REGISTRY = new HashMap<>();

	static {
		// Apple
		REGISTRY.put("fbfc3007-154e-4ecc-8c0b-6e020557d7bd",
				new AuthenticatorInfo("iCloud Keychain", "apple", "apple"));
		REGISTRY.put("dd4ec289-e01d-41c9-bb89-70fa845d4bf2",
				new AuthenticatorInfo("iCloud Keychain", "apple", "apple"));
		REGISTRY.put("531126d6-e717-415c-9320-3d9aa6981239",
				new AuthenticatorInfo("iCloud Keychain", "apple", "apple"));

		// Google
		REGISTRY.put("adce0002-35bc-c60a-648b-0b25f1f05503",
				new AuthenticatorInfo("Google Password Manager", "google", "google"));
		REGISTRY.put("ea9b8d66-4d01-1d21-3ce4-b6b48cb575d4",
				new AuthenticatorInfo("Google Password Manager", "google", "google"));
		REGISTRY.put("b5397d10-a3e6-47f6-8f8e-bd3b6d8e1f0a",
				new AuthenticatorInfo("Google Password Manager", "google", "google"));

		// 1Password
		REGISTRY.put("d548826e-79b4-db40-a3d8-11116f7e8349",
				new AuthenticatorInfo("1Password", "1password", "1password"));
		REGISTRY.put("bada5566-a7aa-401f-bd96-45619a55120d",
				new AuthenticatorInfo("1Password", "1password", "1password"));

		// Bitwarden
		REGISTRY.put("d548826e-79b4-db40-a3d8-11116f7e8350",
				new AuthenticatorInfo("Bitwarden", "bitwarden", "bitwarden"));
		REGISTRY.put("aaguidbd-0001-0001-0001-000000000001",
				new AuthenticatorInfo("Bitwarden", "bitwarden", "bitwarden"));

		// Dashlane
		REGISTRY.put("531126d6-e717-415c-9320-3d9aa6981240", new AuthenticatorInfo("Dashlane", "dashlane", "dashlane"));

		// Keeper
		REGISTRY.put("0ea242b4-43c4-4a1b-8b17-dd6d0b6baec6", new AuthenticatorInfo("Keeper", "keeper", "keeper"));

		// LastPass
		REGISTRY.put("fdb141b2-5d84-443e-8a35-4698c205a502", new AuthenticatorInfo("LastPass", "lastpass", "lastpass"));

		// Microsoft
		REGISTRY.put("6028b017-b1d4-4c02-b4b3-afcdafc96bb2",
				new AuthenticatorInfo("Microsoft Authenticator", "microsoft", "microsoft"));
		REGISTRY.put("9ddd1817-af5a-4672-a2b9-3e3dd95000a9",
				new AuthenticatorInfo("Windows Hello", "windows", "microsoft"));

		// Samsung
		REGISTRY.put("53414d53-554e-4700-0000-000000000000",
				new AuthenticatorInfo("Samsung Pass", "samsung", "samsung"));

		// YubiKey (hardware keys)
		REGISTRY.put("2fc0579f-8113-47ea-b116-bb5a8db9202a", new AuthenticatorInfo("YubiKey 5", "yubikey", "yubico"));
		REGISTRY.put("73bb0cd4-e502-49b8-9c6f-b59445bf720b",
				new AuthenticatorInfo("YubiKey 5 NFC", "yubikey", "yubico"));
		REGISTRY.put("c5ef55ff-ad9a-4b9f-b580-adebafe026d0", new AuthenticatorInfo("YubiKey 5C", "yubikey", "yubico"));
		REGISTRY.put("85203421-48f9-4355-9bc8-8a53846e5083", new AuthenticatorInfo("YubiKey 5Ci", "yubikey", "yubico"));

		// Chrome on Android
		REGISTRY.put("b93fd961-f2e6-462f-b122-82002247de78",
				new AuthenticatorInfo("Chrome on Android", "chrome", "google"));

		// Firefox
		REGISTRY.put("a4e9fc6d-4cbe-4758-b8ba-37598bb5bbaa", new AuthenticatorInfo("Firefox", "firefox", "mozilla"));

		// All zeros - generic platform authenticator
		REGISTRY.put("00000000-0000-0000-0000-000000000000",
				new AuthenticatorInfo("Platform Authenticator", "key", "platform"));
	}

	/**
	 * Default authenticator info for unknown AAGUIDs
	 */
	private static final AuthenticatorInfo DEFAULT = new AuthenticatorInfo("Passkey", "key", "unknown");

	/**
	 * Get authenticator information by AAGUID
	 * @param aaguid The authenticator's AAGUID (can be null)
	 * @return AuthenticatorInfo with name, logo, and provider
	 */
	public static AuthenticatorInfo getAuthenticator(String aaguid) {
		if (aaguid == null || aaguid.isBlank()) {
			return DEFAULT;
		}
		return REGISTRY.getOrDefault(aaguid.toLowerCase(), DEFAULT);
	}

	/**
	 * Check if an AAGUID is known
	 */
	public static boolean isKnown(String aaguid) {
		return aaguid != null && REGISTRY.containsKey(aaguid.toLowerCase());
	}

	/**
	 * Get the total number of known authenticators
	 */
	public static int getKnownCount() {
		return REGISTRY.size();
	}

}
