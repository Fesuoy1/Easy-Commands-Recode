package fesuoy.mods.easy_commands_recode.mixin;

import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.feature.trunkplacers.TrunkPlacer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static fesuoy.mods.easy_commands_recode.EasyCommandsRecode.getTreeHeight;

@Mixin(TrunkPlacer.class)
public class TrunkPlacerMixin {

    @Final
    @Shadow
    protected int baseHeight;

    @Final
    @Shadow
    protected int heightRandA;

    @Final
    @Shadow
    protected int heightRandB;

    @Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/util/RandomSource;nextInt(I)I"), method = "getTreeHeight", cancellable = true)
    public void onGetHeight(RandomSource random, CallbackInfoReturnable<Integer> cir) {
        // Original code: this.baseHeight + randomSource.nextInt(this.heightRandA + 1) + randomSource.nextInt(this.heightRandB + 1);
        // we want to modify the "+ 1" that come after the first and second random height, and we want to change that to the amount we want.
        // That way we can set the height to the amount of blocks we want. then the tree will grow bigger.
        cir.setReturnValue(this.baseHeight + random.nextInt(this.heightRandA + getTreeHeight() + random.nextInt(this.heightRandB + getTreeHeight())));
    }
}