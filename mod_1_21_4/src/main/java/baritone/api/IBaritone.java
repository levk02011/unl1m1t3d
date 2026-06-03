import baritone.api.behavior.IPathingBehavior;
import baritone.api.behavior.ILookBehavior;
import baritone.api.behavior.MockPathingBehavior;
import baritone.api.behavior.MockLookBehavior;
import baritone.api.process.ICustomGoalProcess;
import baritone.api.process.MockCustomGoalProcess;

/**
 * Stub implementation of IBaritone interface for build compatibility
 */
public interface IBaritone {
    IPathingBehavior getPathingBehavior();
    ILookBehavior getLookBehavior();
    ICustomGoalProcess getCustomGoalProcess();
}

// Mock implementation
class MockBaritone implements IBaritone {
    private IPathingBehavior pathingBehavior = new MockPathingBehavior();
    private ILookBehavior lookBehavior = new MockLookBehavior();
    private ICustomGoalProcess goalProcess = new MockCustomGoalProcess();
    
    @Override
    public IPathingBehavior getPathingBehavior() {
        return pathingBehavior;
    }
    
    @Override
    public ILookBehavior getLookBehavior() {
        return lookBehavior;
    }
    
    @Override
    public ICustomGoalProcess getCustomGoalProcess() {
        return goalProcess;
    }
}
