package me.mss1r.siegeworks.gameplay.firearm;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static me.mss1r.siegeworks.gameplay.firearm.FirearmLoad.Operation.*;

class FirearmLoadTest {
    @Test void firingRequirementsDistinguishChargeMatchAndExternalIgniter() {
        assertEquals(FirearmLoad.FireRequirement.CHARGE,FirearmLoad.EMPTY.fireRequirement(false,true));
        var charged=reload(FirearmLoad.EMPTY,true);
        assertEquals(FirearmLoad.FireRequirement.IGNITER,charged.fireRequirement(false,false));
        assertEquals(FirearmLoad.FireRequirement.NONE,charged.fireRequirement(false,true));
        assertEquals(FirearmLoad.FireRequirement.MATCH,charged.fireRequirement(true,true));
        var matched=charged.complete(INSERT_MATCH,true);
        assertEquals(FirearmLoad.FireRequirement.LIGHT,matched.fireRequirement(true,true));
        assertEquals(FirearmLoad.FireRequirement.NONE,matched.complete(LIGHT_MATCH,true).fireRequirement(true,false));
    }
    private static FirearmLoad reload(FirearmLoad load, boolean matchlock) {
        return load.complete(POWDER, matchlock).complete(BALL, matchlock).complete(RAM, matchlock);
    }

    @Test void cannotFirePowderOrAnUnrammedBall() {
        var empty = FirearmLoad.EMPTY;
        assertFalse(empty.accepts(FIRE, false));
        assertFalse(empty.accepts(BALL, false));
        assertFalse(empty.accepts(RAM, false));
        var powder = empty.complete(POWDER, false);
        assertFalse(powder.accepts(FIRE, false));
        assertFalse(powder.accepts(POWDER, false));
        var ball = powder.complete(BALL, false);
        assertFalse(ball.accepts(FIRE, false));
        assertTrue(ball.complete(RAM, false).accepts(FIRE, false));
    }

    @Test void matchLastsExactlySixteenSuccessfulShots() {
        var load = FirearmLoad.EMPTY.complete(INSERT_MATCH, true).complete(LIGHT_MATCH, true);
        for (int shot = 0; shot < 16; shot++) {
            load = reload(load, true).complete(FIRE, true);
            assertEquals(15 - shot, load.matchShots());
            assertEquals(shot < 15, load.matchLit());
        }
        var loaded = reload(load, true);
        assertFalse(loaded.accepts(FIRE, true));
        assertThrows(IllegalStateException.class, () -> loaded.complete(FIRE, true));
    }

    @Test void extinguishingDoesNotUseUpTheMatchOrTheCharge() {
        var load = reload(FirearmLoad.EMPTY, true).complete(INSERT_MATCH, true).complete(LIGHT_MATCH, true);
        for (int i = 0; i < 30; i++) {
            load = load.extinguish();
            assertFalse(load.accepts(FIRE, true));
            load = load.complete(LIGHT_MATCH, true);
        }
        assertEquals(16, load.matchShots());
        assertEquals(FirearmLoad.Charge.READY, load.charge());
    }

    @Test void handCannonDoesNotHaveAMatchAndCannotFireTwice() {
        assertFalse(FirearmLoad.EMPTY.accepts(INSERT_MATCH, false));
        var fired = reload(FirearmLoad.EMPTY, false).complete(FIRE, false);
        assertEquals(FirearmLoad.EMPTY, fired);
        assertThrows(IllegalStateException.class, () -> fired.complete(FIRE, false));
    }
}
