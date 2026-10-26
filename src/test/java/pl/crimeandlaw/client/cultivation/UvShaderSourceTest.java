package pl.crimeandlaw.client.cultivation;
import com.google.gson.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class UvShaderSourceTest {
    private static final String TERRAIN="#version 150\nuniform mat4 ModelViewMat;\nuniform vec3 ChunkOffset;\nin vec3 Normal;\nvoid main() { vertexColor = Color * minecraft_sample_lightmap(Sampler2, UV2); }";
    @Test void coloredLightChangesMaterialLightingBeforeItMultipliesTheTexture(){String source=UvShaderSource.vertex(TERRAIN);assertTrue(source.contains("Color * cal_uv_light(minecraft_sample_lightmap(Sampler2, UV2), Position + ChunkOffset, Normal)"));assertFalse(source.contains("fragColor"));assertFalse(source.contains("scene.rgb"));assertEquals(source,UvShaderSource.vertex(source));}
    @Test void entityShadersUseTheirModelPositionWithoutAChunkOffset(){String source=UvShaderSource.vertex(TERRAIN.replace("uniform vec3 ChunkOffset;","").replace("minecraft_sample_lightmap(Sampler2, UV2)","texelFetch(Sampler2, UV2 / 16, 0)"));assertTrue(source.contains("cal_uv_light(texelFetch(Sampler2, UV2 / 16, 0), Position, Normal)"));}
    @Test void particlesWithoutNormalsUseTheirPosition(){String source=UvShaderSource.vertex(TERRAIN.replace("in vec3 Normal;", ""));assertTrue(source.contains(", vec3(0.0))"));}
    @Test void unlitUiAndEmissiveShadersRemainUntouched(){for(String source:new String[]{"#version 150\nvoid main() { gl_Position = vec4(1.0); }",TERRAIN.replace("minecraft_sample_lightmap(Sampler2, UV2)","vec4(1.0)")})assertEquals(source,UvShaderSource.vertex(source));}
    @Test void programMetadataRetainsExistingSamplersAndUniforms(){String original="{\"vertex\":\"rendertype_solid\",\"fragment\":\"rendertype_solid\",\"samplers\":[{\"name\":\"Sampler0\"},{\"name\":\"Sampler2\"}],\"uniforms\":[{\"name\":\"FogStart\",\"type\":\"float\",\"count\":1,\"values\":[0]}]}";String patched=UvShaderSource.program(original);var json=JsonParser.parseString(patched).getAsJsonObject();assertEquals("Sampler0",json.getAsJsonArray("samplers").get(0).getAsJsonObject().get("name").getAsString());assertEquals("FogStart",json.getAsJsonArray("uniforms").get(0).getAsJsonObject().get("name").getAsString());assertEquals(patched,UvShaderSource.program(patched));}
}
