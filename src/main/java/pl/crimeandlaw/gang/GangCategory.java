package pl.crimeandlaw.gang;

import java.util.*;

/** Stable category IDs; Transport is deliberately unavailable until developed. */
public enum GangCategory {
    GANG(0,"Gang",Placement.NAVIGATION),
    MEMBERS(1,"Członkowie",Placement.NAVIGATION),
    REGISTER(4,"Rejestr",Placement.NAVIGATION),
    TERRITORY(2,"Mapa",Placement.NAVIGATION),
    BONUSES(6,"Premie",Placement.NAVIGATION),
    BALANCE(7,"Zarządzanie saldem",Placement.NAVIGATION),
    BLACKLIST(8,"Czarna lista",Placement.NAVIGATION),
    GOALS(9,"Cele",Placement.NAVIGATION),
    SETTINGS(3,"Ustawienia",Placement.FOOTER),
    TRANSPORT(5,"Transport",Placement.HIDDEN);
    public enum Placement { NAVIGATION, FOOTER, HIDDEN }
    private final int tabId;private final String label;private final Placement placement;
    GangCategory(int tabId,String label,Placement placement){this.tabId=tabId;this.label=label;this.placement=placement;}
    public int tabId(){return tabId;}
    public String label(){return label;}
    public Placement placement(){return placement;}
    public boolean enabled(){return placement!=Placement.HIDDEN;}
    public static List<GangCategory> navigation(){return Arrays.stream(values()).filter(c->c.placement==Placement.NAVIGATION).toList();}
    public static GangCategory visibleOrOverview(int id){return Arrays.stream(values()).filter(c->c.tabId==id&&c.enabled()).findFirst().orElse(GANG);}
}
