package com.gmail.bobason01;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public class ActionExecutor {

    private final GUIManager plugin;
    private final GUIListener guiListener;

    public ActionExecutor(GUIManager plugin, GUIListener guiListener) {
        this.plugin = plugin;
        this.guiListener = guiListener;
    }

    // GUI 클릭 실행 로직
    public void execute(Player player, String guiId, int slot, ItemStack item, ClickType clickType) {
        if (item == null || !item.hasItemMeta()) return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        // 1. 해당 클릭 타입(좌/우/쉬프트 등)에 맞는 명령어 키 가져오기
        NamespacedKey commandKey = ActionKeyUtil.getCommandKey(clickType);

        // 2. 명령어가 설정되어 있지 않으면 무시
        if (commandKey == null || !pdc.has(commandKey, PersistentDataType.STRING)) return;

        // 3. 고유 액션 ID 생성 (쿨타임용)
        String actionId = guiId + ":" + slot + ":" + clickType.name();

        // 4. 실행에 필요한 나머지 키들 가져오기
        NamespacedKey permKey = ActionKeyUtil.getPermissionKey(clickType);
        NamespacedKey moneyCostKey = ActionKeyUtil.getMoneyCostKey(clickType);
        NamespacedKey itemCostKey = ActionKeyUtil.getItemCostKey(clickType);
        NamespacedKey cooldownKey = ActionKeyUtil.getCooldownKey(clickType);
        NamespacedKey executorKey = ActionKeyUtil.getExecutorKey(clickType);
        NamespacedKey keepOpenKey = ActionKeyUtil.getKeepOpenKey(clickType);

        // 5. 실행 로직 위임
        executePdcPath(player, pdc, item, actionId, commandKey, permKey, moneyCostKey, itemCostKey, cooldownKey, executorKey, keepOpenKey);
    }

    // 키바인딩(F, Q) 실행 로직
    public void execute(Player player, ItemStack item, ActionKeyUtil.KeyAction keyAction) {
        if (item == null || !item.hasItemMeta()) return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        NamespacedKey commandKey = ActionKeyUtil.getCommandKey(keyAction);
        if (commandKey == null || !pdc.has(commandKey, PersistentDataType.STRING)) return;

        String command = pdc.get(commandKey, PersistentDataType.STRING);
        if (command == null) return;

        // 키바인딩은 슬롯 정보가 없으므로 명령어를 ID 일부로 사용
        String actionId = player.getName() + ":" + keyAction.name() + ":" + command.hashCode();

        NamespacedKey permKey = ActionKeyUtil.getPermissionKey(keyAction);
        NamespacedKey moneyCostKey = ActionKeyUtil.getMoneyCostKey(keyAction);
        NamespacedKey itemCostKey = ActionKeyUtil.getItemCostKey(keyAction);
        NamespacedKey cooldownKey = ActionKeyUtil.getCooldownKey(keyAction);
        NamespacedKey executorKey = ActionKeyUtil.getExecutorKey(keyAction);

        executePdcPath(player, pdc, item, actionId, commandKey, permKey, moneyCostKey, itemCostKey, cooldownKey, executorKey, null);
    }

    // 공통 실행 로직 (검사 및 명령어 수행)
    private void executePdcPath(Player player,
                                PersistentDataContainer pdc, ItemStack item,
                                String actionId,
                                NamespacedKey commandKey, NamespacedKey permKey,
                                NamespacedKey moneyCostKey, NamespacedKey itemCostKey,
                                NamespacedKey cooldownKey, NamespacedKey executorKey,
                                NamespacedKey keepOpenKey) {

        // 1. 쿨타임 확인
        long remainingCooldown = plugin.getRemainingCooldownMillis(player, actionId);
        if (remainingCooldown > 0) {
            player.sendMessage(ChatColor.RED + "You must wait " + String.format("%.1f", remainingCooldown / 1000.0) + " more seconds.");
            return;
        }

        // 2. 권한 확인
        String permission = pdc.get(permKey, PersistentDataType.STRING);
        if (permission != null && !permission.isEmpty() && !player.hasPermission(permission)) {
            String noPermMsg = pdc.get(GUIManager.KEY_PERMISSION_MESSAGE, PersistentDataType.STRING);
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', noPermMsg != null ? noPermMsg : "&cYou don't have permission."));
            return;
        }

        // 3. 비용(아이템/돈) 확인 및 차감
        if (!guiListener.checkAndTakeCosts(player, pdc, moneyCostKey, itemCostKey)) {
            return; // 비용 부족 시 중단
        }

        // 4. 쿨타임 설정 (실행 확정 후)
        if (pdc.has(cooldownKey, PersistentDataType.DOUBLE)) {
            plugin.setCooldown(player, actionId, pdc.get(cooldownKey, PersistentDataType.DOUBLE));
        }

        // 5. 명령어 가져오기
        String command = pdc.get(commandKey, PersistentDataType.STRING);
        if (command == null || command.isEmpty()) return;

        // 6. GUI 닫기 여부 확인
        boolean keepOpen = keepOpenKey != null && pdc.getOrDefault(keepOpenKey, PersistentDataType.BYTE, (byte) 0) == 1;

        // 7. 타겟 플레이어 입력 필요 여부 확인
        Byte requireTarget = pdc.get(GUIManager.KEY_REQUIRE_TARGET, PersistentDataType.BYTE);
        String executorTypeName = pdc.get(executorKey, PersistentDataType.STRING);
        GUIManager.ExecutorType executor = executorTypeName != null ? GUIManager.ExecutorType.valueOf(executorTypeName) : GUIManager.ExecutorType.PLAYER;

        if (requireTarget != null && requireTarget == 1 && command.contains("{target}")) {
            // 타겟 입력 대기 상태로 전환
            plugin.setAwaitingTarget(player, new TargetInfo(command, executor));
            if (!keepOpen) player.closeInventory();
            player.sendMessage(ChatColor.GREEN + "Please enter the target player's name in chat. Type 'cancel' to abort.");
        } else {
            // 즉시 실행
            if (!keepOpen) player.closeInventory();
            executeCommand(player, command, executor, null);
        }
    }

    public void executeCommand(Player player, String command, GUIManager.ExecutorType executor, String targetName) {
        // 플레이어 이름 치환
        String finalCommand = command.replace("%player%", player.getName());
        if (targetName != null) finalCommand = finalCommand.replace("{target}", targetName);

        String commandToExecute = finalCommand;

        // 비동기 스레드에서 호출될 수 있으므로 메인 스레드에서 실행 보장
        Bukkit.getScheduler().runTask(plugin, () -> {
            switch (executor) {
                case CONSOLE:
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), commandToExecute);
                    break;
                case OP:
                    boolean wasOp = player.isOp();
                    try {
                        player.setOp(true);
                        player.performCommand(commandToExecute);
                    } finally {
                        if (!wasOp) player.setOp(false);
                    }
                    break;
                case PLAYER:
                default:
                    player.performCommand(commandToExecute);
                    break;
            }
        });
    }
}