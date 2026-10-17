package dev.xt9y.perf.client;

import cn.enaium.fabric.imgui.TextureBindings;
import dev.xt9y.perf.GrantBlockPayload;
import imgui.ImGui;
import imgui.ImGuiIO;
import imgui.ImGuiListClipper;
import imgui.flag.ImGuiCond;
import imgui.type.ImBoolean;
import imgui.type.ImString;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Separate ImGui block browser. Block-atlas texture thumbnails work on OpenGL and Vulkan. */
public final class CheatMenu {
    private static final ImBoolean OPEN = new ImBoolean(false);
    private static final ImString SEARCH = new ImString(128);
    private static final List<Entry> ALL = new ArrayList<>();
    private static final List<Entry> FILTERED = new ArrayList<>();
    private static final float ICON_SIZE = 24.0f;
    private static final float ROW_HEIGHT = 29.0f;

    private record Entry(Identifier id, Block block, String name, String searchable) {}

    private CheatMenu() {}

    public static boolean visible() {
        return OPEN.get();
    }

    public static void toggle() {
        if (OPEN.get()) {
            close();
        } else {
            if (Minecraft.getInstance().player == null) return;
            reload();
            OPEN.set(true);
            Minecraft.getInstance().mouseHandler.releaseMouse();
        }
    }

    public static void close() {
        if (!OPEN.get()) return;
        OPEN.set(false);
        ALL.clear();
        FILTERED.clear();
        SEARCH.set("");
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.gui.screen() == null) mc.mouseHandler.grabMouse();
    }

    private static void reload() {
        ALL.clear();
        for (Block block : BuiltInRegistries.BLOCK) {
            Item item = block.asItem();
            if (item == Items.AIR) continue;
            Identifier id = BuiltInRegistries.BLOCK.getKey(block);
            String name = new ItemStack(item).getHoverName().getString();
            ALL.add(new Entry(id, block, name, (name + " " + id).toLowerCase(Locale.ROOT)));
        }
        ALL.sort(Comparator.comparing(e -> e.id().toString()));
        filter();
    }

    private static void filter() {
        FILTERED.clear();
        String term = SEARCH.get().strip().toLowerCase(Locale.ROOT);
        for (Entry entry : ALL) {
            if (term.isEmpty() || entry.searchable().contains(term)) FILTERED.add(entry);
        }
    }

    private static void give(Entry entry, boolean fullStack) {
        if (ClientPlayNetworking.canSend(GrantBlockPayload.TYPE)) {
            // Server independently checks permission and grants only to the packet sender.
            ClientPlayNetworking.send(new GrantBlockPayload(entry.id(), fullStack));
        }
    }

    public static void draw(ImGuiIO io) {
        if (!OPEN.get()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            close();
            return;
        }

        ImGui.setNextWindowSize(520, 500, ImGuiCond.FirstUseEver);
        boolean showContents = ImGui.begin("26.3 Block Cheats", OPEN);
        if (showContents) {
            ImGui.textDisabled("LMB: full stack   |   RMB: one   |   Ctrl + Shift + Alt + Right Shift: toggle");
            if (ImGui.inputTextWithHint("##xt9y-block-search", "Search blocks by name or ID...", SEARCH)) {
                filter();
            }
            ImGui.sameLine();
            ImGui.text(FILTERED.size() + " blocks");
            ImGui.separator();

            if (ImGui.beginChild("##xt9y-blocks", 0, 0, false)) {
                ImGuiListClipper clipper = new ImGuiListClipper();
                try {
                    clipper.begin(FILTERED.size(), ROW_HEIGHT);
                    while (clipper.step()) {
                        for (int i = clipper.getDisplayStart(); i < clipper.getDisplayEnd(); i++) {
                            Entry entry = FILTERED.get(i);
                            drawBlock(mc, entry, i);
                        }
                    }
                } finally {
                    clipper.destroy();
                }
            }
            ImGui.endChild();
        }
        ImGui.end();
        if (!OPEN.get()) closeAfterWindow();
    }

    private static void closeAfterWindow() {
        // ImGui already changed OPEN via the window close control.
        ALL.clear();
        FILTERED.clear();
        SEARCH.set("");
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.gui.screen() == null) mc.mouseHandler.grabMouse();
    }

    private static void drawBlock(Minecraft mc, Entry entry, int index) {
        TextureAtlasSprite sprite = mc.getModelManager().getBlockStateModelSet()
                .getParticleMaterial(entry.block().defaultBlockState()).sprite();
        // Resolve the current atlas each frame, so resource-pack reloads cannot leave
        // a stale texture ID. The binding handles the Vulkan texture-view backend too.
        long texture = TextureBindings.INSTANCE.textureId(
                mc.getTextureManager().getTexture(sprite.atlasLocation()).getTextureView());

        ImGui.image(texture, ICON_SIZE, ICON_SIZE, sprite.getU0(), sprite.getV0(),
                sprite.getU1(), sprite.getV1());
        if (ImGui.isItemClicked(0)) give(entry, true);
        else if (ImGui.isItemClicked(1)) give(entry, false);

        ImGui.sameLine();
        ImGui.selectable(entry.name() + "  [" + entry.id() + "]##" + index, false, 0, 0, ICON_SIZE);
        if (ImGui.isItemClicked(0)) give(entry, true);
        else if (ImGui.isItemClicked(1)) give(entry, false);
    }
}
