package com.jellied.gamerules;

import com.fox2code.foxloader.loader.ClientMod;
import com.fox2code.foxloader.network.NetworkPlayer;
import com.fox2code.foxloader.registry.CommandCompat;

import com.jellied.gamerules.chatcommands.GameruleChatCommandClient;

import com.jellied.gamerules.chatcommands.GameruleHelpChatCommandClient;
import net.minecraft.src.client.packets.NetworkManager;
import net.minecraft.src.game.level.World;
import net.minecraft.src.game.nbt.NBTTagCompound;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class GamerulesClient extends GamerulesMod implements ClientMod {
    // Field
    public final static Map<String, String> GAMERULE_DESCRIPTIONS = new HashMap<>();
    public final static Map<String, String> GAMERULE_CASE_INSENSITIVE_MAP = new HashMap<>();
    public final static Map<String, Integer> GAMERULE_DEFAULTS = new HashMap<>();
    public final static Map<String, String> GAMERULE_SYNTAX = new HashMap<>();
    public static String[] GAMERULE_IDS = new String[32];

    public static NBTTagCompound worldGamerules;



    // Initialization
    public void onInit() {
        initializeGamerules();
        FastChat.init();

        // Chat commands
        CommandCompat.registerCommand(new GameruleChatCommandClient());
        CommandCompat.registerClientCommand(new GameruleHelpChatCommandClient());
    }

    public void onTick() {
        // im gonna kill myself
        // the reason this shit was flickering was because
        // i was calling this twice
        // once here, and once in CommandHelperGuiMixin
        // FastChat.drawAutocompleteSuggestions();

        FastChat.handleKeybinds();
    }

    public void initializeGamerules() {
        for(EnumGameruleDataClient gamerule : EnumGameruleDataClient.values()) {
            String gameruleName = gamerule.getName();

            GAMERULE_DEFAULTS.put(gameruleName, gamerule.getDefaultValue());
            GAMERULE_CASE_INSENSITIVE_MAP.put(gameruleName.toLowerCase(), gameruleName);
            GAMERULE_DESCRIPTIONS.put(gameruleName, gamerule.getDescription());
            GAMERULE_SYNTAX.put(gameruleName, gamerule.getSyntaxHelp());
            GAMERULE_IDS[gamerule.getId()] = gameruleName;
        }
    }



    // get & set
    public static Integer getGamerule(String gameruleName) {
        gameruleName = GAMERULE_CASE_INSENSITIVE_MAP.get((gameruleName.toLowerCase()));

        if (worldGamerules == null | gameruleName == null) {
            return GAMERULE_DEFAULTS.get(gameruleName);
        }

        if (!worldGamerules.hasKey(gameruleName)) {
            return GAMERULE_DEFAULTS.get(gameruleName);
        }

        return worldGamerules.getInteger(gameruleName);
    }

    public static void setGamerule(String gameruleName, Integer gameruleValue) {
        if (worldGamerules == null) {
            return;
        }

        worldGamerules.setInteger(gameruleName, gameruleValue);
    }

    public static String autocompleteGamerule(String textInput) {
        for (int i = 0; i < GAMERULE_IDS.length - 1; i++) {
            String gamerule = GAMERULE_IDS[i];
            if (gamerule != null && gamerule.toLowerCase().startsWith(textInput.toLowerCase())) {
                return gamerule;
            }
            else if (gamerule == null) {
                break;
            }
        }

        return textInput;
    }

    public static String[] getGamerulesThatBeginWith(String with) {
        String[] gamerules = new String[GAMERULE_IDS.length];
        int totalGamerules = 0;

        for(int i = 0; i < GAMERULE_IDS.length - 1; i++) {
            String gamerule = GAMERULE_IDS[i];
            if (gamerule != null && gamerule.toLowerCase().startsWith(with.toLowerCase())) {
                gamerules[totalGamerules] = gamerule;
                totalGamerules++;
            }
            else if (gamerule == null) {
                break;
            }
        }

        return gamerules;
    }



    // For singleplayer
    public static void onWorldChanged(World world) {
        if (world.multiplayerWorld) {
            return;
        }

        worldGamerules = ((WorldInfoAccessorClient) world.worldInfo).getGamerules();

        // Set defaults
        for (Map.Entry<String, Integer> set : GAMERULE_DEFAULTS.entrySet()) {
            String name = set.getKey();
            Integer defaultValue = set.getValue();

            if (!worldGamerules.hasKey(name)) {
                worldGamerules.setInteger(name, defaultValue);
            }
        }
    }



    // For multiplayer
    public static void onGamerulesPacketRecieved(byte[] packet) {
        NBTTagCompound newTag = new NBTTagCompound();

        // Parse packet
        // Example packet:
        // 0 0 1 1 0 2 3
        // We ignore the first byte as it's just the packet identifier
        // Everything else can be grouped into pairs
        // 0 1   1 0   2 3
        // First integer in each pair is the gamerule's numerical id
        // Second integer is the gamerule's value
        for (int i = 1; i < packet.length - 1; i += 2) {
            String gameruleName = GAMERULE_IDS[packet[i]];
            Integer gameruleValue = (int) packet[i + 1];

            newTag.setInteger(gameruleName, gameruleValue);
        }

        worldGamerules = newTag;
    }

    public void onReceiveServerPacket(NetworkPlayer plr, byte[] packet) {
        byte packetId = packet[0];
        if (packetId == 0) {
            onGamerulesPacketRecieved(packet);
        }
    }
}
