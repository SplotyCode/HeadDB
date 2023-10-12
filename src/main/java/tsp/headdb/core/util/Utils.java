package tsp.headdb.core.util;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import tsp.headdb.HeadDB;
import tsp.headdb.core.api.HeadAPI;
import tsp.headdb.core.economy.BasicEconomyProvider;
import tsp.headdb.core.hook.Hooks;
import tsp.headdb.implementation.category.Category;
import tsp.headdb.implementation.head.Head;
import tsp.nexuslib.inventory.Button;
import tsp.nexuslib.inventory.PagedPane;
import tsp.nexuslib.inventory.Pane;
import tsp.nexuslib.util.StringUtils;
import tsp.nexuslib.util.Validate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class Utils {

    private static final HeadDB instance = HeadDB.getInstance();

    public static String toString(Collection<String> set) {
        String[] array = set.toArray(new String[0]);

        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < array.length; i++) {
            builder.append(array[i]);
            if (i < array.length - 1) {
                builder.append(",");
            }
        }

        return builder.toString();
    }

    public static Optional<UUID> validateUniqueId(@Nonnull String raw) {
        try {
            return Optional.of(UUID.fromString(raw));
        } catch (IllegalArgumentException ignored) {
            return Optional.empty();
        }
    }

    @ParametersAreNonnullByDefault
    public static String translateTitle(String raw, int size, String category, @Nullable String query) {
        return StringUtils.colorize(raw)
                .replace("%size%", String.valueOf(size))
                .replace("%category%", category)
                .replace("%query%", (query != null ? query : "%query%"));
    }

    @ParametersAreNonnullByDefault
    public static String translateTitle(String raw, int size, String category) {
        return translateTitle(raw, size, category, null);
    }

    public static boolean matches(String provided, String query) {
        provided = ChatColor.stripColor(provided.toLowerCase(Locale.ROOT));
        query = query.toLowerCase(Locale.ROOT);
        return provided.equals(query)
                || provided.startsWith(query)
                || provided.contains(query);
                //|| provided.endsWith(query);
    }

    public static void fill(@Nonnull Pane pane, @Nullable ItemStack item) {
        Validate.notNull(pane, "Pane can not be null!");

        if (item == null) {
            item = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
            ItemMeta meta = item.getItemMeta();
            //noinspection DataFlowIssue
            meta.setDisplayName("");
            item.setItemMeta(meta);
        }

        for (int i = 0; i < pane.getInventory().getSize(); i++) {
            ItemStack current = pane.getInventory().getItem(i);
            if (current == null || current.getType().isAir()) {
                pane.setButton(i, new Button(item, e -> e.setCancelled(true)));
            }
        }
    }

    @SuppressWarnings("SpellCheckingInspection")
    public static PagedPane createPaged(Player player, String title) {
        PagedPane main = new PagedPane(4, 6, title);
        HeadAPI.getHeadByTexture("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvODY1MmUyYjkzNmNhODAyNmJkMjg2NTFkN2M5ZjI4MTlkMmU5MjM2OTc3MzRkMThkZmRiMTM1NTBmOGZkYWQ1ZiJ9fX0=").ifPresent(head -> main.setBackItem(head.getItem(player.getUniqueId())));
        HeadAPI.getHeadByTexture("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvY2Q5MWY1MTI2NmVkZGM2MjA3ZjEyYWU4ZDdhNDljNWRiMDQxNWFkYTA0ZGFiOTJiYjc2ODZhZmRiMTdmNGQ0ZSJ9fX0=").ifPresent(head -> main.setCurrentItem(head.getItem(player.getUniqueId())));
        HeadAPI.getHeadByTexture("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMmEzYjhmNjgxZGFhZDhiZjQzNmNhZThkYTNmZTgxMzFmNjJhMTYyYWI4MWFmNjM5YzNlMDY0NGFhNmFiYWMyZiJ9fX0=").ifPresent(head -> main.setNextItem(head.getItem(player.getUniqueId())));
        main.setControlCurrent(new Button(main.getCurrentItem(), e -> Bukkit.dispatchCommand(player, "hdb")));
        return main;
    }

    public static void openFavoritesMenu(Player player) {
        List<Head> heads = HeadAPI.getFavoriteHeads(player.getUniqueId());
        PagedPane main = Utils.createPaged(player, Utils.translateTitle(HeadDB.getInstance().getLocalization().getMessage(player.getUniqueId(), "menu.main.favorites.name").orElse("Favorites"), heads.size(), "Favorites"));
        for (Head head : heads) {
            main.addButton(new Button(head.getItem(player.getUniqueId()), fe -> {
                if (!player.hasPermission("headdb.favorites")) {
                    HeadDB.getInstance().getLocalization().sendMessage(player, "noAccessFavorites");
                    return;
                }

                if (fe.isLeftClick()) {
                    int amount = 1;
                    if (fe.isShiftClick()) {
                        amount = 64;
                    }

                    Utils.purchase(player, head, amount);
                } else if (fe.isRightClick()) {
                    HeadDB.getInstance().getStorage().getPlayerStorage().removeFavorite(player.getUniqueId(), head.getTexture());
                    HeadDB.getInstance().getLocalization().sendMessage(player, "removedFavorite", msg -> msg.replace("%name%", head.getName()));
                    openFavoritesMenu(player);
                }
            }));
        }

        main.open(player);
    }

    @ParametersAreNonnullByDefault
    public static void addHeads(Player player, @Nullable Category category, PagedPane pane, Collection<Head> heads) {
        for (Head head : heads) {
            ItemStack item = head.getItem(player.getUniqueId());
            pane.addButton(new Button(item, e -> {
                e.setCancelled(true);

                if (category != null && instance.getConfig().getBoolean("requireCategoryPermission") && !player.hasPermission("headdb.category." + category.getName())) {
                    instance.getLocalization().sendMessage(player.getUniqueId(), "noPermission");
                    return;
                }

                if (e.isLeftClick()) {
                    int amount = 1;
                    if (e.isShiftClick()) {
                        amount = 64;
                    }

                    purchase(player, head, amount);
                } else if (e.isRightClick()) {
                    if (player.hasPermission("headdb.favorites")) {
                        HeadDB.getInstance().getStorage().getPlayerStorage().addFavorite(player.getUniqueId(), head.getTexture());
                        HeadDB.getInstance().getLocalization().sendMessage(player, "addedFavorite", msg -> msg.replace("%name%", head.getName()));
                    } else {
                        HeadDB.getInstance().getLocalization().sendMessage(player, "noAccessFavorites");
                    }
                }
           }));
        }
    }

    private static CompletableFuture<Boolean> processPayment(Player player, Head head, int amount) {
        Optional<BasicEconomyProvider> optional = HeadDB.getInstance().getEconomyProvider();
        if (optional.isEmpty()) {
            return CompletableFuture.completedFuture(true); // No economy, the head is free
        } else {
            BigDecimal cost = BigDecimal.valueOf(HeadDB.getInstance().getConfig().getDouble("economy.cost." + head.getCategory().getName()) * amount);
            HeadDB.getInstance().getLocalization().sendMessage(player.getUniqueId(), "processPayment", msg -> msg
                    .replace("%name%", head.getName())
                    .replace("%amount%", String.valueOf(amount))
                    .replace("%cost%", HeadDB.getInstance().getDecimalFormat().format(cost))
            );
            return optional.get().purchase(player, cost).thenApply(success -> {
                Bukkit.getScheduler().runTask(HeadDB.getInstance(), () -> {
                    if (success) {
                        HeadDB.getInstance().getLocalization().sendMessage(player, "completePayment", msg -> msg
                                .replace("%name%", head.getName())
                                .replace("%cost%", cost.toString()));
                    } else {
                        HeadDB.getInstance().getLocalization().sendMessage(player, "invalidFunds", msg -> msg.replace("%name%", head.getName()));
                    }
                });
                return success;

                /* Note: Issues caused by sync call to async event but when run async above method fucks up.
                Bukkit.getScheduler().runTaskAsynchronously(HeadDB.getInstance(), () -> {
                    HeadPurchaseEvent event = new HeadPurchaseEvent(player, head, cost, success);
                    Bukkit.getPluginManager().callEvent(event);
                });
                return true;
                 */
            });
        }
    }

    public static void purchase(Player player, Head head, int amount) {
        processPayment(player, head, amount).whenComplete((success, ex) -> {
            if (ex != null) {
                HeadDB.getInstance().getLog().error("Failed to purchase head '" + head.getName() + "' for player: " + player.getName());
                ex.printStackTrace();
            } else {
                    // Bukkit API, therefore task is ran sync.
                    Bukkit.getScheduler().runTask(HeadDB.getInstance(), () -> {
                        if (success) {
                            ItemStack item = head.getItem(player.getUniqueId());
                            item.setAmount(amount);
                            player.getInventory().addItem(item);
                            HeadDB.getInstance().getConfig().getStringList("commands.purchase").forEach(command -> {
                                if (command.isEmpty()) {
                                    return;
                                }
                                if (Hooks.PAPI.enabled()) {
                                    command = PlaceholderAPI.setPlaceholders(player, command);
                                }

                                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
                            });
                        }
                    });
            }
        });
    }

    public static Optional<String> getTexture(ItemStack head) {
        ItemMeta meta = head.getItemMeta();
        if (!(meta instanceof SkullMeta skullMeta)) {
            return Optional.empty();
        }

        try {
            Field profileField = meta.getClass().getDeclaredField("profile");
            profileField.setAccessible(true);
            GameProfile profile = (GameProfile) profileField.get(meta);
            if (profile == null) {
                return Optional.empty();
            }

            return profile.getProperties().get("textures").stream()
                    .filter(p -> p.name().equals("textures"))
                    .findAny()
                    .map(Property::value);
        } catch (NoSuchFieldException | SecurityException | IllegalAccessException e ) {
            e.printStackTrace();
            return Optional.empty();
        }
    }

    public static int resolveInt(String raw) {
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException nfe) {
            return 1;
        }
    }

    public static ItemStack getItemFromConfig(String path, Material def) {
        ConfigurationSection section = HeadDB.getInstance().getConfig().getConfigurationSection(path);
        Validate.notNull(section, "Section can not be null!");

        Material material = Material.matchMaterial(section.getString("material", def.name()));
        if (material == null) {
            material = def;
        }

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            //noinspection DataFlowIssue
            meta.setDisplayName(StringUtils.colorize(section.getString("name")));

            List<String> lore = new ArrayList<>();
            for (String line : section.getStringList("lore")) {
                if (line != null && !line.isEmpty()) {
                    lore.add(StringUtils.colorize(line));
                }
            }
            meta.setLore(lore);
            item.setItemMeta(meta);
        }

        return item;
    }

}
