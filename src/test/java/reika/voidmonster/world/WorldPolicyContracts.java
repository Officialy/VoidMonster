package reika.voidmonster.world;

import java.util.List;

/** Runs with just Java 25 while the entity/client dependency cluster is still being ported. */
public final class WorldPolicyContracts {
    private static int checks;

    public static void main(String[] args) {
        DimensionRules<String> rules = new DimensionRules<>();
        check(rules.allows("overworld"), "Empty blacklist admits unlisted worlds");
        rules.configure(List.of("end", "other"), false);
        check(!rules.allows("end"), "Blacklist denies listed world");
        check(rules.allows("overworld"), "Blacklist admits unlisted world");
        rules.override("end", true);
        check(rules.allows("end"), "API allow overrides blacklist");
        rules.override("overworld", false);
        check(!rules.allows("overworld"), "API denial overrides list");
        rules.configure(List.of("nether"), true);
        check(rules.allows("nether"), "Whitelist admits listed world");
        check(!rules.allows("other"), "Whitelist denies unlisted world");
        check(rules.allows("end"), "Reload retains API overrides");
        check(!rules.allows("overworld"), "Reload retains API denial");
        rules.addDimensions(List.of("other"));
        check(rules.allows("other"), "API list extension preserves whitelist semantics");
        rules.configure(List.of(), true);
        check(!rules.allows("nether"), "Reload replaces old list rather than appending");

        MonsterCooldowns<Object> cooldowns = new MonsterCooldowns<>();
        Object first = new Object();
        Object second = new Object();
        check(!cooldowns.active(first, 50000), "World without deadline is ready");
        cooldowns.add(first, 50000, 600);
        check(cooldowns.active(first, 50000), "Duration becomes an absolute expiry even in an old world");
        check(cooldowns.active(first, 50599), "Deadline remains active until final tick");
        check(!cooldowns.active(second, 50000), "Cooldown does not leak to another world");
        cooldowns.add(first, 50010, 20);
        check(cooldowns.active(first, 50599), "Shorter delay cannot shorten cooldown");
        cooldowns.add(first, 50500, 500);
        check(cooldowns.active(first, 50999), "Longer delay extends cooldown");
        check(!cooldowns.active(first, 51000), "Deadline expires at exact game-time tick");
        cooldowns.add(first, Integer.MAX_VALUE, 3600);
        check(cooldowns.active(first, (long)Integer.MAX_VALUE + 3599), "Deadlines do not overflow int game time");
        cooldowns.clear(first);
        check(!cooldowns.active(first, 0), "Unloading removes deadline before world reuse");
        cooldowns.add(first, 100, 0);
        check(!cooldowns.active(first, 100), "Zero delay is immediately ready");
        boolean rejected = false;
        try { cooldowns.add(first, 100, -1); }
        catch (IllegalArgumentException expected) { rejected = true; }
        check(rejected, "Negative durations are rejected");
        System.out.println("VoidMonster world policy contracts: " + checks + "/" + checks + " passed");
    }

    private static void check(boolean success, String message) {
        if (!success)
            throw new AssertionError(message);
        checks++;
    }
}
