// Test to verify what ProviderStatus.CONNECTED.getValue() returns
public class TestEnumValue {
    public static void main(String[] args) {
        // This is what should happen:
        // ProviderStatus.CONNECTED has value "connected" (lowercase)
        // When entity.setStatus(ProviderStatus.CONNECTED) is called,
        // it should set the string field to "connected"
        
        System.out.println("ProviderStatus.CONNECTED.getValue() should return: connected");
        System.out.println("ProviderStatus.CONNECTED.name() returns: CONNECTED");
        System.out.println("ProviderStatus.CONNECTED.toString() should return: connected");
        
        // The issue is that Firestore might be using reflection
        // and getting the enum name() instead of getValue()
    }
}