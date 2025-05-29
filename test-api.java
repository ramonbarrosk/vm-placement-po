import org.cloudsimplus.hosts.HostSuitability;

public class TestAPI {
    public static void main(String[] args) {
        // Test HostSuitability constants and methods
        System.out.println("HostSuitability fields and methods:");
        
        // Try different ways to create unsuitable result
        try {
            // Method 1: Try static factory methods
            System.out.println("Trying HostSuitability.create methods...");
            
            // Method 2: Try constants
            System.out.println("Available constants:");
            // Print all available constants/fields
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
} 