package pl.crimeandlaw.gang;

import com.google.gson.Gson;
import java.util.*;

/** Server-owned domain model, independent of Minecraft rendering and networking. */
public final class GangDirectory {
    private static final Gson JSON = new Gson();
    public static final int MAX_MEMBERS = 64;
    public static final int MAX_GANGS = 512;
    public enum Permission { INVITE, MANAGE_MEMBERS, MANAGE_ROLES, MANAGE_THEME }
    public record Theme(int background, int border, int opacity) {
        public static final Theme DEFAULT = new Theme(0x15161C, 0xA3425D, 235);
        public Theme { background &= 0xFFFFFF; border &= 0xFFFFFF; opacity = Math.clamp(opacity, 0, 255); }
        public int backgroundArgb() { return opacity << 24 | background; }
        public int borderArgb() { return 0xFF000000 | border; }
    }
    public static final class Role {
        public String id, name;
        public Set<Permission> permissions = new HashSet<>();
        public Role(String id, String name, Set<Permission> permissions) {
            this.id=id; this.name=name; this.permissions=new HashSet<>(permissions);
        }
    }
    public static final class Member {
        public UUID id;
        public String name, role;
        public Member(UUID id,String name,String role){this.id=id;this.name=name;this.role=role;}
    }
    public static final class Gang {
        public UUID id, owner;
        public String name, logo="";
        public Theme theme=Theme.DEFAULT;
        public Map<UUID,Member> members=new LinkedHashMap<>();
        public Map<String,Role> roles=new LinkedHashMap<>();
        public Map<UUID,String> requests=new LinkedHashMap<>();
    }
    private final Map<UUID,Gang> gangs=new LinkedHashMap<>();
    public Collection<Gang> gangs(){return Collections.unmodifiableCollection(gangs.values());}
    public Gang get(UUID id){return gangs.get(id);}
    public Gang of(UUID player){return gangs.values().stream().filter(g->g.members.containsKey(player)).findFirst().orElse(null);}
    public boolean allowed(Gang g,UUID actor,Permission permission){
        if(g==null)return false;
        if(g.owner.equals(actor))return true;
        Member m=g.members.get(actor);Role r=m==null?null:g.roles.get(m.role);
        return r!=null && r.permissions.contains(permission);
    }
    public Gang create(UUID actor,String playerName,String name,String logo){
        require(of(actor)==null,"Należysz już do gangu.");
        name=validName(name,3,32);
        require(gangs.size()<MAX_GANGS,"Osiągnięto limit gangów tego świata.");
        final String chosen=name;
        require(gangs.values().stream().noneMatch(g->g.name.equalsIgnoreCase(chosen)),"Ta nazwa jest już zajęta.");
        Gang g=new Gang();g.id=UUID.randomUUID();g.owner=actor;g.name=name;g.logo=logo;
        g.roles.put("leader",new Role("leader","Lider",EnumSet.allOf(Permission.class)));
        g.roles.put("member",new Role("member","Członek",Set.of()));
        g.members.put(actor,new Member(actor,playerName,"leader"));gangs.put(g.id,g);return g;
    }
    public void request(UUID actor,String name,UUID id){
        require(of(actor)==null,"Należysz już do gangu.");Gang g=required(id);
        require(g.members.size()<MAX_MEMBERS,"Gang jest pełny.");
        require(g.requests.size()<128 || g.requests.containsKey(actor),"Lista zgłoszeń jest pełna.");
        g.requests.put(actor,name);
    }
    public void accept(UUID actor,UUID target){
        Gang g=own(actor,Permission.INVITE);
        String name=g.requests.get(target);require(name!=null,"Nie ma takiego zgłoszenia.");
        require(of(target)==null,"Ten gracz należy już do gangu.");
        require(g.members.size()<MAX_MEMBERS,"Gang jest pełny.");
        g.members.put(target,new Member(target,name,"member"));
        for(Gang other:gangs.values())other.requests.remove(target);
    }
    public void reject(UUID actor,UUID target){own(actor,Permission.INVITE).requests.remove(target);}
    public void leave(UUID actor){
        Gang g=of(actor);require(g!=null,"Nie należysz do gangu.");
        require(!g.owner.equals(actor) || g.members.size()==1,"Najpierw przekaż przywództwo innemu członkowi.");
        g.members.remove(actor);if(g.members.isEmpty())gangs.remove(g.id);
    }
    public void kick(UUID actor,UUID target){
        Gang g=own(actor,Permission.MANAGE_MEMBERS);
        require(!g.owner.equals(target),"Nie można usunąć lidera.");
        require(!actor.equals(target),"Użyj przycisku opuszczenia gangu.");
        require(g.members.remove(target)!=null,"Nie ma takiego członka.");
    }
    public void transfer(UUID actor,UUID target){
        Gang g=of(actor);require(g!=null && g.owner.equals(actor),"Tylko lider może przekazać przywództwo.");
        require(!actor.equals(target) && g.members.containsKey(target),"Wybierz innego członka.");
        g.members.get(actor).role="member";g.members.get(target).role="leader";g.owner=target;
    }
    public void role(UUID actor,String roleId,String name,Set<Permission> permissions){
        Gang g=own(actor,Permission.MANAGE_ROLES);
        require(!"leader".equals(roleId),"Uprawnienia lidera są stałe.");
        if(roleId==null || roleId.isBlank()){
            require(g.roles.size()<12,"Maksymalnie 12 rang.");roleId=UUID.randomUUID().toString();
        }else require(g.roles.containsKey(roleId),"Nie ma takiej rangi.");
        name=validName(name,2,24);
        g.roles.put(roleId,new Role(roleId,name,permissions));
    }
    public void assign(UUID actor,UUID target,String role){
        Gang g=own(actor,Permission.MANAGE_ROLES);
        require(g.members.containsKey(target),"Nie ma takiego członka.");
        require(!g.owner.equals(target) && !"leader".equals(role),"Lidera zmienia się przez przekazanie przywództwa.");
        require(g.roles.containsKey(role),"Nie ma takiej rangi.");g.members.get(target).role=role;
    }
    /** Visual order only: no role gains permissions or ownership by moving up. */
    public void reorderRoles(UUID actor,List<String> order){
        Gang g=own(actor,Permission.MANAGE_ROLES);
        require(order!=null&&order.size()==g.roles.size()&&new HashSet<>(order).size()==order.size()
                &&g.roles.keySet().containsAll(order),"Kolejność musi zawierać każdą rangę dokładnie raz.");
        Map<String,Role> sorted=new LinkedHashMap<>();for(String id:order)sorted.put(id,g.roles.get(id));g.roles=sorted;
    }
    public void theme(UUID actor,Theme theme){own(actor,Permission.MANAGE_THEME).theme=theme;}
    public void logo(UUID actor,String logo){own(actor,Permission.MANAGE_THEME).logo=logo;}
    public void renameSeenPlayer(UUID player,String name){
        Gang g=of(player);if(g!=null)g.members.get(player).name=name;
        for(Gang other:gangs.values())if(other.requests.containsKey(player))other.requests.put(player,name);
    }
    private Gang own(UUID actor,Permission permission){
        Gang g=of(actor);require(allowed(g,actor,permission),"Nie masz uprawnień do tej czynności.");return g;
    }
    private Gang required(UUID id){Gang g=gangs.get(id);require(g!=null,"Nie ma takiego gangu.");return g;}
    private static String validName(String name,int min,int max){
        require(name!=null,"Podaj nazwę.");name=name.strip().replaceAll("\\s+"," ");
        require(name.length()>=min && name.length()<=max,"Nazwa musi mieć od "+min+" do "+max+" znaków.");
        require(name.matches("[\\p{L}\\p{N} _'\\-]+"),"Nazwa może zawierać litery, cyfry, spacje, myślniki i podkreślenia.");return name;
    }
    private static void require(boolean ok,String message){if(!ok)throw new IllegalArgumentException(message);}
    public String toJson(){return JSON.toJson(this);}
    public static GangDirectory fromJson(String text){
        GangDirectory directory=JSON.fromJson(text,GangDirectory.class);
        if(directory==null || directory.gangs==null || directory.gangs.size()>MAX_GANGS)throw new IllegalArgumentException("Nieprawidłowy zapis gangów.");
        Set<UUID> seen=new HashSet<>();
        for(var entry:directory.gangs.entrySet()){
            Gang g=entry.getValue();
            require(g!=null && entry.getKey().equals(g.id) && g.owner!=null,"Uszkodzony identyfikator gangu.");
            validName(g.name,3,32);
            require(g.members!=null && g.members.size()<=MAX_MEMBERS && g.members.containsKey(g.owner),"Uszkodzone członkostwo.");
            require(g.roles!=null && g.roles.containsKey("leader") && g.roles.containsKey("member") && g.roles.size()<=12,"Uszkodzone rangi.");
            for(var member:g.members.entrySet()){
                require(member.getValue()!=null && member.getKey().equals(member.getValue().id) && seen.add(member.getKey()),"Powielone członkostwo.");
                require(g.roles.containsKey(member.getValue().role),"Nieznana ranga członka.");
                require(member.getKey().equals(g.owner)=="leader".equals(member.getValue().role),"Uszkodzona ranga lidera.");
            }
            require(g.theme!=null && g.requests!=null && g.requests.size()<=128 && g.logo!=null && g.logo.length()<=27000,"Uszkodzone ustawienia gangu.");
        }
        return directory;
    }
}
