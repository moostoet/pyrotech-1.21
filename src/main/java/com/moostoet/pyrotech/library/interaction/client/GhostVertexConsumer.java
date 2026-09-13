package com.moostoet.pyrotech.library.interaction.client;

import com.mojang.blaze3d.vertex.VertexConsumer;

/** Passes an item model's vertices through with a fixed alpha, for the ghost preview. */
record GhostVertexConsumer(VertexConsumer target, int alpha) implements VertexConsumer {

    @Override
    public VertexConsumer addVertex(float x, float y, float z) {
        this.target.addVertex(x, y, z);
        return this;
    }

    @Override
    public VertexConsumer setColor(int red, int green, int blue, int alpha) {
        this.target.setColor(red, green, blue, this.alpha);
        return this;
    }

    @Override
    public VertexConsumer setUv(float u, float v) {
        this.target.setUv(u, v);
        return this;
    }

    @Override
    public VertexConsumer setUv1(int u, int v) {
        this.target.setUv1(u, v);
        return this;
    }

    @Override
    public VertexConsumer setUv2(int u, int v) {
        this.target.setUv2(u, v);
        return this;
    }

    @Override
    public VertexConsumer setNormal(float normalX, float normalY, float normalZ) {
        this.target.setNormal(normalX, normalY, normalZ);
        return this;
    }
}
