struct PSInput {
    float4 fragColor : COLOR0;
};

struct PSOutput {
    float4 outColor : SV_Target0;
};

PSOutput main(PSInput input) {
    PSOutput output;
    output.outColor = input.fragColor;
    return output;
}
