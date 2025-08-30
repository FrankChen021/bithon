import java.lang.reflect.Method;

public class TestDirectMemory {
    
    public static void main(String[] args) {
        System.out.println("Testing DirectMemory implementations...");
        
        // Test JDK 9+ implementation
        try {
            Class<?> vmClass = Class.forName("jdk.internal.misc.VM");
            Method maxDirectMemoryMethod = vmClass.getDeclaredMethod("maxDirectMemory");
            maxDirectMemoryMethod.setAccessible(true);
            long maxDirectMemory = (Long) maxDirectMemoryMethod.invoke(null);
            System.out.println("JDK 9+ implementation works: " + maxDirectMemory);
        } catch (Exception e) {
            System.out.println("JDK 9+ implementation failed: " + e.getMessage());
            
            // Test JDK 8 implementation
            try {
                Class<?> vmClass = Class.forName("sun.misc.VM");
                Method maxDirectMemoryMethod = vmClass.getDeclaredMethod("maxDirectMemory");
                maxDirectMemoryMethod.setAccessible(true);
                long maxDirectMemory = (Long) maxDirectMemoryMethod.invoke(null);
                System.out.println("JDK 8 implementation works: " + maxDirectMemory);
            } catch (Exception e2) {
                System.out.println("JDK 8 implementation also failed: " + e2.getMessage());
            }
        }
    }
}
