package pl.crimeandlaw.territory;

import java.util.*;

/** Immutable description of the generated landscape, with no gameplay multipliers. */
public record TerrainProfile(TerrainKey key,int schema,int sampleCount,int minHeight,int maxHeight,
        Map<Surface,Integer> surfaces,int woodedSamples,List<String> biomes,List<String> villagePieces,
        Set<Trait> traits,Source source,long analysedAt) {
    public enum Source { GENERATED_SNAPSHOT, GENERATOR_RECONSTRUCTION }
    public enum Surface { SOIL, ROCK, SAND, WATER, SNOW, OTHER }
    public enum Trait {
        CULTIVABLE("Przewaga płaskiego podłoża ziemnego"),
        WOODED("Teren leśny"),
        ROCKY("Odsłonięta skała"),
        SANDY("Podłoże piaszczyste"),
        WATER("Woda w obrębie chunka"),
        HILLY("Duże różnice wysokości"),
        SNOWY("Podłoże śnieżne"),
        VILLAGE("Obszar wygenerowanej wioski"),
        FARM("Wygenerowana farma"),
        WORKSHOP("Wygenerowany warsztat"),
        ANIMAL_PEN("Wygenerowana zagroda"),
        FISHERY("Wygenerowany dom rybaka"),
        LIBRARY("Wygenerowana biblioteka"),
        TANNERY("Wygenerowana garbarnia");
        private final String label;
        Trait(String label){this.label=label;}
        public String label(){return label;}
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
        // Coarse descriptive recognition only. These thresholds do not qualify a bonus.
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
}
