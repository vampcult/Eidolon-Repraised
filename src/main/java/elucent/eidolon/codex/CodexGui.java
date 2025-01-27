package elucent.eidolon.codex;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import elucent.eidolon.Eidolon;
import elucent.eidolon.api.spells.Rune;
import elucent.eidolon.api.spells.Sign;
import elucent.eidolon.client.ClientRegistry;
import elucent.eidolon.event.ClientEvents;
import elucent.eidolon.network.AttemptCastPacket;
import elucent.eidolon.network.Networking;
import elucent.eidolon.util.RenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class CodexGui extends Screen {
    public static final CodexGui DUMMY = new CodexGui();
    public static final ResourceLocation CODEX_BACKGROUND = new ResourceLocation(Eidolon.MODID, "textures/gui/codex_bg.png");
    static final int xSize = 312;
    static final int ySize = 208;
    final List<Sign> chant = new ArrayList<>();
    Rune hoveredRune = null;

    Chapter currentChapter;
    Chapter lastChapter;
    int currentPage = 0;

    static CodexGui INSTANCE = null;

    public static CodexGui getInstance() {
        for (Category cat : CodexChapters.categories) cat.reset();
        return INSTANCE != null ? INSTANCE : (INSTANCE = new CodexGui());
    }

    protected CodexGui() {
        super(Component.translatable("gui.eidolon.codex.title"));
        lastChapter = currentChapter = CodexChapters.NATURE_INDEX;
    }

    public static void blit(GuiGraphics guiGraphics, int i, int i1, int i2, int i3, int i4, int i5, int i6, int i7) {
        guiGraphics.blit(CODEX_BACKGROUND, i, i1, i2, i3, i4, i5, i6, i7);
    }

    protected void resetPages() {
        Page left = currentChapter.get(currentPage), right = currentChapter.get(currentPage + 1);
        if (left != null) left.reset();
        if (right != null) right.reset();
    }

    protected void changeChapter(Chapter next) {
        lastChapter = currentChapter;
        currentChapter = next;
        currentPage = 0;
    }

    public void addToChant(Sign rune) {
        if (this.chant.size() < 18) this.chant.add(rune);
    }

    protected void renderChant(@NotNull GuiGraphics mStack, int x, int y, int mouseX, int mouseY, float pticks) {
        int chantWidth = 32 + 24 * chant.size();
        int baseX = x + xSize / 2 - chantWidth / 2, baseY = y + 180;

        RenderSystem.enableBlend();

        int bgx = baseX;
        blit(mStack, bgx, baseY, 256, 208, 16, 32, 512, 512);
        bgx += 16;
        for (int i = 0; i < chant.size(); i++) {
            blit(mStack, bgx, baseY, 272, 208, 24, 32, 512, 512);
            blit(mStack, bgx, baseY, 312, 208, 24, 24, 512, 512);
            bgx += 24;
        }
        blit(mStack, bgx, baseY, 296, 208, 16, 32, 512, 512);
        bgx += 24;
        boolean chantHover = mouseX >= bgx && mouseY >= baseY - 4 && mouseX <= bgx + 32 && mouseY <= baseY + 28;
        blit(mStack, bgx, baseY - 4, 336, chantHover ? 240 : 208, 32, 32, 512, 512);
        bgx += 36;
        boolean cancelHover = mouseX >= bgx && mouseY >= baseY - 4 && mouseX <= bgx + 32 && mouseY <= baseY + 28;
        blit(mStack, bgx, baseY - 4, 368, cancelHover ? 240 : 208, 32, 32, 512, 512);
        if (chantHover) mStack.renderTooltip(font, Component.translatable("eidolon.codex.chant_hover"), mouseX, mouseY);
        if (cancelHover)
            mStack.renderTooltip(font, Component.translatable("eidolon.codex.cancel_hover"), mouseX, mouseY);

        RenderSystem.enableBlend();
        RenderSystem.setShader(ClientRegistry::getGlowingSpriteShader);
        RenderSystem.setShaderTexture(0, InventoryMenu.BLOCK_ATLAS);
        bgx = baseX + 16;

        MultiBufferSource.BufferSource buffersource = Minecraft.getInstance().renderBuffers().bufferSource();

        for (Sign sign : chant) {
            RenderUtil.litQuad(mStack.pose(), buffersource, bgx + 4, baseY + 4, 16, 16,
                    sign.getRed(), sign.getGreen(), sign.getBlue(), Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(sign.getSprite()));
            buffersource.endBatch();
            bgx += 24;
        }
        bgx = baseX + 16;
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        for (int i = 0; i < chant.size(); i++) {
            float flicker = 0.75f + 0.25f * (float) Math.sin(Math.toRadians(12 * ClientEvents.getClientTicks() - 360.0f * i / chant.size()));
            Sign sign = chant.get(i);
            RenderUtil.litQuad(mStack.pose(), buffersource, bgx + 4, baseY + 4, 16, 16,
                    sign.getRed() * flicker, sign.getGreen() * flicker, sign.getBlue() * flicker, Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(sign.getSprite()));
            buffersource.endBatch();
            bgx += 24;
        }


        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        RenderSystem.setShaderTexture(0, BACKGROUND_LOCATION);
    }

    boolean hasTooltip = false;
    Matrix4f tooltipMatrix = null;
    Component tooltipText = null;
    int tooltipX = 0, tooltipY = 0;

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        hasTooltip = false;
        this.minecraft = Minecraft.getInstance();
        renderBackground(guiGraphics);
        Minecraft mc = minecraft;
        RenderSystem.setShaderTexture(0, CODEX_BACKGROUND);

        this.width = mc.getWindow().getGuiScaledWidth();
        this.height = mc.getWindow().getGuiScaledHeight();
        int guiLeft = (width - xSize) / 2, guiTop = (height - ySize) / 2;
        guiGraphics.blit(CODEX_BACKGROUND, guiLeft, guiTop, 0, 256, xSize, ySize, 512, 512);

        for (int i = 0; i < CodexChapters.categories.size(); i++) {
            int y = guiTop + 28 + (i % 8) * 20;
            CodexChapters.categories.get(i).draw(this, guiGraphics, guiLeft + (i >= 8 ? 304 : 8), y, i >= 8, mouseX, mouseY);
        }

        guiGraphics.blit(CODEX_BACKGROUND, guiLeft, guiTop, 0, 0, xSize, ySize, 512, 512);
        Page left = currentChapter.get(currentPage), right = currentChapter.get(currentPage + 1);
        if (left != null) left.fullRender(this, guiGraphics, guiLeft + 14, guiTop + 24, mouseX, mouseY);
        if (right != null) right.fullRender(this, guiGraphics, guiLeft + 170, guiTop + 24, mouseX, mouseY);

        if (currentPage > 0) { // left arrow
            int x = 10, y = 169;
            int v = 208;
            if (mouseX >= guiLeft + x && mouseY >= guiTop + y && mouseX <= guiLeft + x + 32 && mouseY <= guiTop + y + 16)
                v += 18;
            guiGraphics.blit(CODEX_BACKGROUND, guiLeft + x, guiTop + y, 128, v, 32, 18, 512, 512);
        }
        if (currentPage + 2 < currentChapter.size()) { // right arrow
            int x = 270, y = 169;
            int v = 208;
            if (mouseX >= guiLeft + x && mouseY >= guiTop + y && mouseX <= guiLeft + x + 32 && mouseY <= guiTop + y + 16)
                v += 18;
            guiGraphics.blit(CODEX_BACKGROUND, guiLeft + x, guiTop + y, 160, v, 32, 18, 512, 512);
        }

        if (!chant.isEmpty()) renderChant(guiGraphics, guiLeft, guiTop, mouseX, mouseY, partialTicks);


        for (int i = 0; i < CodexChapters.categories.size(); i++) {
            int y = guiTop + 28 + (i % 8) * 20;
            CodexChapters.categories.get(i).drawTooltip(guiGraphics, guiLeft + (i >= 8 ? 304 : 8), y, i >= 8, mouseX, mouseY);
        }

        if (hasTooltip) {
            var pose = guiGraphics.pose();
            pose.pushPose();
            pose.setIdentity();
            pose.mulPoseMatrix(tooltipMatrix);
            guiGraphics.renderTooltip(font, tooltipText, tooltipX, tooltipY);
            pose.popPose();
        }
    }

    protected boolean interactChant(int x, int y, int mouseX, int mouseY) {
        /*
        int chantWidth = 32 + 12 * chant.size();
        int baseX = x + xSize / 2 - chantWidth / 2, baseY = y + 180;
        int bgx = baseX + chantWidth + 8;
        boolean chantHover = mouseX >= bgx && mouseY >= baseY - 4 && mouseX <= bgx + 32 && mouseY <= baseY + 28;
        bgx += 36;
        boolean cancelHover = mouseX >= bgx && mouseY >= baseY - 4 && mouseX <= bgx + 32 && mouseY <= baseY + 28;
        */
        int chantWidth = 32 + 24 * chant.size();
        int baseX = x + xSize / 2 - chantWidth / 2, baseY = y + 180;
        int bgx = baseX + chantWidth + 8;
        boolean chantHover = mouseX >= bgx && mouseY >= baseY - 4 && mouseX <= bgx + 32 && mouseY <= baseY + 28;
        bgx += 36;
        boolean cancelHover = mouseX >= bgx && mouseY >= baseY - 4 && mouseX <= bgx + 32 && mouseY <= baseY + 28;
        if (chantHover) {
            Player player = Minecraft.getInstance().player;
            Level world = Minecraft.getInstance().level;
            if (player == null || world == null) return false;
            Networking.sendToServer(new AttemptCastPacket(player, chant));
            chant.clear();
            player.playNotifySound(SoundEvents.UI_BUTTON_CLICK.get(), SoundSource.NEUTRAL, 1.0f, 1.0f);
            this.onClose();
            return true;
        }
        if (cancelHover) {
            chant.clear();
            Minecraft.getInstance().player.playNotifySound(SoundEvents.UI_BUTTON_CLICK.get(), SoundSource.NEUTRAL, 1.0f, 1.0f);
            return true;
        }
        return false;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            Minecraft mc = Minecraft.getInstance();
            this.width = mc.getWindow().getGuiScaledWidth();
            this.height = mc.getWindow().getGuiScaledHeight();
            int guiLeft = (width - xSize) / 2, guiTop = (height - ySize) / 2;

            if (currentPage > 0) { // left arrow
                int x = guiLeft + 10, y = guiTop + 169;
                if (mouseX >= x && mouseY >= y && mouseX <= x + 32 && mouseY <= y + 16) {
                    currentPage -= 2;
                    Minecraft.getInstance().player.playNotifySound(SoundEvents.BOOK_PAGE_TURN, SoundSource.NEUTRAL, 1.0f, 1.0f);
                    resetPages();
                    return true;
                }
            }
            if (currentPage + 2 < currentChapter.size()) { // right arrow
                int x = guiLeft + 270, y = guiTop + 169;
                if (mouseX >= x && mouseY >= y && mouseX <= x + 32 && mouseY <= y + 16) {
                    currentPage += 2;
                    Minecraft.getInstance().player.playNotifySound(SoundEvents.BOOK_PAGE_TURN, SoundSource.NEUTRAL, 1.0f, 1.0f);
                    resetPages();
                    return true;
                }
            }

            for (int i = 0; i < CodexChapters.categories.size(); i++) {
                int y = guiTop + 28 + (i % 8) * 20;
                if (CodexChapters.categories.get(i).click(this, guiLeft + (i >= 8 ? 304 : 8), y, i >= 8, (int) mouseX, (int) mouseY))
                    return true;
            }

            Page left = currentChapter.get(currentPage), right = currentChapter.get(currentPage + 1);
            if (left != null) if (left.click(this, guiLeft + 14, guiTop + 24, (int) mouseX, (int) mouseY)) return true;
            if (right != null)
                if (right.click(this, guiLeft + 170, guiTop + 24, (int) mouseX, (int) mouseY)) return true;

            return !chant.isEmpty() && interactChant(guiLeft, guiTop, (int) mouseX, (int) mouseY);
        } else if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            if (!chant.isEmpty() && currentChapter.get(currentPage) instanceof SignIndexPage) {
                chant.remove(chant.size() - 1);
                Minecraft.getInstance().player.playNotifySound(SoundEvents.UI_BUTTON_CLICK.get(), SoundSource.NEUTRAL, 1.0f, 1.0f);
                return true;
            }
            //otherwise, if it's not an index page, go back to the index page
            if (!(currentChapter.get(currentPage) instanceof SignIndexPage)) {
                currentChapter = lastChapter;
                currentPage = 0;
                Minecraft.getInstance().player.playNotifySound(SoundEvents.BOOK_PAGE_TURN, SoundSource.NEUTRAL, 1.0f, 1.0f);
                resetPages();
                return true;
            }

        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double pMouseX, double pMouseY, double pDelta) {
        if (pDelta < 0) {
            if (currentPage + 2 < currentChapter.size()) {
                currentPage += 1;
                Minecraft.getInstance().player.playNotifySound(SoundEvents.BOOK_PAGE_TURN, SoundSource.NEUTRAL, 1.0f, 1.0f);
                resetPages();
                return true;
            }
        } else if (pDelta > 0) {
            if (currentPage > 0) {
                currentPage -= 1;
                Minecraft.getInstance().player.playNotifySound(SoundEvents.BOOK_PAGE_TURN, SoundSource.NEUTRAL, 1.0f, 1.0f);
                resetPages();
                return true;
            }
        }

        return super.mouseScrolled(pMouseX, pMouseY, pDelta);
    }
}
