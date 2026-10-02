package net.mera;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;

public class MeraClient implements ClientModInitializer {
    private int tick;

    @Override
    public void onInitializeClient() {
        Thread t = new Thread(() -> {
            DiscordRPC.connect();
            DiscordRPC.setDetails("Idling");
        }, "MERA-Discord");
        t.setDaemon(true);
        t.start();

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            if (++tick < 40) return; // check every 2 seconds
            tick = 0;
            String state = mc.level == null ? "Idling"
                    : (mc.getCurrentServer() != null ? "Playing multiplayer" : "Playing singleplayer");
            Thread u = new Thread(() -> DiscordRPC.setDetails(state), "MERA-Discord-Update");
            u.setDaemon(true);
            u.start();
        });
    }
}
