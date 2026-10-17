package dev.xt9y.perf.client;

import dev.xt9y.perf.GrantBlockPayload;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Searchable, scrollable block-item grid, rendered by Minecraft's own item renderer. */
public final class BlockBrowserScreen extends Screen {
    private static final int CELL = 23;
    private static final int MAX_COLUMNS = 15;
    private static final int MAX_ROWS = 8;

    private record Entry(Identifier id, ItemStack icon, String searchText) {}

    private final List<Entry> entries = new ArrayList<>();
    private final List<Entry> matches = new ArrayList<>();
    private EditBox search;
    private int columns;
    private int rows;
    private int left;
    private int top;
    private int scroll;

    public BlockBrowserScreen() {
        super(Component.literal("Blocks"));
    }

    @Override
    protected void init() {
        columns = Math.max(1, Math.min(MAX_COLUMNS, (width - 42) / CELL));
        rows = Math.max(1, Math.min(MAX_ROWS, (height - 110) / CELL));
        left = (width - columns * CELL) / 2;
        top = (height - rows * CELL) / 2 + 10;
        search = new EditBox(font, left, top - 28, columns * CELL, 20,
                Component.literal("Search blocks"));
        search.setHint(Component.literal("Search name or ID..."));
        search.setMaxLength(100);
        search.setResponder(this::filter);
        addRenderableWidget(search);
        setInitialFocus(search);

        entries.clear();
        for (Block block : BuiltInRegistries.BLOCK) {
            Item item = block.asItem();
            if (item == Items.AIR) continue;
            ItemStack icon = new ItemStack(item);
            Identifier id = BuiltInRegistries.BLOCK.getKey(block);
            String name = icon.getHoverName().getString();
            entries.add(new Entry(id, icon,
                    (name + " " + id).toLowerCase(Locale.ROOT)));
        }
        entries.sort(Comparator.comparing(entry -> entry.id().toString()));
        filter(search.getValue());
    }

    private void filter(String query) {
        String term = query.strip().toLowerCase(Locale.ROOT);
        matches.clear();
        for (Entry entry : entries) {
            if (term.isEmpty() || entry.searchText().contains(term)) matches.add(entry);
        }
        scroll = 0;
    }

    private int maxScroll() {
        return Math.max(0, (matches.size() + columns - 1) / columns - rows);
    }

    private boolean inGrid(double x, double y) {
        return x >= left && x < left + columns * CELL
                && y >= top && y < top + rows * CELL;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial) {
        graphics.fill(0, 0, width, height, 0xA0000000);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial) {
        super.extractRenderState(graphics, mouseX, mouseY, partial);

        int right = left + columns * CELL;
        int bottom = top + rows * CELL;
        graphics.fill(left - 7, top - 9, right + 7, bottom + 9, 0xED171A20);
        graphics.text(font, "Block browser", left, top - 42, 0xFFFFFFFF);
        graphics.text(font, matches.size() + " blocks", right - font.width(matches.size() + " blocks"),
                top - 42, 0xFFAAB4C0);

        int begin = scroll * columns;
        for (int n = 0; n < columns * rows && begin + n < matches.size(); n++) {
            int x = left + (n % columns) * CELL;
            int y = top + (n / columns) * CELL;
            boolean hovered = mouseX >= x && mouseX < x + CELL
                    && mouseY >= y && mouseY < y + CELL;
            graphics.fill(x, y, x + CELL - 1, y + CELL - 1,
                    hovered ? 0xFF586476 : 0xFF2B3039);
            graphics.item(matches.get(begin + n).icon(), x + 3, y + 3);
            if (hovered) {
                graphics.setTooltipForNextFrame(font, matches.get(begin + n).icon(), mouseX, mouseY);
            }
        }
        // Scroll indicator on the right, without rendering off-screen items.
        if (maxScroll() > 0) {
            int track = rows * CELL;
            int thumb = Math.max(12, track * rows / (rows + maxScroll()));
            int y = top + (track - thumb) * scroll / maxScroll();
            graphics.fill(right + 3, top, right + 5, bottom, 0xFF343A45);
            graphics.fill(right + 3, y, right + 5, y + thumb, 0xFFB3BDCF);
        }
        graphics.text(font, "LMB: full stack  |  RMB: one  |  Scroll: browse  |  Esc: close",
                left, bottom + 13, 0xFFCED4DF);
        graphics.text(font, "Local world owner / server operator only",
                left, bottom + 25, 0xFF9DA8B7);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        if (inGrid(mouseX, mouseY) && vertical != 0) {
            int amount = Math.max(1, (int) Math.ceil(Math.abs(vertical) * 3));
            scroll = Math.clamp(scroll - (vertical > 0 ? amount : -amount), 0, maxScroll());
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        int button = event.button();
        if (inGrid(event.x(), event.y()) && (button == InputConstants.MOUSE_BUTTON_LEFT || button == InputConstants.MOUSE_BUTTON_RIGHT)) {
            int col = ((int) event.x() - left) / CELL;
            int row = ((int) event.y() - top) / CELL;
            int index = (scroll + row) * columns + col;
            if (index < matches.size() && ClientPlayNetworking.canSend(GrantBlockPayload.TYPE)) {
                // The requesting player's identity comes from the connection, not this packet.
                ClientPlayNetworking.send(new GrantBlockPayload(matches.get(index).id(), button == InputConstants.MOUSE_BUTTON_LEFT));
            }
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
