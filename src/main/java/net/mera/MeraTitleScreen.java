package net.mera;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.OptionsScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.fabricmc.loader.api.FabricLoader;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class MeraTitleScreen extends Screen {
    private static final ResourceLocation LOGO = ResourceLocation.fromNamespaceAndPath("mera", "textures/gui/logo.png");
    private static final ResourceLocation BG_ID = ResourceLocation.fromNamespaceAndPath("mera", "dynamic_background");
    private static final Path BG_FILE = FabricLoader.getInstance().getConfigDir().resolve("mera/background.png");

    private static DynamicTexture bgTexture;
    private static int bgW, bgH;

    public MeraTitleScreen() {
        super(Component.literal("MΞRA Client"));
    }

    @Override
    protected void init() {
        if (bgTexture == null && Files.exists(BG_FILE)) loadBackground(BG_FILE);

        int cx = this.width / 2;
        int bw = 150, bh = 16;
        int y = this.height / 2 - 10;

        addRenderableWidget(Button.builder(Component.literal("SINGLEPLAYER"),
                b -> minecraft.setScreen(new SelectWorldScreen(this))).bounds(cx - bw / 2, y, bw, bh).build());
        addRenderableWidget(Button.builder(Component.literal("MULTIPLAYER"),
                b -> minecraft.setScreen(new JoinMultiplayerScreen(this))).bounds(cx - bw / 2, y + 20, bw, bh).build());
        addRenderableWidget(Button.builder(Component.literal("MODS"),
                b -> minecraft.setScreen(new MeraModsScreen(this))).bounds(cx - bw / 2, y + 40, bw, bh).build());

        // X = close the game
        addRenderableWidget(Button.builder(Component.literal("X"),
                b -> minecraft.stop()).bounds(this.width - 24, 4, 20, 20).build());

        // Import background
        addRenderableWidget(Button.builder(Component.literal("BG"),
                b -> pickBackground()).bounds(4, 4, 20, 20).build());

        // Options (small, bottom centre)
        addRenderableWidget(Button.builder(Component.literal("OPTIONS"),
                b -> minecraft.setScreen(new OptionsScreen(this, minecraft.options)))
                .bounds(cx - 35, this.height - 24, 70, 16).build());
    }

    private void pickBackground() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            PointerBuffer filters = stack.mallocPointer(3);
            filters.put(stack.UTF8("*.png")).put(stack.UTF8("*.jpg")).put(stack.UTF8("*.jpeg")).flip();
            String chosen = TinyFileDialogs.tinyfd_openFileDialog("Choose background", "", filters, "Images", false);
            if (chosen == null) return;
            Path src = Path.of(chosen);
            Files.createDirectories(BG_FILE.getParent());
            // Convert anything to PNG so NativeImage can read it
            if (chosen.toLowerCase().endsWith(".png")) {
                Files.copy(src, BG_FILE, StandardCopyOption.REPLACE_EXISTING);
            } else {
                java.awt.image.BufferedImage img = javax.imageio.ImageIO.read(src.toFile());
                javax.imageio.ImageIO.write(img, "png", BG_FILE.toFile());
            }
            loadBackground(BG_FILE);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void loadBackground(Path file) {
        try (InputStream in = Files.newInputStream(file)) {
            NativeImage img = NativeImage.read(in);
            Minecraft mc = Minecraft.getInstance();
            if (bgTexture != null) mc.getTextureManager().release(BG_ID);
            bgW = img.getWidth();
            bgH = img.getHeight();
            bgTexture = new DynamicTexture(() -> "mera_bg", img);
            mc.getTextureManager().register(BG_ID, bgTexture);
        } catch (Exception e) {
            e.printStackTrace();
            bgTexture = null;
        }
    }

    // No vanilla panorama / blur
    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float delta) { }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        if (bgTexture != null) {
            g.blit(RenderPipelines.GUI_TEXTURED, BG_ID, 0, 0, 0f, 0f, this.width, this.height, bgW, bgH, bgW, bgH);
            g.fill(0, 0, this.width, this.height, 0x88000000); // darken so buttons stay readable
        } else {
            g.fill(0, 0, this.width, this.height, 0xFF0E1116);
        }

        int cx = this.width / 2;
        int y = this.height / 2 - 10;

        // "M" logo + name
        g.blit(RenderPipelines.GUI_TEXTURED, LOGO, cx - 20, y - 70, 0f, 0f, 40, 40, 50, 50, 50, 50);
        g.drawCenteredString(this.font, "MΞRA CLIENT", cx, y - 25, 0xFFFFFFFF);

        // bottom-left version text
        g.drawString(this.font, "MΞRA Client 1.21.11", 4, this.height - 12, 0xFFAAAAAA, false);

        super.render(g, mouseX, mouseY, delta);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }
}
