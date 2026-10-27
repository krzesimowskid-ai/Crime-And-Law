package pl.crimeandlaw.client.cultivation;

import com.google.gson.*;


/** Adds UV to the material's lightmap before its texture is shaded, never to the final image. */
public final class UvShaderSource {
    private static final String SAMPLE="minecraft_sample_lightmap(Sampler2, UV2)",FETCH="texelFetch(Sampler2, UV2 / 16, 0)";
    private UvShaderSource(){}
    public static boolean supports(String vertex){return vertex.contains("uniform mat4 ModelViewMat;")&&vertex.contains("void main()")&&(vertex.contains(SAMPLE)||vertex.contains(FETCH));}
    public static String vertex(String source){
        if(!supports(source)||source.contains("uniform sampler2D CalUvIndex;"))return source;
        String position=source.contains("uniform vec3 ChunkOffset;")?"Position + ChunkOffset":"Position";
        String normal=source.contains("in vec3 Normal;")?"Normal":"vec3(0.0)";
        String offset=source.contains("uniform vec3 ChunkOffset;")?"0.45":"0.012";
        String args=", "+position+", "+normal+", "+offset+")";
        source=source.replace(SAMPLE,"cal_uv_light("+SAMPLE+args).replace(FETCH,"cal_uv_light("+FETCH+args);
        int main=source.indexOf("void main()");return source.substring(0,main)+GLSL+source.substring(main);
    }
    public static String program(String json){
        JsonObject root=JsonParser.parseString(json).getAsJsonObject();JsonArray samplers=root.getAsJsonArray("samplers"),uniforms=root.getAsJsonArray("uniforms");
        if(samplers==null){samplers=new JsonArray();root.add("samplers",samplers);}if(uniforms==null){uniforms=new JsonArray();root.add("uniforms",uniforms);}
        for(var u:uniforms)if(u.getAsJsonObject().get("name").getAsString().equals("CalUvCount"))return json;
        for(String name:new String[]{"CalUvVolume","CalUvIndex"}){JsonObject sampler=new JsonObject();sampler.addProperty("name",name);samplers.add(sampler);}
        uniform(uniforms,"CalUvCount","int",0);uniform(uniforms,"CalUvHand","int",0);uniform(uniforms,"CalUvHandLight","float",0);uniform(uniforms,"CalUvGamma","float",0);
        uniform(uniforms,"CalUvInverseView","matrix4x4",1,0,0,0,0,1,0,0,0,0,1,0,0,0,0,1);
        uniform(uniforms,"CalUvOrigin","float",0,0,0);uniform(uniforms,"CalUvExtent","float",0,0,0);
        for(String name:new String[]{"CalUvColumns","CalUvIndexWidth","CalUvCapacity","CalUvProbes"})uniform(uniforms,name,"int",1);
        return root.toString();
    }
    private static void uniform(JsonArray target,String name,String type,float...values){JsonObject u=new JsonObject();u.addProperty("name",name);u.addProperty("type",type);u.addProperty("count",values.length);JsonArray v=new JsonArray();for(float n:values)v.add(n);u.add("values",v);target.add(u);}
    private static final String GLSL="""
        uniform sampler2D CalUvVolume;
        uniform sampler2D CalUvIndex;
        uniform int CalUvCount,CalUvHand,CalUvColumns,CalUvIndexWidth,CalUvCapacity,CalUvProbes;
        uniform float CalUvHandLight,CalUvGamma;
        uniform mat4 CalUvInverseView;
        uniform vec3 CalUvOrigin,CalUvExtent;
        ivec4 cal_uv_index(int i){return ivec4(round(texelFetch(CalUvIndex,ivec2(i%CalUvIndexWidth,i/CalUvIndexWidth),0)*255.0));}
        int cal_uv_slot(ivec3 key){
            if(any(lessThan(key,ivec3(0)))||any(greaterThan(vec3(key),CalUvExtent)))return -1;
            uint hash=uint(key.x)*73856093u^uint(key.y)*19349663u^uint(key.z)*83492791u;
            int at=int(hash&uint(CalUvCapacity-1));
            for(int p=0;p<CalUvProbes;p++){
                ivec4 a=cal_uv_index(at*3),b=cal_uv_index(at*3+1),c=cal_uv_index(at*3+2);
                int slot=c.g+c.b*256+c.a*65536;if(slot==0)return -1;
                ivec3 stored=ivec3(a.r+a.g*256+a.b*65536,a.a+b.r*256+b.g*65536,b.b+b.a*256+c.r*65536);
                if(all(equal(key,stored)))return slot-1;
                at=(at+1)&(CalUvCapacity-1);
            }return -1;
        }
        float cal_uv_voxel(ivec3 p,int slot,float height){
            ivec2 tile=ivec2((slot%CalUvColumns)*324,(slot/CalUvColumns)*18);
            vec4 raw=texelFetch(CalUvVolume,tile+ivec2(p.y*18+p.x,p.z),0);
            ivec3 ceiling=ivec3(round(raw.gba*255.0));
            if(height>=float(ceiling.r+ceiling.g*256+ceiling.b*65536)+0.545)return 0.0;
            return raw.r;
        }
        float cal_uv_field(vec3 point){
            vec3 q=point-CalUvOrigin;ivec3 key=ivec3(floor(q/16.0));int slot=cal_uv_slot(key);if(slot<0)return 0.0;
            vec3 local=q-vec3(key)*16.0-0.5;ivec3 base=ivec3(floor(local))+1;vec3 f=fract(local);float value=0.0;
            for(int y=0;y<2;y++)for(int z=0;z<2;z++)for(int x=0;x<2;x++){
                vec3 weight=mix(vec3(1.0)-f,f,vec3(x,y,z));
                value+=cal_uv_voxel(base+ivec3(x,y,z),slot,q.y)*weight.x*weight.y*weight.z;
            }return value;
        }
        vec4 cal_uv_light(vec4 ordinary,vec3 modelPosition,vec3 modelNormal,float normalOffset){
            if(CalUvCount==0)return ordinary;
            float light=CalUvHandLight;
            if(CalUvHand==0){
                mat4 world=CalUvInverseView*ModelViewMat;
                vec3 p=(world*vec4(modelPosition,1.0)).xyz;
                vec3 n=mat3(world)*modelNormal;float lengthN=length(n);
                if(lengthN>0.001)p+=n/lengthN*normalOffset;
                light=cal_uv_field(p);
            }
            vec3 uv=vec3(0.50,0.015,1.0)*light;
            uv=mix(uv,vec3(1.0)-pow(vec3(1.0)-uv,vec3(4.0)),clamp(CalUvGamma,0.0,1.0));
            return vec4(max(ordinary.rgb,uv),ordinary.a);
        }
        """;
}
