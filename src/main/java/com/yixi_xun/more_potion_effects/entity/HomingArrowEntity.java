package com.yixi_xun.more_potion_effects.entity;

import com.yixi_xun.more_potion_effects.init.MorePotionEffectsModEntities;
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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class HomingArrowEntity extends Arrow implements ItemSupplier {
    public static final ItemStack PROJECTILE_ITEM = new ItemStack(Items.ARROW);
    private static final EntityDataAccessor<Integer> TARGET_ID = SynchedEntityData.defineId(HomingArrowEntity.class, EntityDataSerializers.INT);

    // --- 可配置的追踪参数 ---
    private static final double SEEK_RANGE = 32.0;
    private static final float SEEK_ANGLE = (float) (Math.PI / 3);
    private static final double SEEK_THRESHOLD = Math.cos(SEEK_ANGLE / 2);
    private double SEEK_FACTOR = 0.1;

    public HomingArrowEntity(EntityType<? extends HomingArrowEntity> type, Level level) {
        super(type, level);
    }

    public HomingArrowEntity(Level level, LivingEntity shooter, double homingLevel) {
        this(MorePotionEffectsModEntities.HOMING_ARROW.get(), level);
        this.setOwner(shooter);
        this.SEEK_FACTOR = 0.1 + 0.025 * homingLevel;
    }

    @Override
    protected void defineSynchedData(@NotNull SynchedEntityData.Builder builder) {
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

    /**
     * 目标选择逻辑
     */
    private void updateTarget() {
        // 检查当前目标是否有效（存活）
        Entity target = getTarget();
        if (target != null && !target.isAlive()) {
            target = null;
            this.setTarget(null);
        }

        // 若目标为空，重新检测
        if (target == null) {
            // 优先目标1：箭矢所有者的攻击目标
            if (this.getOwner() instanceof Monster owner && owner.getTarget() != null) {
                setTarget(owner.getTarget());
                return;
            }

            // 构建扇形检测区域：以箭矢当前位置为中心，沿飞行方向向两侧各偏折 seekAngle（30度）
            AABB positionBB = new AABB(getX(), getY(), getZ(), getX(), getY(), getZ());
            AABB targetBB = positionBB;

            // 计算飞行方向向量向两侧偏折后的区域
            Vec3 courseVec = getDeltaMovement().scale(SEEK_RANGE).yRot(SEEK_ANGLE); // 右侧偏折
            targetBB = targetBB.minmax(positionBB.move(courseVec));
            courseVec = getDeltaMovement().scale(SEEK_RANGE).yRot(- SEEK_ANGLE); // 左侧偏折
            targetBB = targetBB.minmax(positionBB.move(courseVec));
            targetBB = targetBB.inflate(0, SEEK_RANGE * 0.5, 0); // 垂直方向扩展

            double closestDot = -1.0;
            Entity closestTarget = null;

            // 获取区域内所有生物
            List<LivingEntity> entityList = this.level().getEntitiesOfClass(LivingEntity.class, targetBB);
            List<LivingEntity> monsters = entityList.stream().filter(l -> l instanceof Monster).toList();

            // 优先目标2：攻击箭矢所有者的怪物
            if (!monsters.isEmpty()) {
                for (LivingEntity monster : monsters) {
                    if (((Monster) monster).getTarget() == this.getOwner()) {
                        setTarget(monster);
                        return;
                    }
                }
                // 优先目标3：视野内的非中立怪物
                for (LivingEntity monster : monsters) {
                    if (monster instanceof NeutralMob) continue; // 排除中立生物
                    if (monster.hasLineOfSight(this)) {
                        setTarget(monster);
                        return;
                    }
                }
            }

            // 其他目标：计算与飞行方向的夹角，选择夹角最小的目标
            for (LivingEntity living : entityList) {
                if (!living.hasLineOfSight(this)) continue; // 无视线则跳过
                if (living == this.getOwner()) continue; // 排除所有者
                if (getOwner() != null && living instanceof TamableAnimal animal && animal.getOwner() == this.getOwner())
                    continue; // 排除所有者的驯服生物

                // 计算飞行方向与目标方向的向量点积（余弦相似度，值越大夹角越小）
                Vec3 motionVec = getDeltaMovement().normalize();
                Vec3 targetVec = living.getEyePosition().subtract(this.position()).normalize();
                double dot = motionVec.dot(targetVec);

                // 筛选点积最大（夹角最小）且在有效角度内的目标
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
    }
}