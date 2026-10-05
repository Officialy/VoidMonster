#version 330
#extension GL_ARB_separate_shader_objects : require
#include <minecraft:globals.glsl>
#include <dragonapi:reika_color.glsl>

uniform sampler2D InSampler;
layout(std140) uniform MonsterFocus { vec4 Focus; };
layout(location = 0) in vec2 texCoord;
layout(location = 0) out vec4 fragColor;

void main() {
    vec2 monsterXY = Focus.xy;
    float distance = max(Focus.z, 0.00001);
    float intensity = Focus.w;
    vec2 diff = texCoord - monsterXY;
    vec2 distanceUV = diff * vec2(1.0, ScreenSize.y / ScreenSize.x);
    float distv = dot(distanceUV, distanceUV);
    float scale = 1.0 + intensity * min(1.0, distance / 5.0)
        * max(0.0, min(1.0, max(0.0, 18.0 / distance)) * min(4.0, 0.009 / max(0.00001, distv)));
    vec4 color = texture(InSampler, monsterXY + diff * scale);
    float cf = intensity * max(0.0, 1.0 - 3.5 * distv);
    fragColor = vec4(mix(color.rgb, vec3(getVisualBrightness(color.rgb)), cf), color.a);
}
