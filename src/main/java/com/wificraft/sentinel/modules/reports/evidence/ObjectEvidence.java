package com.wificraft.sentinel.modules.reports.evidence;

import com.wificraft.sentinel.modules.reports.Evidence;
import org.bukkit.inventory.ItemStack;
import java.util.UUID;

public class ObjectEvidence extends Evidence {
    private final ItemStack itemStack;
    private final int amount;
    private final String displayName;

    public ObjectEvidence(UUID reporterId, ItemStack itemStack, String notes) {
        super(reporterId, notes, Evidence.EvidenceType.OBJECT);
        this.itemStack = itemStack != null ? itemStack.clone() : null;
        this.amount = itemStack != null ? itemStack.getAmount() : 0;
        this.displayName = itemStack != null && itemStack.hasItemMeta() && 
                          itemStack.getItemMeta().hasDisplayName() ?
                          itemStack.getItemMeta().getDisplayName() : 
                          itemStack != null ? itemStack.getType().toString() : "Unknown Item";
    }

    public ItemStack getItemStack() {
        return itemStack != null ? itemStack.clone() : null;
    }

    public int getAmount() {
        return amount;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getMaterialName() {
        return itemStack != null ? itemStack.getType().name() : "AIR";
    }
}
