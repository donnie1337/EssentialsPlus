package com.donnie1337.essentialsplus.teleport;

import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientCustomClickAction;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/** Recebe os cliques do TPA sem transformar o clique em um comando executável pelo cliente. */
public final class CustomTpaClickListener extends PacketListenerAbstract {
    private static final String PREFIX = "essentialsplus:tpa/";

    private final TeleportService service;

    public CustomTpaClickListener(TeleportService service) {
        this.service = service;
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() != PacketType.Play.Client.CUSTOM_CLICK_ACTION) return;

        final Player player = event.getPlayer();
        if (player == null) return;

        final WrapperPlayClientCustomClickAction packet = new WrapperPlayClientCustomClickAction(event);
        final String identifier = packet.getId().toString();
        if (!identifier.startsWith(PREFIX)) return;

        event.setCancelled(true);
        Bukkit.getScheduler().runTask(service.plugin(), () -> service.handleCustomButton(player, identifier));
    }
}
