package me.mss1r.siegeworks.gameplay.upgrade;

/** Retains the tower storage key and the existing integration API. */
public final class TowerBannerRack extends SiegeBannerRack {
    public TowerBannerRack(Host host, int slots) {
        super(host, slots, "TowerBanners");
    }

    public interface Host extends SiegeBannerRack.Host {
    }
}
