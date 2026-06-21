package com.yixi_xun.more_potion_effects.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@OnlyIn(value = Dist.CLIENT, _interface = ItemSupplier.class)
public class HomingArrowEntity extends Arrow implements ItemSupplier {
    public static final ItemStack PROJECTILE_ITEM = new ItemStack(Items.ARROW);
    private static final EntityDataAccessor<Integer> TARGET_ID = SynchedEntityData.defineId(HomingArrowEntity.class, EntityDataSerializers.INT);

    private static final double SEEK_RANGE = 32.0;
    private static final float SEEK_ANGLE = (float) (Math.PI / 3);
    private static final double SEEK_THRESHOLD = Math.cos(SEEK_ANGLE / 2);
    private double SEEK_FACTOR = 0.1;

    public HomingArrowEntity(EntityType<? extends HomingArrowEntity> type, Level level) {
        super(type, level);
    }

    public HomingArrowEntity(Level level, LivingEntity shooter, double homingLevel) {
        super(com.yixi_xun.more_potion_effects.init.MorePotionEffectsModEntities.HOMING_ARROW.get(), level);
        this.setOwner(shooter);
        this.setPos(shooter.getX(), shooter.getEyeY() - 0.1, shooter.getZ());
        this.SEEK_FACTOR = 0.1 + 0.025 * homingLevel;
    }

    public void setSeekFactor(double factor) {
        SEEK_FACTOR = factor;
    }

    public double getSeekFactor() {
        return SEEK_FACTOR;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(TARGET_ID, -1);
    }

    @Nullable
    public Entity getTarget() {
        int targetId = this.entityData.get(TARGET_ID);
        return targetId == -1 ? null : this.level().getEntity(targetId);
    }

    private void setTarget(@Nullable Entity target) {
        this.entityData.set(TARGET_ID, target == null ? -1 : target.getId());
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public @NotNull ItemStack getItem() {
        return PROJECTILE_ITEM;
    }

    @Override
    protected @NotNull ItemStack getPickupItem() {
        return PROJECTILE_ITEM;
    }

    @Override
    public void tick() {
        if (isThisArrowFlying()) {
            updateTarget();
            adjustCourse();

            if (this.level().isClientSide() && !this.onGround()) {
                for (int i = 0; i < 4; ++i) {
                    this.level().addParticle(ParticleTypes.WITCH,
                            this.getX() + this.getDeltaMovement().x() * i / 4.0D,
                            this.getY() + this.getDeltaMovement().y() * i / 4.0D,
                            this.getZ() + this.getDeltaMovement().z() * i / 4.0D,
                            -this.getDeltaMovement().x(), -this.getDeltaMovement().y() + 0.2D, -this.getDeltaMovement().z());
                }
            }
        } else {
            setTarget(null);
        }
        super.tick();
    }

    private boolean isThisArrowFlying() {
        return !this.inGround && this.getDeltaMovement().lengthSqr() > 0.1;
    }

    private void updateTarget() {
        Entity target = getTarget();
        if (target != null && !target.isAlive()) {
            target = null;
            this.setTarget(null);
        }

        if (target == null) {
            if (this.getOwner() instanceof Monster owner && owner.getTarget() != null) {
                setTarget(owner.getTarget());
                return;
            }

            AABB positionBB = new AABB(getX(), getY(), getZ(), getX(), getY(), getZ());
            AABB targetBB = positionBB;

            Vec3 courseVec = getDeltaMovement().scale(SEEK_RANGE).yRot(SEEK_ANGLE);
            targetBB = targetBB.minmax(positionBB.move(courseVec));
            courseVec = getDeltaMovement().scale(SEEK_RANGE).yRot(-SEEK_ANGLE);
            targetBB = targetBB.minmax(positionBB.move(courseVec));
            targetBB = targetBB.inflate(0, SEEK_RANGE * 0.5, 0);

            double closestDot = -1.0;
            Entity closestTarget = null;

            List<LivingEntity> entityList = this.level().getEntitiesOfClass(LivingEntity.class, targetBB);
            List<LivingEntity> monsters = entityList.stream().filter(l -> l instanceof Monster).toList();

            if (!monsters.isEmpty()) {
                for (LivingEntity monster : monsters) {
                    if (((Monster) monster).getTarget() == this.getOwner()) {
                        setTarget(monster);
                        return;
                    }
                }
                for (LivingEntity monster : monsters) {
                    if (monster instanceof NeutralMob) continue;
                    if (monster.hasLineOfSight(this)) {
                        setTarget(monster);
                        return;
                    }
                }
            }

            for (LivingEntity living : entityList) {
                if (!living.hasLineOfSight(this)) continue;
                if (living == this.getOwner()) continue;
                if (getOwner() != null && living instanceof TamableAnimal animal && animal.getOwner() == this.getOwner())
                    continue;

                Vec3 motionVec = getDeltaMovement().normalize();
                Vec3 targetVec = living.getEyePosition().subtract(this.position()).normalize();
                double dot = motionVec.dot(targetVec);

                if (dot > Math.max(closestDot, SEEK_THRESHOLD)) {
                    closestDot = dot;
                    closestTarget = living;
                }
            }

            if (closestTarget != null) {
                setTarget(closestTarget);
            }
        }
    }

    private void adjustCourse() {
        if (isThisArrowFlying()) {
            if (!this.level().isClientSide()) {
                this.updateTarget();
            }

            Entity target = getTarget();
            if (target != null) {
                Vec3 targetVec = target.getEyePosition().subtract(this.position()).scale(SEEK_FACTOR);
                Vec3 courseVec = getDeltaMovement();

                double courseLen = courseVec.length();
                double targetLen = targetVec.length();
                double totalLen = Math.sqrt(courseLen * courseLen + targetLen * targetLen);
                double dotProduct = courseVec.dot(targetVec) / (courseLen * targetLen);

                if (dotProduct > SEEK_THRESHOLD) {
                    Vec3 newMotion = courseVec.scale(courseLen / totalLen)
                            .add(targetVec.scale(courseLen / totalLen));
                    this.setDeltaMovement(newMotion.add(0, 0.045F, 0));
                } else if (!this.level().isClientSide()) {
                    this.setTarget(null);
                }
            }
        }

        super.tick();
    }
}