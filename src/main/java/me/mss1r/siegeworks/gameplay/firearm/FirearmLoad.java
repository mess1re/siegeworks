package me.mss1r.siegeworks.gameplay.firearm;

/** Persistent charge and match state. Animation progress belongs to the wielder, not the item. */
public record FirearmLoad(Charge charge, int matchShots, boolean matchLit) {
    public static final int SHOTS_PER_MATCH = 16;
    public static final FirearmLoad EMPTY = new FirearmLoad(Charge.EMPTY, 0, false);

    public enum Charge { EMPTY, POWDER, BALL, READY }
    public enum FireRequirement { CHARGE, MATCH, LIGHT, IGNITER, NONE }
    public enum Operation { POWDER, BALL, RAM, INSERT_MATCH, LIGHT_MATCH, EXTINGUISH_MATCH, FIRE }

    public FirearmLoad {
        if (charge == null || matchShots < 0 || matchShots > SHOTS_PER_MATCH || (matchLit && matchShots == 0)) {
            throw new IllegalArgumentException("Invalid firearm load");
        }
    }

    public boolean accepts(Operation operation, boolean matchlock) {
        return switch (operation) {
            case POWDER -> charge == Charge.EMPTY;
            case BALL -> charge == Charge.POWDER;
            case RAM -> charge == Charge.BALL;
            case INSERT_MATCH -> matchlock && matchShots == 0;
            case LIGHT_MATCH -> matchlock && matchShots > 0 && !matchLit;
            case EXTINGUISH_MATCH -> matchlock && matchLit;
            case FIRE -> charge == Charge.READY && (!matchlock || matchLit);
        };
    }

    public FireRequirement fireRequirement(boolean matchlock, boolean igniterInOffhand) {
        if (charge != Charge.READY) return FireRequirement.CHARGE;
        if (matchlock) {
            if (matchShots == 0) return FireRequirement.MATCH;
            if (!matchLit) return FireRequirement.LIGHT;
        } else if (!igniterInOffhand) return FireRequirement.IGNITER;
        return FireRequirement.NONE;
    }

    /** Called only when the action commits; starting or cancelling an animation never consumes a charge. */
    public FirearmLoad complete(Operation operation, boolean matchlock) {
        if (!accepts(operation, matchlock)) throw new IllegalStateException("Cannot " + operation + " with " + this);
        return switch (operation) {
            case POWDER -> new FirearmLoad(Charge.POWDER, matchShots, matchLit);
            case BALL -> new FirearmLoad(Charge.BALL, matchShots, matchLit);
            case RAM -> new FirearmLoad(Charge.READY, matchShots, matchLit);
            case INSERT_MATCH -> new FirearmLoad(charge, SHOTS_PER_MATCH, false);
            case LIGHT_MATCH -> new FirearmLoad(charge, matchShots, true);
            case EXTINGUISH_MATCH -> extinguish();
            case FIRE -> new FirearmLoad(Charge.EMPTY, matchlock ? matchShots - 1 : 0,
                    matchlock && matchShots > 1);
        };
    }

    public FirearmLoad extinguish() { return new FirearmLoad(charge, matchShots, false); }
}
