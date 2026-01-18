package io.github.flaethegreat.teleportplayers;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class TeleportPlayersCommand extends AbstractPlayerCommand {
    public TeleportPlayersCommand() {
        super("teleportplayers", "Opens a page to teleport players");
        this.addAliases("tpp");
        this.setPermissionGroup(GameMode.Creative);
    }

    @Override
    protected void execute(@NotNull CommandContext commandContext, @NotNull Store<EntityStore> store, @NotNull Ref<EntityStore> ref, @NotNull PlayerRef playerRef, @NotNull World world) {
        List<PlayerRef> refs = new ArrayList<>(world.getPlayerRefs());
        refs.sort(Comparator.comparing(PlayerRef::getUsername, String.CASE_INSENSITIVE_ORDER));

        Player player = store.getComponent(ref, Player.getComponentType());
        TeleportPlayersPage tppage = new TeleportPlayersPage(playerRef, refs, world);
        player.getPageManager().openCustomPage(ref, store, tppage);
    }
}
