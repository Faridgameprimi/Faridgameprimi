package com.example.eloadmin;

import dev.lrxh.api.NeptuneAPI;
import dev.lrxh.api.NeptuneAPIProvider;
import dev.lrxh.api.data.IGlobalStats;
import dev.lrxh.api.profile.IProfile;
import dev.lrxh.api.profile.IProfileService;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.concurrent.CompletableFuture;

/**
 * Plugin tambahan (addon) untuk Neptune.
 * Menambahkan command "/elo <add|remove|set> <player> <jumlah>"
 * yang mengubah Elo global pemain lewat NeptuneAPI resmi
 * (tanpa menyentuh/ mengubah file Neptune.jar aslinya).
 *
 * Wajib di-load SETELAH Neptune (lihat paper-plugin.yml).
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

        if (args.length < 3) {
            sender.sendMessage("§cPenggunaan: /elo <add|remove|set> <player> <jumlah>");
            return true;
        }

        String action = args[0].toLowerCase();
        String targetName = args[1];

        if (!action.equals("add") && !action.equals("remove") && !action.equals("set")) {
            sender.sendMessage("§cAksi tidak dikenal. Gunakan: add, remove, atau set.");
            return true;
        }

        int amount;
        try {
            amount = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            sender.sendMessage("§cJumlah harus berupa angka bulat, contoh: 50");
            return true;
        }

        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null) {
            // Catatan: NeptuneAPI di versi ini hanya mengekspos profile lewat
            // IProfileService#getProfile(UUID), yang mengacu ke profile pemain
            // yang sedang online (di-cache saat join). Pemain harus online
            // untuk saat ini bisa diubah Elo-nya lewat command ini.
            sender.sendMessage("§cPlayer harus online untuk mengubah Elo: " + targetName);
            return true;
        }

        NeptuneAPI api = NeptuneAPIProvider.getAPI();
        if (api == null) {
            sender.sendMessage("§cNeptuneAPI belum siap / tidak tersedia.");
            return true;
        }

        IProfileService profileService = api.getProfileService();

        @SuppressWarnings("unchecked")
        CompletableFuture<IProfile> future = (CompletableFuture<IProfile>) profileService.getProfile(target.getUniqueId());

        future.thenAccept(profile -> Bukkit.getScheduler().runTask(this, () -> {
            if (profile == null) {
                sender.sendMessage("§cProfile untuk " + targetName + " tidak ditemukan.");
                return;
            }

            IGlobalStats stats = profile.getGameData().getGlobalStats();
            int current = stats.getElo();
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

            stats.setElo(result);
            sender.sendMessage("§aElo " + target.getName() + " diubah dari §e" + current
                    + " §amenjadi §e" + result + "§a.");
            if (target.isOnline() && !target.getName().equals(sender.getName())) {
                target.sendMessage("§aElo kamu diubah oleh " + sender.getName()
                        + " menjadi §e" + result + "§a.");
            }
        })).exceptionally(ex -> {
            getLogger().warning("Gagal mengubah elo " + targetName + ": " + ex.getMessage());
            sender.sendMessage("§cTerjadi error saat mengubah Elo. Cek console server.");
            return null;
        });

        return true;
    }
}
