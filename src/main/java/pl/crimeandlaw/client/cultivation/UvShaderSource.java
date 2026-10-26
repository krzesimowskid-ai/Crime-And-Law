package pl.crimeandlaw.client.cultivation;

import com.google.gson.*;
import pl.crimeandlaw.cultivation.UvVolume;

/** Adds UV to the material's lightmap before its texture is shaded, never to the final image. */
public final class UvShaderSource {
    private static final String SAMPLE="minecraft_sample_lightmap(Sampler2, UV2)",FETCH="texelFetch(Sampler2, UV2 / 16, 0)";
    private UvShaderSource(){}
    public static boolean supports(String vertex){return vertex.contains("uniform mat4 ModelViewMat;")&&vertex.contains("void main()")&&(vertex.contains(SAMPLE)||vertex.contains(FETCH));}
    public static String vertex(String source){
        if(!supports(source)||source.contains("uniform int CalUvCount;"))return source;
        String position=source.contains("uniform vec3 ChunkOffset;")?"Position + ChunkOffset":"Position";
        String normal=source.contains("in vec3 Normal;")?"Normal":"vec3(0.0)";
        String args=", "+position+", "+normal+")";
        source=source.replace(SAMPLE,"cal_uv_light("+SAMPLE+args).replace(FETCH,"cal_uv_light("+FETCH+args);
        int main=source.indexOf("void main()");return source.substring(0,main)+GLSL+source.substring(main);
    }
    public static String program(String json){
        JsonObject root=JsonParser.parseString(json).getAsJsonObject();JsonArray samplers=root.getAsJsonArray("samplers"),uniforms=root.getAsJsonArray("uniforms");
        if(samplers==null){samplers=new JsonArray();root.add("samplers",samplers);}if(uniforms==null){uniforms=new JsonArray();root.add("uniforms",uniforms);}
        for(var u:uniforms)if(u.getAsJsonObject().get("name").getAsString().equals("CalUvCount"))return json;
        JsonObject sampler=new JsonObject();sampler.addProperty("name","CalUvVolume");samplers.add(sampler);
        uniform(uniforms,"CalUvCount","int",0);uniform(uniforms,"CalUvHand","int",0);uniform(uniforms,"CalUvHandLight","float",0);uniform(uniforms,"CalUvGamma","float",0);
        uniform(uniforms,"CalUvInverseView","matrix4x4",1,0,0,0,0,1,0,0,0,0,1,0,0,0,0,1);
        for(int i=0;i<UvVolume.MAX_LIGHTS;i++)uniform(uniforms,"CalUvLight"+i,"float",0,0,0,i);
        return root.toString();
    }
    private static void uniform(JsonArray target,String name,String type,float...values){JsonObject u=new JsonObject();u.addProperty("name",name);u.addProperty("type",type);u.addProperty("count",values.length);JsonArray v=new JsonArray();for(float n:values)v.add(n);u.add("values",v);target.add(u);}
    private static final String GLSL="""
        uniform sampler2D CalUvVolume;
        uniform int CalUvCount;
        uniform int CalUvHand;
        uniform float CalUvHandLight;
        uniform float CalUvGamma;
        uniform mat4 CalUvInverseView;
        uniform vec4 CalUvLight0,CalUvLight1,CalUvLight2,CalUvLight3,CalUvLight4,CalUvLight5,CalUvLight6,CalUvLight7;
        float cal_uv_field(vec3 point,vec4 lamp){
            vec3 q=point-lamp.xyz;
            if(any(lessThan(q,vec3(0.0)))||any(greaterThan(q,vec3(%1$s.0))))return 0.0;
            q=clamp(q,vec3(0.501),vec3(%2$s.499));float yy=q.y-0.5,y0=floor(yy),f=fract(yy);
            vec2 a=vec2((y0*%1$s.0+q.x)/%3$s.0,(lamp.w*%1$s.0+q.z)/%4$s.0);
            vec2 b=vec2((min(y0+1.0,%2$s.0)*%1$s.0+q.x)/%3$s.0,a.y);
            return mix(texture(CalUvVolume,a).r,texture(CalUvVolume,b).r,f);
        }
        vec4 cal_uv_light(vec4 ordinary,vec3 modelPosition,vec3 modelNormal){
            if(CalUvCount==0)return ordinary;
            float light=CalUvHandLight;
            if(CalUvHand==0){
                mat4 world=CalUvInverseView*ModelViewMat;
                vec3 p=(world*vec4(modelPosition,1.0)).xyz;
                vec3 n=mat3(world)*modelNormal;float lengthN=length(n);
                if(lengthN>0.001)p+=n/lengthN*0.45;
                light=0.0;
                if(CalUvCount>0)light=max(light,cal_uv_field(p,CalUvLight0));
                if(CalUvCount>1)light=max(light,cal_uv_field(p,CalUvLight1));
                if(CalUvCount>2)light=max(light,cal_uv_field(p,CalUvLight2));
                if(CalUvCount>3)light=max(light,cal_uv_field(p,CalUvLight3));
                if(CalUvCount>4)light=max(light,cal_uv_field(p,CalUvLight4));
                if(CalUvCount>5)light=max(light,cal_uv_field(p,CalUvLight5));
                if(CalUvCount>6)light=max(light,cal_uv_field(p,CalUvLight6));
                if(CalUvCount>7)light=max(light,cal_uv_field(p,CalUvLight7));
            }
            vec3 uv=vec3(0.58,0.035,1.0)*light;
            uv=mix(uv,vec3(1.0)-pow(vec3(1.0)-uv,vec3(4.0)),clamp(CalUvGamma,0.0,1.0));
            return vec4(max(ordinary.rgb,uv),ordinary.a);
        }
        """.formatted(UvVolume.SIZE,UvVolume.SIZE-1,UvVolume.SIZE*UvVolume.SIZE,UvVolume.SIZE*UvVolume.MAX_LIGHTS);
}
