package me.seasnail.meteorplus.mixins;

import me.seasnail.meteorplus.modules.Moses;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity {
    public LivingEntityMixin(EntityType<?> type, World world) {
        super(type, world);
    }

    @Redirect(method = "travel", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;isTouchingWater()Z"))
    private boolean travelIsTouchingWaterProxy(LivingEntity self) {
        if (self.isTouchingWater() && Modules.get().isActive(Moses.class)) return false;
        return self.isTouchingWater();
    }

    @Redirect(method = "tickMovement", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;isInLava()Z"))
    private boolean tickMovementIsInLavaProxy(LivingEntity self) {
        if (self.isInLava() && Modules.get().isActive(Moses.class)) {
            return !Modules.get().get(Moses.class).doLava();
        }
        return self.isInLava();
    }

    @Redirect(method = "tickMovement", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;isTouchingWater()Z"))
    private boolean tickIsTouchingWaterProxy(LivingEntity self) {
        if (self.isTouchingWater() && Modules.get().isActive(Moses.class)) return false;
        return self.isTouchingWater();
    }

    @Redirect(method = "travel", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;isInLava()Z"))
    private boolean travelIsInLavaProxy(LivingEntity self) {
        if (self.isInLava() && Modules.get().isActive(Moses.class)) {
            return !Modules.get().get(Moses.class).doLava();
        }
        return self.isInLava();
    }
}
