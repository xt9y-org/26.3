package dev.xt9y.perf;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;

/** Server-authoritative, self-only block grants; never invokes a command or chat. */
public final class BlockGranting {
    private BlockGranting() {}

    public static void register() {
        PayloadTypeRegistry.serverboundPlay().register(GrantBlockPayload.TYPE, GrantBlockPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(GrantBlockPayload.TYPE, (request, context) -> {
            ServerPlayer player = context.player();
            // Integrated world's owner may use the browser even with cheats disabled.
            // Remote players require game-master (operator) permissions.
            boolean permitted = player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)
                    || (context.server().isSingleplayer()
                    && context.server().isSingleplayerOwner(player.nameAndId()));
            if (!permitted || !BuiltInRegistries.BLOCK.containsKey(request.blockId())) return;

            Block block = BuiltInRegistries.BLOCK.getValue(request.blockId());
            Item item = block.asItem();
            // Air, fluids and other blocks without an inventory item cannot be granted.
            if (item == Items.AIR) return;

            ItemStack stack = new ItemStack(item);
            if (request.fullStack()) stack.setCount(stack.getMaxStackSize());
            // No /give, no output to player chat, no broadcast and no ground drop.
            // A full inventory simply cannot accept the remaining items.
            player.getInventory().add(stack);
            player.inventoryMenu.broadcastChanges();
        });
    }
}
