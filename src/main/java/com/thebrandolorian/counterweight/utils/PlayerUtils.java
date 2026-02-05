package com.thebrandolorian.counterweight.utils;

import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.inventory.transaction.ItemStackSlotTransaction;

public class PlayerUtils {
	public static boolean tryUpdateHeldItem(InteractionContext context, ItemContainer container, ItemStack newItem) {
		ItemStackSlotTransaction transaction = container.setItemStackForSlot(context.getHeldItemSlot(), newItem);
		if (transaction.succeeded()) { context.setHeldItem(newItem); return true; }
		
		DebugUtils.logWarn("Failed to update item in slot %d for player %s. Item: %s",
						   context.getHeldItemSlot(),
						   context.getTargetEntity(), //TODO: check this identifies the player properly
						   newItem.getItemId());
		return false;
	}
	
}
