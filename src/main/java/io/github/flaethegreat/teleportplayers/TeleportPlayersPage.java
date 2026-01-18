package io.github.flaethegreat.teleportplayers;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.util.List;

public class TeleportPlayersPage extends InteractiveCustomUIPage<TeleportPlayersPage.TeleportData> {
    List<PlayerRef> refs;
    World world;

    private int pageIndex = 0;
    private static final int PAGE_SIZE = 8; // <-- CHANGED from 5 to 8

    public static class TeleportData {
        public String XCoord;
        public String YCoord;
        public String ZCoord;

        public String Action;
        public String Target;

        public static final BuilderCodec<TeleportData> CODEC =
                BuilderCodec.builder(TeleportData.class, TeleportData::new)
                        .append(
                                new KeyedCodec<>("@Action", Codec.STRING),
                                (obj, val) -> obj.Action = val,
                                (obj) -> obj.Action
                        ).add()
                        .append(
                                new KeyedCodec<>("@XCoord", Codec.STRING),
                                (obj, val) -> obj.XCoord = val,
                                (obj) -> obj.XCoord
                        ).add()
                        .append(
                                new KeyedCodec<>("@YCoord", Codec.STRING),
                                (obj, val) -> obj.YCoord = val,
                                (obj) -> obj.YCoord
                        ).add()
                        .append(
                                new KeyedCodec<>("@ZCoord", Codec.STRING),
                                (obj, val) -> obj.ZCoord = val,
                                (obj) -> obj.ZCoord
                        ).add()
                        .append(
                                new KeyedCodec<>("@Target", Codec.STRING),
                                (obj, val) -> obj.Target = val,
                                (obj) -> obj.Target
                        ).add()
                        .build();
    }

    public TeleportPlayersPage(@Nonnull PlayerRef playerRef, List<PlayerRef> refs, World world) {
        this.world = world;
        this.refs = refs;
        super(playerRef, CustomPageLifetime.CanDismissOrCloseThroughInteraction, TeleportData.CODEC);
    }

    @Override
    public void build(@NotNull Ref<EntityStore> ref, @NotNull UICommandBuilder ui, @NotNull UIEventBuilder evt, @NotNull Store<EntityStore> store) {
        ui.append("Pages/TeleportPlayers.ui");

        fillPage(ui, store, refs);

        evt.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#TeleportButton",
                new EventData()
                        .append("@Action", "#TeleportButtonAction.Value")
                        .append("@XCoord", "#XCoord.Value")
                        .append("@YCoord", "#YCoord.Value")
                        .append("@ZCoord", "#ZCoord.Value")
        );

        createEvt(evt, "#LastPage");
        createEvt(evt, "#NextPage");

        // Bind P1..P8 (driven by PAGE_SIZE)
        for (int i = 1; i <= PAGE_SIZE; i++) {
            bindPlayerButtons(evt, i);
        }
    }

    private static String fmt(double v) {
        return String.format("%.1f", v);
    }

    private static double dist(Vector3d a, Vector3d b) {
        double dx = a.getX() - b.getX();
        double dy = a.getY() - b.getY();
        double dz = a.getZ() - b.getZ();
        return Math.sqrt(dx*dx + dy*dy + dz*dz);
    }

    private void fillPage(UICommandBuilder ui, Store<EntityStore> store, List<PlayerRef> refs) {
        int start = pageIndex * PAGE_SIZE;
        int maxPage = Math.max(0, (refs.size() - 1) / PAGE_SIZE);

        ui.set("#PageLabel.Text", "Page " + (pageIndex + 1) + "/" + (maxPage + 1));

        boolean canGoBack = pageIndex > 0;
        boolean canGoNext = pageIndex < maxPage;

        ui.set("#LastPageAction.Value", canGoBack ? "lastPage" : "");
        ui.set("#NextPageAction.Value", canGoNext ? "nextPage" : "");

        ui.set("#LastPage.Text", canGoBack ? "Last Page" : "No Pages");
        ui.set("#NextPage.Text", canGoNext ? "Next Page" : "No Pages");

        Ref<EntityStore> meEntity = this.playerRef.getReference();
        Vector3d myPos = getPosition(store, meEntity);

        for (int slot = 0; slot < PAGE_SIZE; slot++) {
            int i = start + slot;
            int row = slot + 1;

            String infoText = "#P" + row + "Info.Text";
            String nameText = "#P" + row + "Name.Text";
            String targetVal = "#P" + row + "Target.Value";

            String tpToAction = "#P" + row + "TPToAction.Value";
            String tpHereAction = "#P" + row + "TPHereAction.Value";

            String tpToText = "#P" + row + "TPTo.Text";
            String tpHereText = "#P" + row + "TPHere.Text";

            if (i < refs.size()) {
                PlayerRef p = refs.get(i);
                if (p == null || p.getUsername() == null) {
                    ui.set(nameText, "");
                    ui.set(targetVal, "");
                    ui.set(tpToAction, "");
                    ui.set(tpHereAction, "");
                    ui.set(tpToText, "—");
                    ui.set(tpHereText, "—");
                    ui.set(infoText, "");
                    continue;
                }

                String username = p.getUsername();
                ui.set(nameText, username);
                ui.set(targetVal, username);

                ui.set(tpToAction, "tpTo");
                ui.set(tpHereAction, "tpHere");
                ui.set(tpToText, "TP TO");
                ui.set(tpHereText, "TP HERE");

                Ref<EntityStore> targetEntity = p.getReference();
                if (targetEntity == null) {
                    ui.set(infoText, "");
                    continue;
                }

                Vector3d targetPos = getPosition(store, targetEntity);
                double d = dist(myPos, targetPos);

                ui.set(infoText,
                        "X " + fmt(targetPos.getX()) +
                                " Y " + fmt(targetPos.getY()) +
                                " Z " + fmt(targetPos.getZ()) +
                                " D " + fmt(d)
                );
            } else {
                ui.set(nameText, "");
                ui.set(targetVal, "");

                ui.set(tpToAction, "");
                ui.set(tpHereAction, "");

                ui.set(tpToText, "—");
                ui.set(tpHereText, "—");

                ui.set(infoText, "");
            }
        }
    }

    private void bindPlayerButtons(UIEventBuilder evt, int i) {
        String n = String.valueOf(i);

        evt.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#P" + n + "TPTo",
                new EventData()
                        .append("@Action", "#P" + n + "TPToAction.Value")
                        .append("@Target", "#P" + n + "Target.Value")
        );

        evt.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#P" + n + "TPHere",
                new EventData()
                        .append("@Action", "#P" + n + "TPHereAction.Value")
                        .append("@Target", "#P" + n + "Target.Value")
        );
    }

    private void createEvt(UIEventBuilder evt, String id) {
        evt.addEventBinding(
                CustomUIEventBindingType.Activating,
                id,
                new EventData()
                        .append("@Action", id + "Action.Value")
                        .append("@XCoord", "#XCoord.Value")
                        .append("@YCoord", "#YCoord.Value")
                        .append("@ZCoord", "#ZCoord.Value")
        );
    }

    private PlayerRef findTargetByUsername(String username) {
        if (username == null) return null;
        for (PlayerRef p : refs) {
            if (p != null && username.equalsIgnoreCase(p.getUsername())) {
                return p;
            }
        }
        return null;
    }

    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nonnull TeleportData data) {
        if (data.Action == null || data.Action.isBlank()) {
            sendUpdate();
            return;
        }

        if ("teleport".equals(data.Action)) {
            try {
                double x = (data.XCoord == null || data.XCoord.isEmpty()) ? 0 : Double.parseDouble(data.XCoord);
                double y = (data.YCoord == null || data.YCoord.isEmpty()) ? 0 : Double.parseDouble(data.YCoord);
                double z = (data.ZCoord == null || data.ZCoord.isEmpty()) ? 0 : Double.parseDouble(data.ZCoord);

                Teleport teleport = new Teleport(new Vector3d(x, y, z), new Vector3f(0, 0, 0));
                world.execute(() -> store.addComponent(ref, Teleport.getComponentType(), teleport));
                this.close();
                return;
            } catch (NumberFormatException ignored) {
                return;
            }
        }

        if ("tpTo".equals(data.Action) || "tpHere".equals(data.Action)) {
            PlayerRef target = findTargetByUsername(data.Target);
            if (target == null) return;

            Ref<EntityStore> targetEntity = target.getReference();
            if (targetEntity == null) return;

            if ("tpTo".equals(data.Action)) {
                Vector3d targetPos = getPosition(store, targetEntity);
                teleportEntity(store, ref, targetPos);
                sendUpdate();
                return;
            }

            if ("tpHere".equals(data.Action)) {
                Vector3d myPos = getPosition(store, ref);
                teleportEntity(store, targetEntity, myPos);
                sendUpdate();
                return;
            }
        }

        if ("nextPage".equals(data.Action)) {
            int maxPage = Math.max(0, (refs.size() - 1) / PAGE_SIZE);
            pageIndex = Math.min(pageIndex + 1, maxPage);
            this.rebuild();
            sendUpdate();
            return;
        }

        if ("lastPage".equals(data.Action)) {
            pageIndex = Math.max(pageIndex - 1, 0);
            this.rebuild();
            sendUpdate();
            return;
        }
    }

    private Vector3d getPosition(Store<EntityStore> store, Ref<EntityStore> entity) {
        TransformComponent t = store.getComponent(entity, TransformComponent.getComponentType());
        if (t == null) return new Vector3d(0, 0, 0);
        return t.getPosition();
    }

    private void teleportEntity(Store<EntityStore> store, Ref<EntityStore> entity, Vector3d pos) {
        Teleport tp = new Teleport(pos, new Vector3f(0, 0, 0));
        world.execute(() -> store.addComponent(entity, Teleport.getComponentType(), tp));
    }
}
