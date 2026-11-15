package com.a23bc.ammod;

/*
 * AGSL shaders from Kyant0/AndroidLiquidGlass (branch kmp, backdrop v2.0.1)
 *   backdrop/src/commonMain/kotlin/com/kyant/backdrop/internal/Shaders.kt
 *
 * Copyright 2025 Kyant
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * 说明：Xposed 模块没法把目标 App 的 Compose 树接进库的 Backdrop，
 * 因此这里以源码方式引用库同款 shader，用 Android 原生 RenderEffect 在 View 层复现。
 */
public final class BackdropShaders {

    private static final String ROUNDED_RECT_SDF = """
            float radiusAt(float2 centeredCoord, float4 radii) {
                if (centeredCoord.x >= 0.0) {
                    if (centeredCoord.y <= 0.0) return radii.y;
                    else return radii.z;
                } else {
                    if (centeredCoord.y <= 0.0) return radii.x;
                    else return radii.w;
                }
            }

            float sdRoundedRect(float2 coord, float2 halfSize, float radius) {
                float2 cornerCoord = abs(coord) - (halfSize - float2(radius));
                float outside = length(max(cornerCoord, 0.0)) - radius;
                float inside = min(max(cornerCoord.x, cornerCoord.y), 0.0);
                return outside + inside;
            }

            float2 gradSdRoundedRect(float2 coord, float2 halfSize, float radius) {
                float2 cornerCoord = abs(coord) - (halfSize - float2(radius));
                if (cornerCoord.x >= 0.0 || cornerCoord.y >= 0.0) {
                    return sign(coord) * normalize(max(cornerCoord, 0.0));
                } else {
                    float gradX = step(cornerCoord.y, cornerCoord.x);
                    return sign(coord) * float2(gradX, 1.0 - gradX);
                }
            }
            """;

    /** RoundedRectRefractionShaderString — LiquidButton 的 lens 折射。 */
    public static final String REFRACTION = """
            uniform shader content;

            uniform float2 size;
            uniform float2 offset;
            uniform float4 cornerRadii;
            uniform float refractionHeight;
            uniform float refractionAmount;
            uniform float depthEffect;

            """ + ROUNDED_RECT_SDF + """
            float circleMap(float x) {
                return 1.0 - sqrt(1.0 - x * x);
            }

            half4 main(float2 coord) {
                float2 halfSize = size * 0.5;
                float2 centeredCoord = (coord + offset) - halfSize;
                float radius = radiusAt(centeredCoord, cornerRadii);

                float sd = sdRoundedRect(centeredCoord, halfSize, radius);
                if (-sd >= refractionHeight) {
                    return content.eval(coord);
                }
                sd = min(sd, 0.0);

                float d = circleMap(1.0 - -sd / refractionHeight) * refractionAmount;
                float gradRadius = min(radius * 1.5, min(halfSize.x, halfSize.y));
                float2 grad = normalize(gradSdRoundedRect(centeredCoord, halfSize, gradRadius) + depthEffect * normalize(centeredCoord));

                float2 refractedCoord = coord + d * grad;
                return content.eval(refractedCoord);
            }
            """;

    /** DefaultHighlightShaderString — Highlight.Default 的方向性边缘高光。 */
    public static final String HIGHLIGHT = """
            uniform float2 size;
            uniform float4 cornerRadii;
            layout(color) uniform half4 color;
            uniform float angle;
            uniform float falloff;

            """ + ROUNDED_RECT_SDF + """
            half4 main(float2 coord) {
                float2 halfSize = size * 0.5;
                float2 centeredCoord = coord - halfSize;
                float radius = radiusAt(centeredCoord, cornerRadii);

                float gradRadius = min(radius * 1.5, min(halfSize.x, halfSize.y));
                float2 grad = gradSdRoundedRect(centeredCoord, halfSize, gradRadius);
                float2 normal = float2(cos(angle), sin(angle));
                float d = dot(grad, normal);
                float intensity = pow(abs(d), falloff);
                return color * intensity;
            }
            """;

    private BackdropShaders() {
    }
}
