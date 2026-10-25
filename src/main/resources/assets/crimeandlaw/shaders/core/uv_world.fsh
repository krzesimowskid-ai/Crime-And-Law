#version 150
uniform sampler2D Scene;
uniform sampler2D SceneDepth;
uniform sampler2D Volume;
uniform mat4 InverseProjection;
uniform mat4 InverseView;
uniform int LightCount;
uniform int HandPass;
uniform float HandLight;
uniform vec4 Light0,Light1,Light2,Light3,Light4,Light5,Light6,Light7;
in vec2 texCoord;
out vec4 fragColor;
float volume(vec3 point,vec4 light){
    vec3 q=point-light.xyz;
    if(any(lessThan(q,vec3(0.0)))||any(greaterThan(q,vec3(13.0))))return 0.0;
    q=clamp(q,vec3(0.501),vec3(12.499));float yy=q.y-0.5,y0=floor(yy),f=fract(yy);
    vec2 a=vec2((y0*13.0+q.x)/169.0,(light.w*13.0+q.z)/104.0);
    vec2 b=vec2(((min(y0+1.0,12.0))*13.0+q.x)/169.0,a.y);
    return mix(texture(Volume,a).r,texture(Volume,b).r,f);
}
void main(){
    vec4 scene=texture(Scene,texCoord);float depth=texture(SceneDepth,texCoord).r;
    float light=HandLight;
    if(HandPass==0){
        vec4 view=InverseProjection*vec4(texCoord*2.0-1.0,depth*2.0-1.0,1.0);view/=view.w;
        vec3 p=(InverseView*view).xyz;
        vec3 n=normalize(cross(dFdx(p),dFdy(p)));if(dot(n,-p)<0.0)n=-n;
        vec3 air=p+n*0.08;light=0.0;
        if(LightCount>0)light+=volume(air,Light0);if(LightCount>1)light+=volume(air,Light1);
        if(LightCount>2)light+=volume(air,Light2);if(LightCount>3)light+=volume(air,Light3);
        if(LightCount>4)light+=volume(air,Light4);if(LightCount>5)light+=volume(air,Light5);
        if(LightCount>6)light+=volume(air,Light6);if(LightCount>7)light+=volume(air,Light7);
        light=clamp(light*1.8,0.0,1.0);
    }
    light=depth<0.999999?light:0.0;
    float luma=dot(scene.rgb,vec3(0.2126,0.7152,0.0722));
    vec3 reflected=scene.rgb*mix(vec3(1.0),vec3(0.80,0.21,1.36),light*0.9);
    reflected+=vec3(0.25,0.035,0.49)*light*(0.30+luma*0.55);
    fragColor=vec4(clamp(reflected,0.0,1.0),scene.a);
}
