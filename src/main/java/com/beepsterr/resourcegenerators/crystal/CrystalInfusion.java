package com.beepsterr.resourcegenerators.crystal;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * In-progress infusion state carried by a blank crystal while it is being filled in a Crystal
 * Infuser. Stored in the {@code crystal_infusion} component and shown via the item's durability
 * bar. When {@link #amount} reaches {@link #required}, the Infuser sets the crystal's resource to
 * {@link #target} and removes this component.
 *
 * @param target   the resource the crystal is becoming (locked in by the first material fed)
 * @param amount   units of material fed so far
 * @param required total units needed to complete
 */
public record CrystalInfusion(CrystalResource target, int amount, int required) {

    public static final Codec<CrystalInfusion> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            CrystalResource.CODEC.fieldOf("target").forGetter(CrystalInfusion::target),
            Codec.INT.fieldOf("amount").forGetter(CrystalInfusion::amount),
            Codec.INT.fieldOf("required").forGetter(CrystalInfusion::required)
    ).apply(inst, CrystalInfusion::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, CrystalInfusion> STREAM_CODEC = StreamCodec.composite(
            CrystalResource.STREAM_CODEC, CrystalInfusion::target,
            ByteBufCodecs.VAR_INT, CrystalInfusion::amount,
            ByteBufCodecs.VAR_INT, CrystalInfusion::required,
            CrystalInfusion::new
    );

    public CrystalInfusion started(CrystalResource target, int required) {
        return new CrystalInfusion(target, 0, required);
    }

    public CrystalInfusion plus(int units) {
        return new CrystalInfusion(target, Math.min(required, amount + units), required);
    }

    public boolean isComplete() {
        return amount >= required;
    }

    public float fraction() {
        return required <= 0 ? 1.0f : (float) amount / required;
    }
}
