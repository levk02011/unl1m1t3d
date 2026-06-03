package baritone.api.behavior;

/**
 * Stub implementation for IPathingBehavior
 */
public class MockPathingBehavior implements IPathingBehavior {
    @Override
    public boolean isPathing() {
        return false;
    }
    
    @Override
    public void cancelEverything() {
    }
}