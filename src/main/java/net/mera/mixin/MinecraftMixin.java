package net.mera.mixin;

import net.mera.MeraTitleScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Minecraft.class)
public class MinecraftMixin {
    @ModifyVariable(method = "setScreen", at = @At("HEAD"), argsOnly = true)
    private Screen mera$replaceTitle(Screen screen) {
        if (screen != null && screen.getClass() == TitleScreen.class) {
            return new MeraTitleScreen();
        }
        return screen;
    }
}
