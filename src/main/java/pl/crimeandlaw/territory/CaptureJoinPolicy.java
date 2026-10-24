package pl.crimeandlaw.territory;

import java.util.UUID;
import pl.crimeandlaw.gang.GangDirectory.Gang;

/** A new rival on neutral land needs a real border and the gang leader's decision. */
public final class CaptureJoinPolicy {
    private CaptureJoinPolicy(){}
    public static boolean challenge(CaptureBoard.Battle battle,Gang gang){
        return battle.previousOwner==null&&!battle.teams().contains(gang.id);
    }
    public static String problem(CaptureBoard.Battle battle,Gang gang,UUID actor,TerrainAtlas atlas){
        if(!challenge(battle,gang))return "";
        if(!gang.owner.equals(actor))return "O ataku na neutralne pole decyduje lider.";
        if(!atlas.neighbours(gang.id,battle.key,true))return "Gang musi mieć teren obok, także po skosie.";
        return "";
    }
}
