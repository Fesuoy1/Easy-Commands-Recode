package fesuoy.mods.easy_commands_recode.mixin;

import fesuoy.mods.easy_commands_recode.EasyCommandsRecode;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Projectile.class)
public abstract class ProjectileMixin extends Entity {
    public ProjectileMixin(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }

    @Inject(method = "onHitBlock", at = @At("HEAD"))
    private void onHitBlock(BlockHitResult hitResult, CallbackInfo ci) {
        Projectile projectile = (Projectile) (Object) this;
        if (!projectile.level().isClientSide() && projectile.level() instanceof ServerLevel serverLevel) {
            if (serverLevel.getGameRules().get(EasyCommandsRecode.EXPLOSIVE_PROJECTILES_ENABLED)) {
                double power = serverLevel.getGameRules().get(EasyCommandsRecode.EXPLOSION_POWER);
                EasyCommandsRecode.explode(projectile, (float)power);
            }
        }
    }

    @Inject(method = "onHitEntity", at = @At("HEAD"))
    private void onHitEntity(EntityHitResult hitResult, CallbackInfo ci) {
        Projectile projectile = (Projectile) (Object) this;
        if (!projectile.level().isClientSide() && projectile.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            if (serverLevel.getGameRules().get(EasyCommandsRecode.EXPLOSIVE_PROJECTILES_ENABLED)) {
                double power = serverLevel.getGameRules().get(EasyCommandsRecode.EXPLOSION_POWER);
                EasyCommandsRecode.explode(projectile, (float)power);
            }
        }
    }
}
