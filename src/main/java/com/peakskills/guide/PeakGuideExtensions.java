package com.peakskills.guide;

import com.peakskills.skill.Skill;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/** Small public extension surface for optional PeakMod modules. */
public final class PeakGuideExtensions {
    private static final Map<String, GuideEntry> ENTRIES = new LinkedHashMap<>();
    private static final List<SkillUnlock> SKILL_UNLOCKS = new ArrayList<>();

    public static synchronized void registerEntry(String id, int slot, Supplier<ItemStack> icon,
                                                   Consumer<ServerPlayer> action) {
        if (id == null || id.isBlank() || slot < 37 || slot > 43 || icon == null
                || action == null) throw new IllegalArgumentException("Invalid guide extension");
        if (ENTRIES.containsKey(id)) throw new IllegalArgumentException("Duplicate guide entry: " + id);
        if (ENTRIES.values().stream().anyMatch(entry -> entry.slot() == slot)) {
            throw new IllegalArgumentException("Guide slot already occupied: " + slot);
        }
        ENTRIES.put(id, new GuideEntry(id, slot, icon, action));
    }

    public static synchronized void registerSkillUnlock(Skill skill, int level, String name,
                                                         String description, String guideCommand) {
        if (skill == null || level < 1 || level > Skill.MAX_LEVEL || name == null || name.isBlank()) {
            throw new IllegalArgumentException("Invalid skill unlock");
        }
        SKILL_UNLOCKS.add(new SkillUnlock(skill, level, name,
            description == null ? "" : description, guideCommand == null ? "peak" : guideCommand));
    }

    public static synchronized List<GuideEntry> entries() {
        return ENTRIES.values().stream().map(GuideEntry::copy).toList();
    }

    public static synchronized List<SkillUnlock> crossedUnlocks(Skill skill, int from, int to) {
        return SKILL_UNLOCKS.stream()
            .filter(unlock -> unlock.skill() == skill && unlock.level() > from && unlock.level() <= to)
            .sorted(Comparator.comparingInt(SkillUnlock::level)).toList();
    }

    public record GuideEntry(String id, int slot, Supplier<ItemStack> icon,
                             Consumer<ServerPlayer> action) {
        private GuideEntry copy() { return new GuideEntry(id, slot, icon, action); }
    }

    public record SkillUnlock(Skill skill, int level, String name, String description,
                              String guideCommand) {}

    private PeakGuideExtensions() {}
}
