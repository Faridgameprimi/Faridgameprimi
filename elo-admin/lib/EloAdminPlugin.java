package com.example.eloadmin;

import dev.lrxh.api.NeptuneAPI;
import dev.lrxh.api.NeptuneAPIProvider;
import dev.lrxh.api.kit.IKit;
import dev.lrxh.api.kit.IKitService;
import dev.lrxh.api.profile.IProfile;
import dev.lrxh.api.profile.IProfileService;
import dev.lrxh.neptune.game.kit.Kit;
import dev.lrxh.neptune.profile.data.GameData;
import dev.lrxh.neptune.profile.data.GlobalStats;
import dev.lrxh.neptune.profile.data.KitData;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Addon untuk Neptune yang menambahkan command:
 *   /elo <add|remove|set> <player> <kit> <jumlah>
 *
 * PENTING: Elo & Divisi di Neptune tersimpan PER-KIT (bukan satu nilai
 * global). UI "Statistics", chat, dan tab list membaca nilai per-kit ini
 * (lewat KitData) + recalculated division-nya, bukan nilai "global stats"
 * yang terpisah. Karena itu, sekarang plugin ini mengubah langsung
 * KitData milik kit yang dipilih (kelas internal Neptune, sama persis
 * yang dipakai game-nya sendiri), lalu memperbarui division dan
 * agregat global stats-nya juga.
 *
 * Catatan: ini memakai kelas internal Neptune (bukan cuma public API),
 * karena public API belum menyediakan setter elo per-kit. Artinya kalau
 * developer Neptune mengubah struktur internalnya di update berikutnya,
 * plugin ini bisa perlu disesuaikan lagi.
 */
public class EloAdminPlugin extends JavaPlugin implements CommandExecutor {

    @Override
    public void onEnable() {
        if (getCommand("elo") != null) {
            getCommand("elo").setExecutor(this);
        }

        if (Bukkit.getPluginManager().getPlugin("Neptune") == null) {
            getLogger().warning("Plugin Neptune tidak ditemukan! EloAdmin butuh Neptune untuk berjalan.");
        } else {
            getLogger().info("EloAdmin aktif, terhubung ke Neptune.");
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("neptune.elo.admin")) {
            sender.sendMessage("§cKamu tidak punya izin untuk perintah ini.");
            return true;
        }

        if (args.length < 4) {
            sender.sendMessage("§cPenggunaan: /elo <add|remove|set> <player> <kit> <jumlah>");
            return true;
        }

        String action = args[0].toLowerCase();
        String targetName = args[1];
        String kitName = args[2];

        if (!action.equals("add") && !action.equals("remove") && !action.equals("set")) {
            sender.sendMessage("§cAksi tidak dikenal. Gunakan: add, remove, atau set.");
            return true;
        }

        int amount;
        try {
            amount = Integer.parseInt(args[3]);
        } catch (NumberFormatException e) {
            sender.sendMessage("§cJumlah harus berupa angka bulat, contoh: 50");
            return true;
        }

        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null) {
            sender.sendMessage("§cPlayer harus online untuk mengubah Elo: " + targetName);
            return true;
        }

        NeptuneAPI api = NeptuneAPIProvider.getAPI();
        if (api == null) {
            sender.sendMessage("§cNeptuneAPI belum siap / tidak tersedia.");
            return true;
        }

        IKitService kitService = api.getKitService();
        IKit iKit = kitService.getKitByName(kitName);
        if (iKit == null) {
            iKit = kitService.getKitByDisplay(kitName);
        }

        if (iKit == null || !(iKit instanceof Kit)) {
            String available = kitService.getAllKits().stream()
                    .map(IKit::getName)
                    .collect(Collectors.joining(", "));
            sender.sendMessage("§cKit tidak ditemukan: " + kitName);
            sender.sendMessage("§7Kit yang tersedia: " + available);
            return true;
        }

        final Kit kit = (Kit) iKit;

        IProfileService profileService = api.getProfileService();
        @SuppressWarnings("unchecked")
        CompletableFuture<IProfile> future = (CompletableFuture<IProfile>) profileService.getProfile(target.getUniqueId());

        future.thenAccept(profile -> Bukkit.getScheduler().runTask(this, () -> {
            if (profile == null || !(profile.getGameData() instanceof GameData)) {
                sender.sendMessage("§cProfile untuk " + targetName + " tidak ditemukan.");
                return;
            }

            GameData gameData = (GameData) profile.getGameData();
            KitData kitData = gameData.get(kit);
            if (kitData == null) {
                sender.sendMessage("§cData kit tidak ditemukan untuk " + targetName + " di kit " + kit.getName());
                return;
            }

            int current = kitData.getElo();
            int result;

            switch (action) {
                case "add":
                    result = current + amount;
                    break;
                case "remove":
                    result = Math.max(0, current - amount);
                    break;
                default: // set
                    result = amount;
                    break;
            }

            // Update elo per-kit (sumber data yang dipakai UI Statistics/chat/tab)
            kitData.setElo(result);
            // Recalculate division berdasarkan elo baru, persis seperti yang
            // dilakukan game setiap kali elo berubah lewat match biasa.
            kitData.updateDivision();

            // Sinkronkan juga agregat global stats-nya.
            GlobalStats globalStats = gameData.getGlobalStats();
            if (globalStats != null) {
                globalStats.update();
            }

            sender.sendMessage("§aElo " + target.getName() + " di kit §e" + kit.getDisplayName()
                    + " §adiubah dari §e" + current + " §amenjadi §e" + result + "§a.");
            if (!target.getName().equals(sender.getName())) {
                target.sendMessage("§aElo kamu di kit " + kit.getDisplayName() + " diubah oleh "
                        + sender.getName() + " menjadi §e" + result + "§a.");
            }
        })).exceptionally(ex -> {
            getLogger().warning("Gagal mengubah elo " + targetName + ": " + ex.getMessage());
            sender.sendMessage("§cTerjadi error saat mengubah Elo. Cek console server.");
            return null;
        });

        return true;
    }
}
