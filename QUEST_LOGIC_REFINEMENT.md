# Quest Logic Deep Refinement Plan

## Critical Issues Identified

### 1. **CRITICAL BUG: Anti-Cheese System Not Working**
**Location**: `BlockListener.java:160-164`
```java
private boolean isPlayerPlaced(Block block) {
    // TODO: Check if block was placed by a player using the database
    // For now, return false (assume all blocks are naturally generated)
    return false;  // ← ALWAYS RETURNS FALSE!
}
```
**Impact**: Players can place/break blocks infinitely for quest progress
**Priority**: CRITICAL - Must fix immediately

---

### 2. **DOUBLE PROGRESS BUG: Duplicate Tracking**
**Location**: `CombatListener.java:58-74`
- Line 58-60: Tracks mob kill (e.g., CREEPER)
- Lines 64-74: ALSO tracks each item dropped (e.g., LEATHER)
**Impact**: Quest "Hunt Leather" progresses when killing creeper + when leather drops = 2x progress
**Priority**: HIGH - Causing incorrect quest progression

---

### 3. **Progress Validation Missing**
**Location**: `DailyQuest.java:72-80`
```java
public void addProgress(int amount) {
    if (!completed) {
        currentProgress += amount;  // No validation!
    }
}
```
**Issues**:
- No check for negative amounts
- No overflow protection
- No thread safety
- Can exceed targetAmount before check
**Priority**: MEDIUM - Could cause data corruption

---

### 4. **No Database Cleanup**
**Location**: `placed_blocks` table
- No TTL (time-to-live) system
- Will grow infinitely
- No cleanup of old entries
**Priority**: MEDIUM - Performance degradation over time

---

### 5. **Quest Completion Not Atomic**
**Location**: Various reward claim methods
- Mark complete → Give rewards = 2 separate operations
- Server crash between them = lost rewards
**Priority**: MEDIUM - Rare but serious

---

## Refinement Plan (Inspired by BetonQuest & Quests)

### Phase 1: Fix Critical Bugs ✅

#### A. Implement Anti-Cheese System
- [ ] Fix `isPlayerPlaced()` to actually check database
- [ ] Add caching layer for recently placed blocks (performance)
- [ ] Add config option for tracking duration
- [ ] Add automatic cleanup of old entries

#### B. Fix Double Progress Bug
- [ ] Separate quest types: MOB_KILL vs ITEM_COLLECTION
- [ ] Ensure quests only progress for their specific type
- [ ] Add quest target validation

### Phase 2: Add Validation & Safety

#### A. Progress Validation
- [ ] Validate amount > 0
- [ ] Check for integer overflow
- [ ] Add max progress cap
- [ ] Add thread-safe progress updates

#### B. Quest State Validation
- [ ] Validate quest transitions (ACTIVE → COMPLETED → CLAIMED)
- [ ] Prevent invalid state changes
- [ ] Add logging for suspicious activity

#### C. Reward Transaction Safety
- [ ] Implement transaction pattern for rewards
- [ ] Add rollback capability
- [ ] Log all reward operations
- [ ] Add duplicate claim prevention

### Phase 3: Enhanced Features (BetonQuest-inspired)

#### A. Quest Event System
- [ ] Create QuestProgressEvent
- [ ] Create QuestCompleteEvent
- [ ] Create QuestClaimEvent
- [ ] Allow other plugins to hook into quest system

#### B. Advanced Requirement Matching
- [ ] Improve `isMatchingCategory()` logic
- [ ] Add regex support for materials
- [ ] Add world/biome restrictions
- [ ] Add time-based requirements

#### C. Quest Conditions System
- [ ] Add prerequisite quests
- [ ] Add level requirements
- [ ] Add permission requirements
- [ ] Add cooldown system

### Phase 4: Performance Optimizations

#### A. Database Optimization
- [ ] Add connection pooling validation
- [ ] Add batch update support
- [ ] Add prepared statement caching
- [ ] Add query optimization

#### B. Caching Strategy
- [ ] Cache active quests in memory
- [ ] Cache player data
- [ ] Invalidate cache on updates
- [ ] Add cache statistics

#### C. Async Processing
- [ ] Move all DB operations to async
- [ ] Add queue system for progress updates
- [ ] Batch similar operations
- [ ] Add timeout handling

### Phase 5: Debug & Monitoring

#### A. Comprehensive Logging
- [ ] Add debug mode for quest tracking
- [ ] Log all progress updates
- [ ] Log all state transitions
- [ ] Add performance metrics

#### B. Admin Tools
- [ ] Add `/wqadmin debug <player>` command
- [ ] Add quest state inspection
- [ ] Add manual progress adjustment
- [ ] Add quest reset capability

---

## Implementation Order (Priority)

1. ✅ Fix anti-cheese system (CRITICAL)
2. ✅ Fix double progress bug (HIGH)
3. ✅ Add progress validation (HIGH)
4. ✅ Add quest event system (MEDIUM)
5. ✅ Add database cleanup (MEDIUM)
6. ✅ Add transaction safety (MEDIUM)
7. ⏸️ Performance optimizations (LOW - if needed)
8. ⏸️ Advanced features (LOW - future enhancement)

---

## Testing Checklist

### Anti-Cheese Testing
- [ ] Place and break same block → should NOT count
- [ ] Break naturally generated block → should count
- [ ] Multiple players placing blocks → tracked separately
- [ ] Old placed blocks (>24h) → should count again

### Progress Testing
- [ ] Normal progress → works correctly
- [ ] Negative progress attempt → rejected
- [ ] Overflow attempt → capped correctly
- [ ] Concurrent updates → no data loss

### Reward Testing
- [ ] Complete quest → rewards given once
- [ ] Server crash during claim → no reward loss
- [ ] Duplicate claim attempt → rejected
- [ ] Multiple quests completed → all rewards given

---

## Reference Implementations

### BetonQuest Patterns
- Event-driven architecture
- Flexible condition system
- Transaction-safe objectives
- Comprehensive validation

### PikaMug's Quests Patterns
- Simple but robust state machine
- Clear separation of concerns
- Good error handling
- User-friendly feedback

---

## Notes

### Database Schema Improvements Needed
```sql
-- Add index for placed block lookups
CREATE INDEX IF NOT EXISTS idx_placed_blocks_coords 
ON placed_blocks(world, x, y, z);

-- Add index for quest lookups
CREATE INDEX IF NOT EXISTS idx_player_quests_status 
ON player_quests(player_uuid, status);

-- Add index for global quest contributions
CREATE INDEX IF NOT EXISTS idx_global_quests_player 
ON global_quests_contributions(quest_id, player_uuid);
```

### Config Additions Needed
```yaml
quests:
  anti-cheese:
    track-placed-blocks: true
    tracking-duration-hours: 24
    cache-size: 1000
    cleanup-interval-minutes: 60
  
  validation:
    max-progress-per-update: 1000
    enable-overflow-protection: true
    enable-negative-protection: true
  
  debug:
    enabled: false
    log-progress-updates: false
    log-state-transitions: true
```

---

**Status**: Ready for implementation
**Estimated Time**: 2-3 hours for Phase 1-3
**Risk Level**: Low (mostly additions, minimal breaking changes)

