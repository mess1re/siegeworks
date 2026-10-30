package me.mss1r.siegeworks.integration.recruits;

import com.talhanation.recruits.client.ClientManager;
import com.talhanation.recruits.client.events.CommandCategoryManager;
import com.talhanation.recruits.client.gui.CommandScreen;
import com.talhanation.recruits.client.gui.commandscreen.ICommandCategory;
import com.talhanation.recruits.client.gui.group.RecruitsCommandButton;
import com.talhanation.recruits.world.RecruitsGroup;
import me.mss1r.axiomata.blueprint.api.BlueprintStacks;
import me.mss1r.siegeworks.api.SiegeAmmunitionMode;
import me.mss1r.siegeworks.entity.base.AbstractSiegeEntity;
import me.mss1r.siegeworks.entity.siege.SiegeLadderEntity;
import me.mss1r.siegeworks.entity.siege.SiegeTowerEntity;
import me.mss1r.siegeworks.integration.recruits.network.RecruitsCommandStatesPayloads;
import me.mss1r.siegeworks.integration.recruits.network.RecruitsNetworking;
import me.mss1r.siegeworks.integration.recruits.network.RecruitsSiegeCommandC2SPayload;
import me.mss1r.siegeworks.integration.recruits.network.RecruitsTowerCrewC2SPayload;
import me.mss1r.siegeworks.registry.SiegeworksItems;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

final class RecruitsSiegeCommandCategory implements ICommandCategory {
    private static final String HOLD_FIRE_STATE = "hold_fire";
    private static final String STRATEGIC_FIRE_STATE = "strategic_fire";
    private static final int TYPE_COLUMNS = 6;
    private static final int TYPE_SPACING = 22;
    private static boolean registered;

    private CommandScreen activeScreen;
    private SiegeCommandType selectedType;

    static void register() {
        if (!registered) {
            ScreenStates.listen();
            CommandCategoryManager.register(new RecruitsSiegeCommandCategory(), 0);
            registered = true;
        }
    }

    @Override
    public Component getToolTipName() {
        return text("category");
    }

    @Override
    public ItemStack getIcon() {
        return new ItemStack(SiegeworksItems.GIANT_CANNON_BALL.get());
    }

    @Override
    public void createButtons(CommandScreen screen, int x, int y, List<RecruitsGroup> groups, Player player) {
        boolean hasActiveGroup = groups.stream().anyMatch(group -> !group.isDisabled());
        AbstractSiegeEntity target = screen.rayEntity instanceof AbstractSiegeEntity siege ? siege : null;
        ScreenStates states = ScreenStates.request(groups, target, screen.rayBlockPos);
        List<SiegeCommandType> availableTypes = states.types();
        SiegeCommandType targetType = target == null ? null : SiegeCommandType.from(target);
        if (activeScreen != screen) {
            activeScreen = screen;
            selectedType = targetType != null && availableTypes.contains(targetType)
                    ? targetType
                    : availableTypes.stream().findFirst().orElse(null);
        } else if (selectedType == null || !availableTypes.contains(selectedType)) {
            selectedType = availableTypes.stream().findFirst().orElse(null);
        }

        addTypeButtons(screen, x, y - 80, availableTypes);
        Layout layout = new Layout(states);

        layout.scope("target.");
        if (target instanceof SiegeTowerEntity tower) {
            layout.add("crew_machine", hasActiveGroup,
                    () -> sendTowerCrewCommand(groups, tower, RecruitsTowerCrewC2SPayload.ACTION_BOARD));
            layout.add("return", hasActiveGroup,
                    () -> sendTowerCrewCommand(groups, tower, RecruitsTowerCrewC2SPayload.ACTION_RETURN));
        } else if (target != null && !(target instanceof SiegeLadderEntity)) {
            layout.add("crew", hasActiveGroup,
                    () -> sendTargetedCommand(groups,
                            RecruitsSiegeCommandC2SPayload.ACTION_CREW_MACHINE, target.getId()));
            layout.add("return", hasActiveGroup,
                    () -> forEachActiveGroup(groups, group ->
                            me.mss1r.siegeworks.platform.SiegeClientPlatform.returnToMount(player.getUUID(), group.getUUID())));
        }

        if (selectedType != null) {
            layout.scope(selectedType.name() + ".");
            addSelectedTypeCommands(screen, layout, groups, hasActiveGroup, selectedType);
        }

        layout.scope("");
        layout.addGeneric("place_ladder", hasActiveGroup && screen.rayBlockPos != null, groups,
                RecruitsSiegeCommandC2SPayload.ACTION_PLACE_LADDER, screen.rayBlockPos);
        layout.addGeneric("build",
                hasActiveGroup && screen.rayBlockPos != null
                        && BlueprintStacks.isBlueprint(player.getOffhandItem()),
                groups, RecruitsSiegeCommandC2SPayload.ACTION_BUILD, screen.rayBlockPos);
        layout.addGeneric("supply", hasActiveGroup && screen.rayBlockPos != null,
                groups, RecruitsSiegeCommandC2SPayload.ACTION_SET_SUPPLIES, screen.rayBlockPos);
        layout.addGeneric("cancel_work", hasActiveGroup, groups,
                RecruitsSiegeCommandC2SPayload.ACTION_CANCEL_MAINTENANCE, null);

        layout.scope("target.");
        if (target instanceof SiegeLadderEntity) {
            layout.addTargeted("pickup_ladder", hasActiveGroup, groups,
                    RecruitsSiegeCommandC2SPayload.ACTION_PICKUP_LADDER, target.getId());
        }
        if (target != null) {
            layout.addTargeted("repair", hasActiveGroup, groups,
                    RecruitsSiegeCommandC2SPayload.ACTION_REPAIR, target.getId());
            layout.addTargeted("dismantle", hasActiveGroup, groups,
                    RecruitsSiegeCommandC2SPayload.ACTION_DISMANTLE, target.getId());
        }

        layout.place(screen, x, y + 35);
    }

    private void addTypeButtons(CommandScreen screen, int centerX, int firstY,
                                List<SiegeCommandType> availableTypes) {
        int columns = Math.min(TYPE_COLUMNS, availableTypes.size());
        int firstX = centerX - (columns * TYPE_SPACING - 2) / 2;
        for (int index = 0; index < availableTypes.size(); index++) {
            SiegeCommandType type = availableTypes.get(index);
            int buttonX = firstX + index % TYPE_COLUMNS * TYPE_SPACING;
            int buttonY = firstY + index / TYPE_COLUMNS * TYPE_SPACING;
            screen.addRenderableWidget(new SiegeTypeButton(buttonX, buttonY, type, type == selectedType, () -> {
                selectedType = type;
                screen.init(Minecraft.getInstance(), screen.width, screen.height);
            }));
        }
    }

    private static void addSelectedTypeCommands(CommandScreen screen, Layout layout,
                                                List<RecruitsGroup> groups, boolean hasActiveGroup,
                                                SiegeCommandType type) {
        if (type.kind() != SiegeCommandType.Kind.LADDER) {
            layout.addSelected("leave", hasActiveGroup, groups, type,
                    RecruitsSiegeCommandC2SPayload.ACTION_LEAVE_ENGINE, null);
        }

        switch (type.kind()) {
            case ARTILLERY -> {
                layout.add("fire_position", hasActiveGroup && screen.rayBlockPos != null, () -> {
                    sendSelectedCommand(groups, type, RecruitsSiegeCommandC2SPayload.ACTION_FIRE_POSITION,
                            screen.rayBlockPos);
                    forEachActiveGroup(groups, group -> {
                        ClientManager.addGroupSpecialState(group.getUUID(), STRATEGIC_FIRE_STATE);
                        ClientManager.removeGroupSpecialState(group.getUUID(), HOLD_FIRE_STATE);
                    });
                });
                layout.add("stop_attack", hasActiveGroup, () -> {
                    sendSelectedCommand(groups, type, RecruitsSiegeCommandC2SPayload.ACTION_HOLD_FIRE, null);
                    forEachActiveGroup(groups, group -> {
                        ClientManager.addGroupSpecialState(group.getUUID(), HOLD_FIRE_STATE);
                        ClientManager.removeGroupSpecialState(group.getUUID(), STRATEGIC_FIRE_STATE);
                    });
                });
                layout.add("fire_at_will", hasActiveGroup, () -> {
                    sendSelectedCommand(groups, type, RecruitsSiegeCommandC2SPayload.ACTION_FIRE_AT_WILL, null);
                    forEachActiveGroup(groups, group -> {
                        ClientManager.removeGroupSpecialState(group.getUUID(), HOLD_FIRE_STATE);
                        ClientManager.removeGroupSpecialState(group.getUUID(), STRATEGIC_FIRE_STATE);
                    });
                });
                addAmmunitionButtons(layout, groups, hasActiveGroup, type);
            }
            case TOWER -> {
                layout.addSelected("bridge_lower", hasActiveGroup, groups, type,
                        RecruitsSiegeCommandC2SPayload.ACTION_BRIDGE_LOWER, null);
                layout.addSelected("bridge_raise", hasActiveGroup, groups, type,
                        RecruitsSiegeCommandC2SPayload.ACTION_BRIDGE_RAISE, null);
                layout.addSelected("bridge_auto", hasActiveGroup, groups, type,
                        RecruitsSiegeCommandC2SPayload.ACTION_BRIDGE_AUTO, null);
                layout.addSelected("unload_tower", hasActiveGroup, groups, type,
                        RecruitsSiegeCommandC2SPayload.ACTION_UNLOAD_TOWER, null);
                layout.addSelected("return_tower", hasActiveGroup, groups, type,
                        RecruitsSiegeCommandC2SPayload.ACTION_RETURN_TOWER, null);
            }
            case MANTLET -> {
                layout.addSelected("flap_open", hasActiveGroup, groups, type,
                        RecruitsSiegeCommandC2SPayload.ACTION_FLAP_OPEN, null);
                layout.addSelected("flap_close", hasActiveGroup, groups, type,
                        RecruitsSiegeCommandC2SPayload.ACTION_FLAP_CLOSE, null);
            }
            case RAM -> {
                layout.add("fire_position", hasActiveGroup && screen.rayBlockPos != null, () ->
                        sendSelectedCommand(groups, type, RecruitsSiegeCommandC2SPayload.ACTION_FIRE_POSITION,
                                screen.rayBlockPos));
                layout.addSelected("attack_at_will", hasActiveGroup, groups, type,
                        RecruitsSiegeCommandC2SPayload.ACTION_FIRE_AT_WILL, null);
                layout.addSelected("stop_attack", hasActiveGroup, groups, type,
                        RecruitsSiegeCommandC2SPayload.ACTION_HOLD_FIRE, null);
            }
            case LADDER -> {
            }
        }
    }

    private static void addAmmunitionButtons(Layout layout, List<RecruitsGroup> groups,
                                             boolean hasActiveGroup, SiegeCommandType type) {
        addAmmunitionButton(layout, groups, hasActiveGroup, type, SiegeAmmunitionMode.AUTO,
                "ammo_auto", RecruitsSiegeCommandC2SPayload.ACTION_AMMO_AUTO);
        addAmmunitionButton(layout, groups, hasActiveGroup, type, SiegeAmmunitionMode.STANDARD,
                "ammo_standard", RecruitsSiegeCommandC2SPayload.ACTION_AMMO_STANDARD);
        addAmmunitionButton(layout, groups, hasActiveGroup, type, SiegeAmmunitionMode.EXPLOSIVE,
                "ammo_explosive", RecruitsSiegeCommandC2SPayload.ACTION_AMMO_EXPLOSIVE);
        addAmmunitionButton(layout, groups, hasActiveGroup, type, SiegeAmmunitionMode.INCENDIARY,
                "ammo_incendiary", RecruitsSiegeCommandC2SPayload.ACTION_AMMO_INCENDIARY);
    }

    private static void addAmmunitionButton(Layout layout, List<RecruitsGroup> groups,
                                            boolean hasActiveGroup, SiegeCommandType type,
                                            SiegeAmmunitionMode mode, String key, int action) {
        if (type.supports(mode)) {
            layout.addSelected(key, hasActiveGroup, groups, type, action, null);
        }
    }

    private static final class Layout {
        private static final int COLUMN_WIDTH = 104;
        private static final int ROW_HEIGHT = 25;
        private static final int ROWS_PER_COLUMN = 5;

        private final List<Entry> entries = new ArrayList<>();
        private final ScreenStates states;
        private String scope = "";

        private Layout(ScreenStates states) {
            this.states = states;
        }

        private record Entry(String key, String stateKey, boolean enabled, Runnable action) {
        }

        /** State key prefix for the following buttons: the target, a machine type, or none. */
        private void scope(String scope) {
            this.scope = scope;
        }

        private void add(String key, boolean enabled, Runnable action) {
            entries.add(new Entry(key, scope + key, enabled, action));
        }

        private void addSelected(String key, boolean enabled, List<RecruitsGroup> groups,
                                 SiegeCommandType type, int action, BlockPos target) {
            add(key, enabled, () -> sendSelectedCommand(groups, type, action, target));
        }

        private void addGeneric(String key, boolean enabled, List<RecruitsGroup> groups,
                                int action, BlockPos target) {
            add(key, enabled, () -> sendGenericCommand(groups, action, target));
        }

        private void addTargeted(String key, boolean enabled, List<RecruitsGroup> groups,
                                 int action, int targetEntityId) {
            add(key, enabled, () -> sendTargetedCommand(groups, action, targetEntityId));
        }

        private void place(CommandScreen screen, int originX, int originY) {
            if (entries.isEmpty()) {
                return;
            }
            int columns = (entries.size() + ROWS_PER_COLUMN - 1) / ROWS_PER_COLUMN;
            int rows = Math.min(entries.size(), ROWS_PER_COLUMN);
            for (int index = 0; index < entries.size(); index++) {
                Entry entry = entries.get(index);
                int column = index / ROWS_PER_COLUMN;
                int row = index % ROWS_PER_COLUMN;
                int buttonX = originX + Math.round((column - (columns - 1) / 2.0F) * COLUMN_WIDTH);
                int buttonY = originY + Math.round((row - (rows - 1) / 2.0F) * ROW_HEIGHT);
                boolean usable = entry.enabled() && states.allows(entry.stateKey());
                addButton(screen, buttonX, buttonY, entry.key(), usable,
                        usable ? null : states.refusal(entry.stateKey()), () -> {
                            entry.action().run();
                            ScreenStates.forget();
                            screen.init(Minecraft.getInstance(), screen.width, screen.height);
                        });
            }
        }
    }

    private static void sendSelectedCommand(List<RecruitsGroup> groups, SiegeCommandType type,
                                            int action, BlockPos targetPos) {
        RecruitsNetworking.sendToServer(new RecruitsSiegeCommandC2SPayload(
                action, activeGroupIds(groups), targetPos, type.id()));
    }

    private static void sendGenericCommand(List<RecruitsGroup> groups, int action, BlockPos targetPos) {
        RecruitsNetworking.sendToServer(
                new RecruitsSiegeCommandC2SPayload(action, activeGroupIds(groups), targetPos));
    }

    private static void sendTargetedCommand(List<RecruitsGroup> groups, int action, int targetEntityId) {
        RecruitsNetworking.sendToServer(new RecruitsSiegeCommandC2SPayload(
                action, activeGroupIds(groups), null, targetEntityId, null));
    }

    private static void sendTowerCrewCommand(List<RecruitsGroup> groups, SiegeTowerEntity tower, int action) {
        RecruitsNetworking.sendToServer(
                new RecruitsTowerCrewC2SPayload(action, tower.getId(), activeGroupIds(groups)));
    }

    private static void addButton(CommandScreen screen, int x, int y, String key, boolean active,
                                  Component refusal, Runnable action) {
        RecruitsCommandButton button = new RecruitsCommandButton(x, y, text("text." + key), ignored -> action.run());
        Component tooltip = text("tooltip." + key);
        button.setTooltip(Tooltip.create(refusal == null ? tooltip : Component.empty().append(tooltip)
                .append("\n").append(refusal.copy().withStyle(ChatFormatting.RED))));
        button.active = active;
        screen.addRenderableWidget(button);
    }

    private static List<UUID> activeGroupIds(List<RecruitsGroup> groups) {
        return groups.stream().filter(group -> !group.isDisabled()).map(RecruitsGroup::getUUID).toList();
    }

    private static void forEachActiveGroup(List<RecruitsGroup> groups,
                                           java.util.function.Consumer<RecruitsGroup> action) {
        groups.stream().filter(group -> !group.isDisabled()).forEach(action);
    }

    /**
     * Last server reply on what the selected groups can do from this screen. Requested again when the groups, the
     * targeted machine or the targeted block change, and after every order; siege buttons stay disabled until it
     * arrives.
     */
    private record ScreenStates(boolean answered, List<SiegeCommandType> types, Map<String, Component> refusals) {
        private static final ScreenStates PENDING = new ScreenStates(false, List.of(), Map.of());
        private static int lastQuery;
        private static Object askedFor;
        private static ScreenStates answer = PENDING;

        static void listen() {
            RecruitsCommandStatesPayloads.Answer.receiveWith(reply -> {
                if (reply.queryId() != lastQuery) {
                    return;
                }
                List<SiegeCommandType> types = new ArrayList<>();
                for (String name : reply.types()) {
                    for (SiegeCommandType type : SiegeCommandType.values()) {
                        if (type.name().equals(name)) {
                            types.add(type);
                        }
                    }
                }
                answer = new ScreenStates(true, List.copyOf(types), reply.refusals());
                if (Minecraft.getInstance().screen instanceof CommandScreen screen) {
                    screen.init(Minecraft.getInstance(), screen.width, screen.height);
                }
            });
        }

        static ScreenStates request(List<RecruitsGroup> groups, AbstractSiegeEntity target, BlockPos targetPos) {
            List<UUID> groupIds = activeGroupIds(groups);
            int targetId = target == null ? -1 : target.getId();
            Object key = List.of(groupIds, targetId, targetPos == null ? "" : targetPos);
            if (!key.equals(askedFor)) {
                askedFor = key;
                answer = PENDING;
                RecruitsNetworking.sendToServer(new RecruitsCommandStatesPayloads.Query(
                        ++lastQuery, groupIds, targetId, targetPos));
            }
            return answer;
        }

        static void forget() {
            askedFor = null;
        }

        boolean allows(String key) {
            return answered && (!refusals.containsKey(key) || refusals.get(key) == null);
        }

        Component refusal(String key) {
            return refusals.get(key);
        }
    }

    private static Component text(String suffix) {
        return Component.translatable("gui.siegeworks.recruits.siege_commands." + suffix);
    }
}
