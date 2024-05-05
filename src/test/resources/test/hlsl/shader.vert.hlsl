struct VSInput {
    float4 inPosition : POSITION;
    float4 inColor : COLOR;
};

struct PSInput {
    float4 Position : SV_POSITION;
    float4 Color : COLOR0;
};

PSInput main(VSInput input) {
    PSInput output;
    output.Position = input.inPosition;
    output.Color = input.inColor;
    return output;
}
