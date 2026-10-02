package net.mera;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/** Simple list of installed mods. */
public class MeraModsScreen extends Screen {
    private final Screen parent;
    private final List<String> lines = new ArrayList<>();

    public MeraModsScreen(Screen parent) {
        super(Component.literal("MODS"));
        this.parent = parent;
        for (ModContainer m : FabricLoader.getInstance().getAllMods()) {
            String id = m.getMetadata().getId();
            if (id.startsWith("fabric-") || id.equals("java") || id.equals("minecraft") || id.equals("fabricloader")) continue;
            lines.add(m.getMetadata().getName() + "  v" + m.getMetadata().getVersion().getFriendlyString());
        }
    }

    @Override
    protected void init() {
        addRenderableWidget(Button.builder(Component.literal("BACK"), b -> minecraft.setScreen(parent))
                .bounds(this.width / 2 - 50, this.height - 28, 100, 18).build());
    }

    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float d) { }

    @Override
    public void render(GuiGraphics g, int mx, int my, float d) {
        g.fill(0, 0, this.width, this.height, 0xEE0E1116);
        g.drawCenteredString(this.font, "MODS", this.width / 2, 12, 0xFFFFFFFF);
        int y = 32;
        for (String l : lines) {
            if (y > this.height - 40) break;
            g.drawCenteredString(this.font, l, this.width / 2, y, 0xFFDDDDDD);
            y += 12;
        }
        super.render(g, mx, my, d);
    }
}
