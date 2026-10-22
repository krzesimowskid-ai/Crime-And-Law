package pl.crimeandlaw.gang;

import com.google.gson.Gson;
import com.google.gson.JsonParser;
import java.util.*;

/** Server-owned domain model, independent of Minecraft rendering and networking. */
public final class GangDirectory {
    private static final Gson JSON = new Gson();
    public static final int MAX_GANGS = 512;
    public static final int MAX_ROLES=12;
    public static final int HISTORY_LIMIT=10000;
    public enum PermissionGroup {
        ALL("Wszystkie"), PERSONNEL("Personel"), PROPERTY("Mienie i zaopatrzenie"), ACTIVITIES("Aktywności"), FINANCES("Finanse"), TRANSPORT("Transport");
        public final String label;PermissionGroup(String label){this.label=label;}
    }
    public enum Permission {
        INVITE("Zapraszanie / zgłoszenia",PermissionGroup.PERSONNEL,true),
        MANAGE_MEMBERS("Usuwanie członków",PermissionGroup.PERSONNEL,true),
        MANAGE_ROLES("Zarządzanie rangami",PermissionGroup.PERSONNEL,true),
        MANAGE_THEME("Wygląd i logo gangu",PermissionGroup.PROPERTY,true),
        ASSIGN_ROLES("Zmiana rang członków",PermissionGroup.PERSONNEL,true),
        WARN_MEMBERS("Wydawanie ostrzeżeń",PermissionGroup.PERSONNEL,false),
        ACCESS_STORAGE("Dostęp do magazynu",PermissionGroup.PROPERTY,false),
        CRAFT_EQUIPMENT("Tworzenie przedmiotów z materiałów",PermissionGroup.PROPERTY,false),
        DEPOSIT_FUNDS("Wpłata środków na saldo",PermissionGroup.FINANCES,false),
        WITHDRAW_FUNDS("Wypłata środków z salda",PermissionGroup.FINANCES,false),
        PAY_BONUSES("Wypłacanie premii",PermissionGroup.FINANCES,false),
        RESET_VEHICLES("Resetowanie pojazdów gangowych",PermissionGroup.TRANSPORT,false),
        START_TERRITORY_ATTACK("Rozpoczynanie ataku na terytorium",PermissionGroup.ACTIVITIES,false),
        BANK_ROBBERY("Dostęp do napadu na bank",PermissionGroup.ACTIVITIES,false),
        JOIN_TERRITORY_ATTACK("Udział w ataku na terytorium",PermissionGroup.ACTIVITIES,false),
        MANAGE_BLACKLIST("Zarządzanie czarną listą",PermissionGroup.PERSONNEL,false),
        EDIT_OUTFITS("Edytowanie zestawów ubrań",PermissionGroup.PROPERTY,false),
        CREATE_OUTFITS("Tworzenie zestawów ubrań",PermissionGroup.PROPERTY,false),
        OPEN_AIRDROP("Otwarcie AirDrop",PermissionGroup.ACTIVITIES,false),
        MANAGE_QUICK_SETS("Zarządzanie szybkimi zestawami",PermissionGroup.PROPERTY,false),
        OPEN_CLOSE_QUICK_SETS("Otwieranie / zamykanie szybkich zestawów",PermissionGroup.PROPERTY,false),
        MANAGE_DEPARTMENTS("Tworzenie i usuwanie działów",PermissionGroup.PERSONNEL,false),
        ASSIGN_DEPARTMENTS("Przydzielanie działów",PermissionGroup.PERSONNEL,false),
        COLLECT_RAID_EVIDENCE("Zbieranie dowodów podczas rajdu",PermissionGroup.ACTIVITIES,false),
        PREPARE_CANCEL_RAID("Przygotowania do rajdu / anulowanie",PermissionGroup.ACTIVITIES,false);
        public final String label;public final PermissionGroup group;private final boolean available;
        Permission(String label,PermissionGroup group,boolean available){this.label=label;this.group=group;this.available=available;}
        public boolean available(){return available;}
        public static EnumSet<Permission> activePermissions(){var set=EnumSet.allOf(Permission.class);set.removeIf(p->!p.available);return set;}
    }
    public record Event(long timestamp,String text) {}
    public record Theme(int background, int border, int opacity) {
        public static final Theme DEFAULT = new Theme(0x15161C, 0xA3425D, 235);
        public Theme { background &= 0xFFFFFF; border &= 0xFFFFFF; opacity = Math.clamp(opacity, 0, 255); }
        public int backgroundArgb() { return opacity << 24 | background; }
        public int borderArgb() { return 0xFF000000 | border; }
    }
    public static final class Role {
        public String id,name;
        public Set<Permission> permissions=EnumSet.noneOf(Permission.class);
        public Role(String id,String name,Set<Permission> permissions){this.id=id;this.name=name;this.permissions=permissions.isEmpty()?EnumSet.noneOf(Permission.class):EnumSet.copyOf(permissions);}
    }
    public static final class Member {
        public UUID id;
        public String name,role;
        public long joinedAt;
        public boolean joinedAtFromHistory;
        public String textures="",textureSignature="";
        public Member(UUID id,String name){this(id,name,"member");}
        public Member(UUID id,String name,String role){this.id=id;this.name=name;this.role=role;}
    }
    public static final class Gang {
        public UUID id, owner;
        public String name, logo="";
        public Theme theme=Theme.DEFAULT;
        public GangBanner banner=GangBanner.DEFAULT;
        public Map<UUID,Member> members=new LinkedHashMap<>();
        public Map<String,Role> roles=new LinkedHashMap<>();
        public Map<UUID,String> requests=new LinkedHashMap<>();
        public List<Event> history=new ArrayList<>();
    }
    private final Map<UUID,Gang> gangs=new LinkedHashMap<>();
    private int schemaVersion=2;
    private transient boolean migrated;
    public boolean needsMigration(){return migrated;}
    public Collection<Gang> gangs(){return Collections.unmodifiableCollection(gangs.values());}
    public Gang get(UUID id){return gangs.get(id);}
    public Gang of(UUID player){return gangs.values().stream().filter(g->g.members.containsKey(player)).findFirst().orElse(null);}
    public boolean isLeader(Gang g,UUID actor){return g!=null&&g.owner.equals(actor)&&g.members.containsKey(actor);}
    public static boolean allowed(Gang g,UUID actor,Permission permission){
        if(!permission.available()||g==null||!g.members.containsKey(actor))return false;
        if(g.owner.equals(actor))return true;
        Role role=g.roles.get(g.members.get(actor).role);
        return role!=null&&role.permissions.contains(permission);
    }
    private static void defaultRoles(Gang g){
        if(g.roles==null)g.roles=new LinkedHashMap<>();
        g.roles.putIfAbsent("leader",new Role("leader","Lider",Permission.activePermissions()));
        g.roles.putIfAbsent("member",new Role("member","Członek",Set.of()));
    }
    public Gang create(UUID actor,String playerName,String name,String logo){
        require(of(actor)==null,"Należysz już do gangu.");
        name=validName(name,3,32);
        require(gangs.size()<MAX_GANGS,"Osiągnięto limit gangów tego świata.");
        final String chosen=name;
        require(gangs.values().stream().noneMatch(g->g.name.equalsIgnoreCase(chosen)),"Ta nazwa jest już zajęta.");
        Gang g=new Gang();g.id=UUID.randomUUID();g.owner=actor;g.name=name;g.logo=logo;
        defaultRoles(g);g.members.put(actor,new Member(actor,playerName,"leader"));g.members.get(actor).joinedAt=System.currentTimeMillis();gangs.put(g.id,g);record(g,playerName+" tworzy gang.");return g;
    }
    public void request(UUID actor,String name,UUID id){
        require(of(actor)==null,"Należysz już do gangu.");Gang g=required(id);
        g.requests.put(actor,name);
    }
    public void accept(UUID actor,UUID target){
        Gang g=own(actor,Permission.INVITE);
        String name=g.requests.get(target);require(name!=null,"Nie ma takiego zgłoszenia.");
        require(of(target)==null,"Ten gracz należy już do gangu.");
        g.members.put(target,new Member(target,name));g.members.get(target).joinedAt=System.currentTimeMillis();record(g,name+" dołącza do gangu.");
        for(Gang other:gangs.values())other.requests.remove(target);
    }
    public void reject(UUID actor,UUID target){own(actor,Permission.INVITE).requests.remove(target);}
    public void leave(UUID actor){
        Gang g=of(actor);require(g!=null,"Nie należysz do gangu.");
        require(!g.owner.equals(actor) || g.members.size()==1,"Najpierw przekaż przywództwo innemu członkowi.");
        record(g,g.members.get(actor).name+" opuszcza gang.");
        g.members.remove(actor);if(g.members.isEmpty())gangs.remove(g.id);
    }
    public void kick(UUID actor,UUID target){
        Gang g=own(actor,Permission.MANAGE_MEMBERS);
        require(!g.owner.equals(target),"Nie można usunąć lidera.");
        require(!actor.equals(target),"Użyj przycisku opuszczenia gangu.");
        Member removed=g.members.remove(target);require(removed!=null,"Nie ma takiego członka.");
        record(g,g.members.get(actor).name+" usuwa gracza "+removed.name+" z gangu.");
    }
    public void transfer(UUID actor,UUID target){
        Gang g=of(actor);require(g!=null && g.owner.equals(actor),"Tylko lider może przekazać przywództwo.");
        require(!actor.equals(target) && g.members.containsKey(target),"Wybierz innego członka.");
        g.members.get(actor).role="member";g.members.get(target).role="leader";
        g.owner=target;record(g,g.members.get(actor).name+" przekazuje przywództwo graczowi "+g.members.get(target).name+".");
    }
    public void role(UUID actor,String roleId,String name,Set<Permission> permissions){
        Gang g=own(actor,Permission.MANAGE_ROLES);name=validName(name,2,24);
        require(!"leader".equals(roleId),"Uprawnienia lidera są stałe.");
        require(permissions!=null&&permissions.stream().allMatch(p->p!=null&&p.available()),"To uprawnienie jest jeszcze w przygotowaniu.");
        boolean creating=roleId==null||roleId.isBlank();
        if(creating){require(g.roles.size()<MAX_ROLES,"Maksymalnie 12 rang.");roleId=UUID.randomUUID().toString();}
        else require(g.roles.containsKey(roleId),"Nie ma takiej rangi.");
        g.roles.put(roleId,new Role(roleId,name,permissions));
    }
    public void assign(UUID actor,UUID target,String role){
        Gang g=own(actor,Permission.ASSIGN_ROLES);
        require(g.members.containsKey(target),"Nie ma takiego członka.");
        require(!g.owner.equals(target)&&!"leader".equals(role),"Lidera zmienia się przez przekazanie przywództwa.");
        require(g.roles.containsKey(role),"Nie ma takiej rangi.");
        if(role.equals(g.members.get(target).role))return;
        g.members.get(target).role=role;
        record(g,g.members.get(actor).name+" zmienia rangę gracza "+g.members.get(target).name+" na "+g.roles.get(role).name+".");
    }
    /** Ordering affects presentation, independently of permissions and ownership. */
    public void reorderRoles(UUID actor,List<String> order){
        Gang g=own(actor,Permission.MANAGE_ROLES);
        require(order!=null&&order.size()==g.roles.size()&&new HashSet<>(order).size()==order.size()
            &&g.roles.keySet().containsAll(order),"Kolejność musi zawierać każdą rangę dokładnie raz.");
        if(order.equals(new ArrayList<>(g.roles.keySet())))return;
        Map<String,Role> sorted=new LinkedHashMap<>();for(String id:order)sorted.put(id,g.roles.get(id));g.roles=sorted;
    }
    public void theme(UUID actor,Theme theme){own(actor,Permission.MANAGE_THEME).theme=theme;}
    public void logo(UUID actor,String logo){own(actor,Permission.MANAGE_THEME).logo=logo;}
    public void banner(UUID actor,GangBanner banner){
        Gang g=own(actor,Permission.MANAGE_THEME);
        g.banner=new GangBanner(GangBanner.validateImage(banner.image()),banner.color(),banner.opacity());
    }
    public boolean rememberProfile(UUID player,String name,String textures,String signature){
        Gang g=of(player);if(g==null)return false;Member m=g.members.get(player);
        if(textures==null||textures.length()>8192||signature==null||signature.length()>2048)return false;
        boolean changed=!Objects.equals(m.name,name)||!Objects.equals(m.textures,textures)||!Objects.equals(m.textureSignature,signature);
        m.name=name;m.textures=textures;m.textureSignature=signature;return changed;
    }
    public void renameSeenPlayer(UUID player,String name){
        Gang g=of(player);if(g!=null)g.members.get(player).name=name;
        for(Gang other:gangs.values())if(other.requests.containsKey(player))other.requests.put(player,name);
    }
    private static void record(Gang g,String text){
        g.history.add(new Event(System.currentTimeMillis(),text));
        if(g.history.size()>HISTORY_LIMIT)g.history.subList(0,g.history.size()-HISTORY_LIMIT).clear();
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
        var document=JsonParser.parseString(text);require(document.isJsonObject(),"Nieprawidłowy zapis gangów.");
        boolean legacy=!document.getAsJsonObject().has("schemaVersion");
        GangDirectory directory=JSON.fromJson(document,GangDirectory.class);
        if(directory==null || directory.gangs==null || directory.gangs.size()>MAX_GANGS)throw new IllegalArgumentException("Nieprawidłowy zapis gangów.");
        directory.schemaVersion=2;directory.migrated=legacy;
        Set<UUID> seen=new HashSet<>();
        for(var entry:directory.gangs.entrySet()){
            Gang g=entry.getValue();
            require(g!=null && entry.getKey().equals(g.id) && g.owner!=null,"Uszkodzony identyfikator gangu.");
            validName(g.name,3,32);
            if(g.banner==null)g.banner=GangBanner.DEFAULT;
            if(g.history==null)g.history=new ArrayList<>();
            for(Event event:g.history)require(event!=null&&event.timestamp()>=0&&event.text()!=null&&event.text().length()<=256,"Uszkodzony rejestr gangu.");
            if(g.history.size()>HISTORY_LIMIT)g.history.subList(0,g.history.size()-HISTORY_LIMIT).clear();
            require(g.members!=null && g.members.containsKey(g.owner),"Uszkodzone członkostwo.");
            defaultRoles(g);
            require(g.roles.size()<=MAX_ROLES,"Uszkodzone rangi.");
            for(var role:g.roles.entrySet()){
                Role value=role.getValue();
                require(value!=null&&role.getKey().equals(value.id)&&value.permissions!=null&&!value.permissions.contains(null),"Uszkodzone uprawnienia rangi.");
                validName(value.name,2,24);
                value.permissions=value.permissions.isEmpty()?EnumSet.noneOf(Permission.class):EnumSet.copyOf(value.permissions);
                if(legacy&&value.permissions.contains(Permission.MANAGE_ROLES))value.permissions.add(Permission.ASSIGN_ROLES);
                value.permissions.removeIf(p->!p.available());
            }
            g.roles.get("leader").permissions=Permission.activePermissions();
            for(var member:g.members.entrySet()){
                require(member.getValue()!=null && member.getKey().equals(member.getValue().id) && seen.add(member.getKey()),"Powielone członkostwo.");
                Member m=member.getValue();
                if(m.joinedAt==0)for(int i=g.history.size()-1;i>=0;i--){
                    Event event=g.history.get(i);String eventText=event.text();
                    if(eventText.equals(m.name+" opuszcza gang.")||eventText.endsWith(" usuwa gracza "+m.name+" z gangu."))break;
                    if(event.timestamp()>0&&(eventText.equals(m.name+" tworzy gang.")||eventText.equals(m.name+" dołącza do gangu."))){
                        m.joinedAt=event.timestamp();m.joinedAtFromHistory=true;directory.migrated=true;break;
                    }
                }
                if(m.textures==null)m.textures="";if(m.textureSignature==null)m.textureSignature="";
                require(m.joinedAt>=0&&m.textures.length()<=8192&&m.textureSignature.length()<=2048,"Uszkodzony profil członka.");
                if(member.getValue().role==null)member.getValue().role=member.getKey().equals(g.owner)?"leader":"member";
                require(g.roles.containsKey(member.getValue().role),"Nieznana ranga członka.");
                require(member.getKey().equals(g.owner)=="leader".equals(member.getValue().role),"Uszkodzona ranga lidera.");
            }
            require(g.theme!=null && g.requests!=null && g.logo!=null && g.logo.length()<=GangLogo.MAX_ENCODED_LENGTH,"Uszkodzone ustawienia gangu.");
        }
        return directory;
    }
}
