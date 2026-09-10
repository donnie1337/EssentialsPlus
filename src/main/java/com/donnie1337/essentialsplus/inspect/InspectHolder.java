package com.donnie1337.essentialsplus.inspect;

import java.util.UUID;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public final class InspectHolder implements InventoryHolder {
    public enum Type {
        PLAYER,
        ENDER_CHEST,
        BAU
    }

    private final UUID target;
    private final Type type;
    private Inventory inventory;

    public InspectHolder(UUID target, Type type) {
        this.target = target;
        this.type = type;
    }

    public UUID target() {
        return target;
    }

    public Type type() {
        return type;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
