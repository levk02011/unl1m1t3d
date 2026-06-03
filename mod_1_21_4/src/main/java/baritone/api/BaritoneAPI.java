package baritone.api;

/**
 * Stub implementation of BaritoneAPI for build compatibility
 * This is a placeholder until the actual Baritone API dependency is resolved
 */
public class BaritoneAPI {
    private static IBaritoneProvider provider = new MockBaritoneProvider();
    
    public static IBaritoneProvider getProvider() {
        return provider;
    }
    
    private static class MockBaritoneProvider implements IBaritoneProvider {
        private IBaritone baritone = new MockBaritone();
        
        @Override
        public IBaritone getPrimaryBaritone() {
            return baritone;
        }
    }
}

public interface IBaritoneProvider {
    IBaritone getPrimaryBaritone();
}
