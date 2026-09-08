package com.donnie1337.essentialsplus.home;

import org.bukkit.Location;

public record Home(String name, Location location) {
    public Home {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Nome da home invalido.");
        if (location == null || location.getWorld() == null) throw new IllegalArgumentException("Localizacao da home invalida.");
        location = location.clone();
    }
}
