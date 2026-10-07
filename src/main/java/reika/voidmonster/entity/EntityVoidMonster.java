/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.voidmonster.entity;

import java.util.ArrayList;
import java.util.Collection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import reika.dragonapi.instantiable.MotionTracker;
import reika.dragonapi.instantiable.RayTracer;
import reika.dragonapi.interfaces.entity.ClampedDamage;
import reika.dragonapi.interfaces.entity.DestroyOnUnload;
import reika.voidmonster.VoidClient;
import reika.voidmonster.VoidMonster;
import reika.voidmonster.api.PlayerLookAtVoidMonsterEvent;
import reika.voidmonster.api.VoidMonsterEatLightEvent;
import reika.voidmonster.api.VoidMonsterHook;
import reika.voidmonster.auxiliary.GhostMonsterDamage;
import reika.voidmonster.auxiliary.VoidMonsterBait;
import reika.voidmonster.auxiliary.VoidMonsterDamage;
import reika.voidmonster.auxiliary.VoidMonsterDrops;
import reika.voidmonster.compat.VoidCompatibility;
import reika.voidmonster.world.MonsterGenerator;

public class EntityVoidMonster extends Monster implements RayTracer.MultipointChecker<LivingEntity>,
        DestroyOnUnload, ClampedDamage {
    private static final EntityDataAccessor<Boolean> NETHER = SynchedEntityData.defineId(EntityVoidMonster.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> GHOST = SynchedEntityData.defineId(EntityVoidMonster.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> HEAL_TIME = SynchedEntityData.defineId(EntityVoidMonster.class, EntityDataSerializers.INT);
    public boolean forcePersist;
    private float baseDifficulty = 1;
    public int innerRotation;
    private int hitCooldown;
    private int ghostTick;
    private int difficultyBoosts;
    private final MotionTracker motionTracker = new MotionTracker(60, 10);
    private final Collection<VoidMonsterHook> hooks = new ArrayList<>();
    private final RayTracer.RayTracerWithCache<?> sight = RayTracer.getMultipointVisualLOSForRenderCulling(this);

    public EntityVoidMonster(EntityType<? extends EntityVoidMonster> type, Level level) {
        super(type, level);
        xpReward = 20000;
        innerRotation = random.nextInt(100000);
        float difficulty = getDifficulty();
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(300D * difficulty * difficulty);
        getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(15D * difficulty);
        setHealth(getMaxHealth());
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 300).add(Attributes.ATTACK_DAMAGE, 15);
    }

    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(NETHER, false);
        builder.define(GHOST, false);
        builder.define(HEAL_TIME, 0);
    }

    public EntityVoidMonster setNether() { entityData.set(NETHER, true); return this; }
    public EntityVoidMonster setGhost() { entityData.set(GHOST, true); return this; }
    public boolean isNetherVoid() { return entityData.get(NETHER); }
    public boolean isGhost() { return entityData.get(GHOST); }
    public float getDifficulty() { return baseDifficulty * VoidMonster.instance.getMonsterDifficulty(); }

    public void increaseDifficulty(float multiplier) {
        if (multiplier < 1)
            throw new IllegalArgumentException(multiplier + " is < 1!");
        baseDifficulty *= multiplier;
        getAttribute(Attributes.MAX_HEALTH).addPermanentModifier(new AttributeModifier(
                Identifier.fromNamespaceAndPath(VoidMonster.MODID, "difficulty_boost_" + difficultyBoosts++),
                multiplier, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
    }

    public void addHook(VoidMonsterHook hook) { hooks.add(hook); }

    @Override public void tick() {
        if (!forcePersist && (!VoidMonster.allowedIn(level()) || level().players().isEmpty())) {
            discard();
            return;
        }
        VoidMonster.registerExistingMonster(this);
        if (!level().isClientSide())
            hooks.forEach(hook -> hook.tick(this));
        super.tick();
        innerRotation = (innerRotation + 1) % 3600;
        if (level().isClientSide() && tickCount % 128 == 0)
            VoidClient.verifyEntity(this);
    }

    @Override public void aiStep() {
        super.aiStep();
        if (getY() < level().getMinY() - 40)
            setPos(getX(), level().getMinY() - 10, getZ());
        setDeltaMovement(getDeltaMovement().multiply(1, 0, 1));
        setXRot(0);
        setYRot(0);
        yHeadRot = yBodyRot = yHeadRotO = yBodyRotO = xRotO = yRotO = 0;
        float difficulty = getDifficulty();
        Entity target = findNearestBait();
        if (target == null)
            target = level().getNearestPlayer(this, -1);
        double distance = -1;
        if (target != null && hitCooldown == 0 && (!isNetherVoid() || target.getY() > level().getMinY() + 125))
            distance = moveToAttackEntity(target, difficulty);
        if (hitCooldown > 0)
            hitCooldown--;
        // The old pushOutOfBlocks checked for suffocation before choosing the nearest
        // opening. Its modern replacement leaves that check to the caller.
        if (!noPhysics && level().getBlockState(blockPosition()).isCollisionShapeFullBlock(level(), blockPosition()))
            moveTowardsClosestSpace(getX(), getY(), getZ());
        if (!level().isClientSide()) {
            if (isHealing()) {
                entityData.set(HEAL_TIME, entityData.get(HEAL_TIME) - 1);
                heal(0.25F * difficulty);
            } else if (!isGhost() && isAtLessHealth() && random.nextInt(Math.max(1, (int)(80 / difficulty))) == 0) {
                entityData.set(HEAL_TIME, 40);
            }
            // The original trap callback had no active effects; retain movement tracking for hooks.
            motionTracker.update(this);
            if (!isGhost()) {
                eatTorches();
                attractDebris();
            }
        } else if (distance >= 0 && distance < 24) {
            VoidClient.playMonsterSounds(this);
        }
    }

    private Entity findNearestBait() {
        Entity nearest = null;
        double distance = Double.POSITIVE_INFINITY;
        for (Entity entity : level().getEntities(this, centeredBox(24), e -> e instanceof VoidMonsterBait)) {
            VoidMonsterBait bait = (VoidMonsterBait)entity;
            double candidate = distanceToSqr(entity);
            if (!entity.isRemoved() && bait.isActive() && candidate <= bait.maxRangeSquared() && candidate <= distance) {
                nearest = entity;
                distance = candidate;
            }
        }
        return nearest;
    }

    private AABB centeredBox(double radius) {
        return new AABB(getX() - radius, getY() - radius, getZ() - radius,
                getX() + radius, getY() + radius, getZ() + radius);
    }

    private void attractDebris() {
        for (Entity entity : level().getEntities(this, centeredBox(9))) {
            if (entity instanceof VoidMonsterBait || entity instanceof Projectile projectile && projectile.getOwner() != null)
                continue;
            Vec3 offset = entity.position().subtract(position());
            double velocity = entity instanceof LivingEntity ? -0.016 : -0.04;
            entity.setDeltaMovement(entity.getDeltaMovement().add(offset.scale(velocity / Math.max(0.25, offset.length()))).add(0, 0.06, 0));
            entity.noPhysics = false;
            if (!(entity instanceof Player))
                entity.syncVelocity = true;
        }
    }

    private double moveToAttackEntity(Entity target, float difficulty) {
        double distance = moveTowards(target.getX(), target.getY(), target.getZ(), difficulty, false);
        if (level().isClientSide())
            return distance;
        if (target instanceof VoidMonsterBait bait) {
            if (distance <= 15)
                bait.attack(distance <= 2 ? 8 : 1 + (distance - 2) * 7 / 13);
        } else if (target instanceof LivingEntity living) {
            boolean lineOfSight = distance <= 60 && sight.isClearLineOfSight(living);
            boolean looking = lineOfSight && isLookingAt(living);
            if (living instanceof Player player && looking) {
                NeoForge.EVENT_BUS.post(new PlayerLookAtVoidMonsterEvent(player, this));
                if (random.nextInt(50) == 0)
                    VoidCompatibility.addTemporaryWarp(player, 1);
            }
            if (isGhost()) {
                if (looking && ++ghostTick >= 1200)
                    discard();
                if (distance < 4) {
                    setDeltaMovement(Vec3.ZERO);
                    syncVelocity = true;
                }
            } else if (distance <= 6 && lineOfSight && living.isAlive() && !(living instanceof Player player && player.isCreative())) {
                drainHealth(living, difficulty, distance, 6);
            }
        }
        return distance;
    }

    private boolean isLookingAt(LivingEntity entity) {
        double dx = getX() - entity.getX();
        double dy = getY() + getBbHeight() / 2 - entity.getY();
        double dz = getZ() - entity.getZ();
        float yaw = entity.getYHeadRot() % 360;
        if (yaw < 0)
            yaw += 360;
        double relativeYaw = -Math.toDegrees(Math.atan2(dx, dz));
        if (relativeYaw < 0)
            relativeYaw += 360;
        double pitch = 90 - Math.toDegrees(Math.atan2(dy, Math.hypot(dx, dz)));
        return Math.abs(relativeYaw - yaw) < 50 && Math.abs(pitch - entity.getXRot() - 90) < 35;
    }

    public double moveTowards(double x, double y, double z, double velocity) { return moveTowards(x, y, z, velocity, true); }

    private double moveTowards(double x, double y, double z, double velocity, boolean add) {
        Vec3 offset = position().subtract(x, y, z);
        double distance = offset.length();
        if (VoidCompatibility.isDeepDark(level()) && (distance >= 200 || getY() >= level().getMinY() + 2 && getY() < level().getMinY() + 80))
            noPhysics = true;
        else if (isGhost())
            noPhysics = tickCount % 200 < 100 && distanceToSqr(x, y, z) >= 64;
        double horizontal = distance >= 256 ? 12 : distance >= 96 ? Math.min(12, 4 + (distance - 96) / 16)
                : distance >= 16 ? 1.5 + (distance - 16) / 32 : 1.5;
        double factor = -velocity * 0.9375 / (Math.max(1, distance) * 16);
        Vec3 movement = offset.multiply(horizontal * factor, 6 * factor, horizontal * factor);
        setDeltaMovement(add ? getDeltaMovement().add(movement) : movement);
        syncVelocity = true;
        return distance;
    }

    @Override public boolean isClearLineOfSight(LivingEntity entity, RayTracer ray, Level level) {
        for (double dx = -1.5; dx <= 1.5; dx += 0.5)
            for (double dy = -0.5; dy <= 0; dy += 0.5)
                for (double dz = -1.5; dz <= 1.5; dz += 0.5) {
                    ray.setOrigins(getX() + dx, getY() + dy, getZ() + dz,
                            entity.getX(), entity.getY() + entity.getBbHeight() / 2, entity.getZ());
                    if (ray.isClearLineOfSight(level))
                        return true;
                }
        return false;
    }

    private void drainHealth(LivingEntity entity, float difficulty, double distance, double damageDistance) {
        // V33a's fullDist-2 numerator is zero: the drain is constant throughout its
        // six-block range. Express it directly to avoid NaN at the exact boundary.
        float factor = 1;
        float attack = (float)getAttributeValue(Attributes.ATTACK_DAMAGE) * factor / 20;
        attack -= VoidCompatibility.drainLifePoints(entity, attack);
        if (attack > 0) {
            attack -= drainArmor(entity, attack * 0.8F);
            if (attack > 0) {
                if (attack >= entity.getHealth()) {
                    entity.setHealth(0.1F);
                    entity.damageCooldownTime = 0;
                    entity.hurtServer((ServerLevel)level(), new VoidMonsterDamage(this), Integer.MAX_VALUE);
                } else {
                    entity.setHealth(entity.getHealth() - attack);
                }
            }
        }
        if (factor > 0.5)
            entity.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, (int)(100 * factor), 0));
        if (isNetherVoid() && factor > 0.25)
            entity.igniteForSeconds((int)(10 * factor));
        heal(2 * difficulty);
        if (entity instanceof Player player)
            VoidCompatibility.addTemporaryWarp(player, 1);
    }

    private int drainArmor(LivingEntity entity, float attack) {
        int slots = 0;
        float factor = 1;
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.getType() != EquipmentSlot.Type.HUMANOID_ARMOR)
                continue;
            ItemStack stack = entity.getItemBySlot(slot);
            if (!stack.isEmpty() && stack.has(DataComponents.EQUIPPABLE)) {
                factor = VoidCompatibility.modifyArmorDamage(factor, stack);
                slots++;
            }
        }
        if (slots == 0)
            return 0;
        int total = 0;
        int amount = (int)Math.ceil(attack / slots * factor);
        for (EquipmentSlot slot : EquipmentSlot.values())
            if (slot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR)
                total += VoidCompatibility.damageArmor(entity, slot, amount);
        return (int)(total / factor);
    }

    private void eatTorches() {
        int radius = isNetherVoid() ? 4 : 3;
        int vertical = isNetherVoid() ? radius + 2 : radius;
        for (BlockPos pos : BlockPos.betweenClosed(blockPosition().offset(-radius, -vertical, -radius),
                blockPosition().offset(radius, vertical, radius))) {
            if (!level().hasChunkAt(pos))
                continue;
            BlockState state = level().getBlockState(pos);
            if (state.isAir() || !state.getFluidState().isEmpty() || state.hasBlockEntity()
                    || state.getDestroySpeed(level(), pos) < 0 || state.getLightEmission(level(), pos) <= 0)
                continue;
            VoidMonsterEatLightEvent event = new VoidMonsterEatLightEvent(level(), pos.immutable(), state);
            NeoForge.EVENT_BUS.post(event);
            if (state.is(Blocks.TORCH) || state.is(Blocks.WALL_TORCH) || state.is(Blocks.GLOWSTONE) || !event.isCanceled())
                level().destroyBlock(pos, true, this);
        }
    }

    @Override public boolean addEffect(MobEffectInstance effect, Entity source) {
        if (isGhost() && effect.is(MobEffects.REGENERATION))
            doGhostDamage(null, effect.getAmplifier(), 1);
        return false;
    }
    @Override public boolean canBeAffected(MobEffectInstance effect) { return false; }
    public void doGhostDamage(LivingEntity source, int amplifier, double factor) {
        if (level() instanceof ServerLevel server)
            hurtServer(server, new GhostMonsterDamage(this, source), (float)(50 * Math.max(0.25, factor) * Math.pow(3, amplifier)));
    }

    @Override public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        // Administrative removal must bypass gameplay immunities, as it does for
        // vanilla living entities. /kill has no player attacker to pass the cap.
        if (source.is(DamageTypes.GENERIC_KILL)) return super.hurtServer(level, source, damage);
        boolean special = source instanceof GhostMonsterDamage || source instanceof VoidMonsterDamage;
        float cap = getDamageCap(source, damage);
        if (cap <= 0 || getY() < level.getMinY())
            return false;
        if (isHealing()) {
            playSound(SoundEvents.ARROW_HIT, 1, 1);
            return false;
        }
        float net = Math.min(damage, cap);
        Entity attacker = source.getEntity();
        if (!special && attacker != null) {
            float reflect = Math.min(15, (damage - net) / 8 * Math.min(8, (float)Math.pow(1.05, damage - net)));
            attacker.hurtServer(level, damageSources().fellOutOfWorld(), reflect);
        }
        boolean hurt = super.hurtServer(level, source, net);
        if (hurt && getHealth() > 0 && !special && attacker != null) {
            hitCooldown = 50;
            Vec3 offset = position().subtract(attacker.getX(), attacker.getY() + attacker.getEyeHeight(), attacker.getZ());
            setDeltaMovement(offset.scale(-2 / Math.max(1, offset.length())));
            syncVelocity = true;
            playSound(SoundEvents.ENDERMAN_TELEPORT, 1, 1);
        }
        return hurt;
    }

    @Override public float getDamageCap(DamageSource source, float damage) {
        if (source instanceof VoidMonsterDamage)
            return 50;
        float cap = 20;
        if (source instanceof GhostMonsterDamage) {
            cap = 50;
        } else {
            if (isGhost() || hitCooldown > 0 || source.is(DamageTypeTags.IS_FIRE) || source.is(DamageTypes.FALL)
                    || source.is(DamageTypes.FELL_OUT_OF_WORLD) || source.is(DamageTypes.IN_WALL) || source.is(DamageTypes.DROWN)
                    || !(source.getEntity() instanceof Player player))
                return 0;
            cap *= VoidCompatibility.weaponDamageMultiplier(player.getMainHandItem());
        }
        if ((source.is(DamageTypes.MAGIC) || source.is(DamageTypes.INDIRECT_MAGIC)) && damage > 5000)
            cap = 100;
        return cap / getDifficulty();
    }

    @Override public void die(DamageSource source) {
        super.die(source);
        if (level() instanceof ServerLevel server) {
            LightningBolt bolt = EntityTypes.LIGHTNING_BOLT.create(server, net.minecraft.world.entity.EntitySpawnReason.EVENT);
            if (bolt != null) {
                bolt.snapTo(position());
                server.addFreshEntity(bolt);
            }
            MonsterGenerator.instance.addCooldown(this, 20 * (30 + random.nextInt(151)));
        }
    }

    @Override public void handleEntityEvent(byte event) {
        super.handleEntityEvent(event);
        if (event == 3)
            VoidClient.spawnDeathParticles(this);
    }
    @Override protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean playerKilled) {
        if (!isGhost())
            VoidMonsterDrops.doDrops(this);
    }
    @Override protected void dropFromLootTable(ServerLevel level, DamageSource source, boolean playerKilled) {
        if (!isGhost())
            dropFromLootTable(level, source, playerKilled, VoidMonsterDrops.LOOT_TABLE, stack -> {
                net.minecraft.world.entity.item.ItemEntity item = spawnAtLocation(level, stack, 0.25F);
                if (item != null && !stack.is(net.minecraft.world.item.Items.ENCHANTED_BOOK))
                    item.setPermanentlyInvulnerable(true);
            });
    }
    @Override protected void dropExperience(ServerLevel level, Entity killer) {
        if (!isGhost())
            ExperienceOrb.award(level, position(), xpReward);
    }

    /** Complete authored dropFewItems rolls for integrations that grant additional death loot. */
    public void dropAdditionalLoot(ServerLevel level, DamageSource source, int rolls) {
        for (int i = 0; i < rolls; i++) {
            dropFromLootTable(level, source, false);
            dropCustomDeathLoot(level, source, false);
            dropExperience(level, null);
        }
    }

    @Override protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putBoolean("nether", isNetherVoid());
        output.putBoolean("ghost", isGhost());
        output.putBoolean("persist", forcePersist);
        output.putBoolean("isdead", isRemoved() && !forcePersist);
        output.putFloat("difficulty", baseDifficulty);
        output.putInt("difficulty_boosts", difficultyBoosts);
    }
    @Override protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        entityData.set(NETHER, input.getBooleanOr("nether", false));
        entityData.set(GHOST, input.getBooleanOr("ghost", false));
        forcePersist = input.getBooleanOr("persist", false);
        baseDifficulty = input.getFloatOr("difficulty", 1);
        difficultyBoosts = input.getIntOr("difficulty_boosts", 0);
        if (input.getBooleanOr("isdead", false))
            discard();
    }

    public boolean isAtLessHealth() { return getHealth() < getMaxHealth(); }
    public boolean isHealing() { return isAtLessHealth() && entityData.get(HEAL_TIME) > 0; }
    @Override public int getAmbientSoundInterval() {
        int delay = VoidMonster.instance.getMonsterSoundDelay();
        return delay / 2 + random.nextInt(1 + delay / 2);
    }
    @Override public void playAmbientSound() { super.playAmbientSound(); super.playAmbientSound(); }
    @Override protected SoundEvent getAmbientSound() {
        return switch (random.nextInt(9)) {
            case 0 -> SoundEvents.ENDER_DRAGON_GROWL;
            case 1 -> SoundEvents.WITHER_DEATH;
            case 2 -> SoundEvents.ZOMBIFIED_PIGLIN_DEATH;
            case 3 -> SoundEvents.IRON_GOLEM_DEATH;
            case 4 -> SoundEvents.GHAST_DEATH;
            case 5 -> SoundEvents.ZOMBIFIED_PIGLIN_AMBIENT;
            case 6 -> SoundEvents.ZOMBIE_AMBIENT;
            case 7 -> SoundEvents.ZOMBIE_DEATH;
            default -> SoundEvents.WITHER_AMBIENT;
        };
    }
    @Override protected float getSoundVolume() {
        Player player = level().getNearestPlayer(this, 32);
        double distance = player != null ? distanceTo(player) : Double.POSITIVE_INFINITY;
        return distance < 6 ? 2 : distance < 12 ? 1 : 0.5F;
    }
    @Override public float getVoicePitch() { return 0; }
    @Override protected SoundEvent getHurtSound(DamageSource source) { return SoundEvents.ZOMBIE_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.WITHER_DEATH; }
    @Override public void checkDespawn() { /* Upstream explicitly disables automatic despawning, including Peaceful. */ }
    @Override protected void onBelowWorld() { /* The monster lives below the world. */ }
    @Override public boolean canBeCollidedWith(Entity other) { return !isGhost(); }
    @Override public boolean isPickable() { return !isGhost() && !isRemoved(); }
    @Override public boolean canCollideWith(Entity other) { return false; } // V33a getCollisionBox returned null.
    @Override public boolean isPushable() { return false; }
    @Override public boolean isPushedByFluid() { return false; }
    @Override public boolean displayFireAnimation() { return false; }
    @Override public void setRemainingFireTicks(int ticks) { super.setRemainingFireTicks(Math.min(0, ticks)); }
    @Override public boolean shouldRenderAtSqrDistance(double distance) { return true; }
    @Override public boolean doHurtTarget(ServerLevel level, Entity entity) { return false; }
    @Override public void remove(RemovalReason reason) {
        if (!forcePersist || getHealth() <= 0 || reason == RemovalReason.UNLOADED_WITH_PLAYER || reason == RemovalReason.UNLOADED_TO_CHUNK)
            super.remove(reason);
    }
    @Override public void destroy() { if (!forcePersist) discard(); }
    protected final void damageFromVoidMetalAmmo(ServerLevel server) {
        if (!isHealing()) super.hurtServer(server, damageSources().generic(), getMaxHealth() / 4);
    }
}
