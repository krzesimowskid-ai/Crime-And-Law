package pl.crimeandlaw.territory;

import java.util.*;

/** Immutable description of the generated landscape, with no gameplay multipliers. */
public record TerrainProfile(TerrainKey key,int schema,int sampleCount,int minHeight,int maxHeight,
        Map<Surface,Integer> surfaces,int woodedSamples,List<String> biomes,List<String> villagePieces,
        Set<Trait> traits,Source source,long analysedAt) {
    public enum Source { GENERATED_SNAPSHOT, GENERATOR_RECONSTRUCTION }
    public enum Surface { SOIL, ROCK, SAND, WATER, SNOW, OTHER }
    public enum Trait {
        CULTIVABLE("Teren sprzyjający uprawom","Potencjał upraw"),
        WOODED("Teren leśny","Potencjał gospodarki leśnej"),
        ROCKY("Odsłonięta skała","Potencjał wydobycia"),
        SANDY("Podłoże piaszczyste","Potencjał surowców piaszczystych"),
        WATER("Woda w obrębie chunka","Dostęp do wody"),
        HILLY("Duże różnice wysokości","Ukształtowanie terenu"),
        SNOWY("Podłoże śnieżne","Warunki chłodnego terenu"),
        VILLAGE("Obszar wygenerowanej wioski","Obiekty wioski"),
        FARM("Wygenerowana farma","Potencjał produkcji roślinnej"),
        WORKSHOP("Wygenerowany warsztat","Potencjał wytwarzania wyposażenia"),
        ANIMAL_PEN("Wygenerowana zagroda","Potencjał hodowli"),
        FISHERY("Wygenerowany dom rybaka","Potencjał rybołówstwa"),
        LIBRARY("Wygenerowana biblioteka","Potencjał wiedzy i badań"),
        TANNERY("Wygenerowana garbarnia","Potencjał obróbki materiałów");
        private final String label,potential;
        Trait(String label,String potential){this.label=label;this.potential=potential;}
        public String label(){return label;}
        public String potential(){return potential;}
    }
    public record Sample(int height,Surface surface,boolean wooded,String biome){}
    public TerrainProfile {
        Objects.requireNonNull(key);Objects.requireNonNull(source);
        if(schema!=1||sampleCount<1||sampleCount>256||minHeight>maxHeight||minHeight< -4096||maxHeight>4096
            ||woodedSamples<0||woodedSamples>sampleCount||analysedAt<0)throw new IllegalArgumentException("Nieprawidłowe pomiary terenu.");
        EnumMap<Surface,Integer> counts=new EnumMap<>(Surface.class);
        if(surfaces==null)throw new IllegalArgumentException("Brak podłoża.");
        for(var entry:surfaces.entrySet()){
            if(entry.getKey()==null||entry.getValue()==null||entry.getValue()<0||entry.getValue()>sampleCount)throw new IllegalArgumentException("Nieprawidłowe podłoże.");
            counts.put(entry.getKey(),entry.getValue());
        }
        if(counts.values().stream().mapToInt(Integer::intValue).sum()!=sampleCount)throw new IllegalArgumentException("Niepełne pomiary podłoża.");
        surfaces=Collections.unmodifiableMap(counts);
        biomes=identifiers(biomes,256);villagePieces=identifiers(villagePieces,128);
        if(traits==null||traits.stream().anyMatch(Objects::isNull))throw new IllegalArgumentException("Brak cech terenu.");
        traits=Collections.unmodifiableSet(traits.isEmpty()?EnumSet.noneOf(Trait.class):EnumSet.copyOf(traits));
    }
    private static List<String> identifiers(List<String> values,int limit){
        if(values==null||values.size()>limit||values.stream().anyMatch(s->s==null||s.length()>256||!s.matches("[a-z0-9_.-]+:[a-z0-9/._-]+")))throw new IllegalArgumentException("Nieprawidłowe dane generowania.");
        return List.copyOf(new TreeSet<>(values));
    }
    public static TerrainProfile analyse(TerrainKey key,List<Sample> samples,boolean village,Collection<String> templates,long when){
        if(samples==null||samples.isEmpty())throw new IllegalArgumentException("Brak pomiarów.");
        EnumMap<Surface,Integer> counts=new EnumMap<>(Surface.class);Set<String> biomes=new TreeSet<>();
        int min=Integer.MAX_VALUE,max=Integer.MIN_VALUE,wooded=0;
        for(Sample s:samples){
            Objects.requireNonNull(s);Objects.requireNonNull(s.surface());
            min=Math.min(min,s.height());max=Math.max(max,s.height());counts.merge(s.surface(),1,Integer::sum);
            if(s.wooded())wooded++;biomes.add(s.biome());
        }
        Set<Trait> traits=EnumSet.noneOf(Trait.class);int n=samples.size();
        // Recognition rules describe landscape suitability, not bonus percentages.
        if(counts.getOrDefault(Surface.SOIL,0)*2>=n&&max-min<=12)traits.add(Trait.CULTIVABLE);
        if(wooded*4>=n)traits.add(Trait.WOODED);
        if(counts.getOrDefault(Surface.ROCK,0)*4>=n)traits.add(Trait.ROCKY);
        if(counts.getOrDefault(Surface.SAND,0)*4>=n)traits.add(Trait.SANDY);
        if(counts.getOrDefault(Surface.WATER,0)>0)traits.add(Trait.WATER);
        if(counts.getOrDefault(Surface.SNOW,0)*4>=n)traits.add(Trait.SNOWY);
        if(max-min>=24)traits.add(Trait.HILLY);
        if(village)traits.add(Trait.VILLAGE);
        for(String template:templates)traits.addAll(TerrainTemplates.traits(template));
        return new TerrainProfile(key,1,n,min,max,counts,wooded,new ArrayList<>(biomes),new ArrayList<>(templates),traits,Source.GENERATED_SNAPSHOT,when);
    }
    public TerrainProfile withBuildings(boolean village,Collection<String> templates){
        Set<Trait> combined=traits.isEmpty()?EnumSet.noneOf(Trait.class):EnumSet.copyOf(traits);
        if(village)combined.add(Trait.VILLAGE);for(String template:templates)combined.addAll(TerrainTemplates.traits(template));
        return new TerrainProfile(key,schema,sampleCount,minHeight,maxHeight,surfaces,woodedSamples,biomes,new ArrayList<>(templates),combined,source,analysedAt);
    }
    public TerrainProfile reconstructed(){return new TerrainProfile(key,schema,sampleCount,minHeight,maxHeight,surfaces,woodedSamples,biomes,villagePieces,traits,Source.GENERATOR_RECONSTRUCTION,analysedAt);}
    public int mapColor(){
        Surface dominant=surfaces.entrySet().stream().max(Map.Entry.comparingByValue()).orElseThrow().getKey();
        return switch(dominant){case WATER->0x315B86;case SAND->0xA69864;case ROCK->0x737579;case SNOW->0xB7C3CC;case SOIL->traits.contains(Trait.WOODED)?0x315C45:0x66804D;default->0x675A63;};
    }
}
