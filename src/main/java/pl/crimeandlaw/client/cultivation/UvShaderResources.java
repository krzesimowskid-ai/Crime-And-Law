package pl.crimeandlaw.client.cultivation;

import com.google.gson.JsonParser;
import java.io.*;
import java.nio.charset.StandardCharsets;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.*;

/** Wraps the current resource pack's vanilla shaders; does not replace textures or custom shaders. */
public final class UvShaderResources {
    private UvShaderResources(){}
    public static ResourceProvider wrap(ResourceProvider original){
        return location->original.getResource(location).map(resource->{
            if(!location.getNamespace().equals("minecraft")||!location.getPath().startsWith("shaders/core/"))return resource;
            return new Resource(resource.source(),()->{
                String source;try(var stream=resource.open()){source=new String(stream.readAllBytes(),StandardCharsets.UTF_8);}
                if(location.getPath().endsWith(".vsh"))source=UvShaderSource.vertex(source);
                else if(location.getPath().endsWith(".json")){
                    String vertex=JsonParser.parseString(source).getAsJsonObject().get("vertex").getAsString();var id=ResourceLocation.parse(vertex);
                    if(id.getNamespace().equals("minecraft"))try(var stream=original.open(ResourceLocation.fromNamespaceAndPath("minecraft","shaders/core/"+id.getPath()+".vsh"))){
                        if(UvShaderSource.supports(new String(stream.readAllBytes(),StandardCharsets.UTF_8)))source=UvShaderSource.program(source);
                    }
                }
                return new ByteArrayInputStream(source.getBytes(StandardCharsets.UTF_8));
            },resource::metadata);
        });
    }
}
