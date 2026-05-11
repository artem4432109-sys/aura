package rich.screens.hud;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import rich.client.draggables.AbstractHudElement;
import rich.modules.impl.combat.Aura;
import rich.util.ColorUtil;
import rich.util.network.Network;
import rich.util.render.Render2D;
import rich.util.render.font.Fonts;
import rich.util.timer.StopWatch;

import java.awt.*;

public class TargetHud extends AbstractHudElement {

    private final StopWatch stopWatch = new StopWatch();
    private LivingEntity lastTarget;

    private float healthAnimation = 0;
    private float trailAnimation = 0;
    private float absorptionAnimation = 0;
    private float displayedHealth = 0;
    private long lastUpdateTime = System.currentTimeMillis();
    private long startTime = System.currentTimeMillis();

    public TargetHud() {
        super("TargetHud", 10, 80, 150, 68, true);
    }

    @Override
    public boolean visible() {
        return true;
    }

    @Override
    public void tick() {
        LivingEntity auraTarget = Aura.target;
        if (auraTarget != null) {
            lastTarget = auraTarget;
            startAnimation();
            stopWatch.reset();
        } else if (isChat(mc.currentScreen)) {
            lastTarget = mc.player;
            startAnimation();
            stopWatch.reset();
        } else if (stopWatch.finished(10)) {
            stopAnimation();
        }
    }

    private float lerp(float current, float target, float deltaTime, float speed) {
        float factor = (float) (1.0 - Math.pow(0.001, deltaTime * speed));
        return current + (target - current) * factor;
    }

    private float snapToStep(float value, float step) {
        return Math.round(value / step) * step;
    }

    private float getHealth(LivingEntity entity) {
        if (entity.isInvisible() && !Network.isSpookyTime() && !Network.isCopyTime()) {
            return entity.getMaxHealth();
        }
        return entity.getHealth();
    }

    private String getHealthString(float health) {
        if (lastTarget != null && lastTarget.isInvisible() && !Network.isSpookyTime() && !Network.isCopyTime()) {
            return "??";
        }
        if (health >= 100) {
            return String.valueOf((int) health);
        } else if (health >= 10) {
            return String.format("%.1f", health);
        } else {
            return String.format("%.2f", health);
        }
    }

    @Override
    public void drawDraggable(DrawContext context, int alpha) {
        if (alpha <= 0) return;
        if (lastTarget == null) return;

        long currentTime = System.currentTimeMillis();
        float deltaTime = (currentTime - lastUpdateTime) / 1000.0f;
        lastUpdateTime = currentTime;
        deltaTime = Math.min(deltaTime, 0.1f);

        float x = getX();
        float y = getY();

        setWidth(150);
        setHeight(68);

        float scaleAlpha = scaleAnimation.getOutput().floatValue();

        drawBackground(x, y, scaleAlpha);
        drawEntityModel(context, x, y, scaleAlpha);
        drawContent(x, y, scaleAlpha, deltaTime);
        drawItems(context, x, y, scaleAlpha);
    }

    private void drawBackground(float x, float y, float alpha) {
        int alphaInt = (int) (255 * alpha);

        Render2D.gradientRect(x + 2, y + 2, getWidth() - 4, getHeight() - 4,
                new int[]{
                        new Color(52, 52, 52, alphaInt).getRGB(),
                        new Color(22, 22, 22, alphaInt).getRGB(),
                        new Color(52, 52, 52, alphaInt).getRGB(),
                        new Color(22, 22, 22, alphaInt).getRGB()
                },
                6);

        Render2D.outline(x + 2, y + 2, getWidth() - 4, getHeight() - 4, 0.35f, new Color(90, 90, 90, alphaInt).getRGB(), 5);

        int blurTint = ColorUtil.rgba(0, 0, 0, 0);
        Render2D.blur(x + 2, y + 2, 1, 1, 0f, 7, blurTint);
    }

    private void drawEntityModel(DrawContext context, float x, float y, float alpha) {
        if (lastTarget == null) return;

        int modelX1 = (int) (x + 5);
        int modelY1 = (int) (y + 3);
        int modelX2 = (int) (x + 42);
        int modelY2 = (int) (y + getHeight() - 3);

        int entitySize = 24;

        float centerX = (modelX1 + modelX2) / 2f;

        context.enableScissor(modelX1, modelY1, modelX2, modelY2);

        InventoryScreen.drawEntity(
                context,
                modelX1, modelY1, modelX2, modelY2,
                entitySize,
                0.0625f,
                centerX + 20,
                (float) modelY1,
                lastTarget
        );

        context.disableScissor();
    }

    private void drawItems(DrawContext context, float x, float y, float alpha) {
        if (lastTarget == null) return;

        ItemStack mainHand = lastTarget.getMainHandStack();
        ItemStack offHand = lastTarget.getOffHandStack();
        ItemStack helmet = lastTarget.getEquippedStack(EquipmentSlot.HEAD);
        ItemStack chestplate = lastTarget.getEquippedStack(EquipmentSlot.CHEST);
        ItemStack leggings = lastTarget.getEquippedStack(EquipmentSlot.LEGS);
        ItemStack boots = lastTarget.getEquippedStack(EquipmentSlot.FEET);

        ItemStack[] items = { mainHand, offHand, helmet, chestplate, leggings, boots };

        float itemScale = 0.625f;
        int itemSize = (int) (16 * itemScale);
        int gap = 2;
        float startX = x + 46;
        float itemY = y + getHeight() - itemSize - 5;

        context.getMatrices().push();
        context.getMatrices().translate(startX, itemY, 0);
        context.getMatrices().scale(itemScale, itemScale, 1f);

        int drawX = 0;
        for (ItemStack stack : items) {
            if (stack != null && !stack.isEmpty()) {
                context.drawItem(stack, drawX, 0);
            }
            drawX += 16 + (int) (gap / itemScale);
        }

        context.getMatrices().pop();
    }

    private void drawContent(float x, float y, float alpha, float deltaTime) {
        float contentX = x + 46;
        float nameY = y + 10;

        float hp = getHealth(lastTarget);
        float maxHp = lastTarget.getMaxHealth();
        float absorp = lastTarget.getAbsorptionAmount();

        boolean isInvisible = lastTarget.isInvisible() && !Network.isSpookyTime() && !Network.isCopyTime();

        float targetDisplayHealth;
        if (isInvisible) {
            targetDisplayHealth = maxHp;
        } else {
            targetDisplayHealth = hp + absorp;
        }
        displayedHealth = lerp(displayedHealth, targetDisplayHealth, deltaTime, 5f);
        float snappedHealth = snapToStep(displayedHealth, 0.25f);

        String hpStr = getHealthString(snappedHealth);
        String name = lastTarget.getName().getString();

        Fonts.BOLD.draw(name, contentX, nameY, 5.5f,
                new Color(255, 255, 255, (int) (255 * alpha)).getRGB());

        float infoY = nameY + 10;
        String hpInfo = "HP: " + hpStr;

        float dist = 0;
        if (mc.player != null && lastTarget != mc.player) {
            dist = mc.player.distanceTo(lastTarget);
        }
        String distInfo = "Dist: " + String.format("%.0f", dist);
        String infoText = hpInfo + " | " + distInfo;

        Fonts.BOLD.draw(infoText, contentX, infoY, 5f,
                new Color(215, 215, 215, (int) (255 * alpha)).getRGB());

        float targetHealth;
        if (isInvisible) {
            targetHealth = 1.0f;
        } else {
            targetHealth = hp / maxHp;
        }
        healthAnimation = lerp(healthAnimation, targetHealth, deltaTime, 3f);

        if (targetHealth > trailAnimation) {
            trailAnimation = targetHealth;
        }
        trailAnimation = lerp(trailAnimation, targetHealth, deltaTime, 3.5f);

        float targetAbsorption;
        if (isInvisible) {
            targetAbsorption = 0;
        } else {
            targetAbsorption = absorp / maxHp;
        }
        absorptionAnimation = lerp(absorptionAnimation, targetAbsorption, deltaTime, 3f);

        float barX = contentX;
        float barY = infoY + 12f;
        float barWidth = getWidth() - (contentX - x) - 10;
        float barHeight = 4;
        float barRadius = 2;

        Render2D.rect(barX, barY, barWidth, barHeight,
                new Color(30, 30, 30, (int) (200 * alpha)).getRGB(), barRadius);

        float healthPercent = Math.max(0, Math.min(1, healthAnimation));
        float trailPercent = Math.max(0, Math.min(1, trailAnimation));

        if (trailPercent > healthPercent) {
            int trailColor = new Color(55, 55, 55, (int) (160 * alpha)).getRGB();
            Render2D.rect(barX, barY, barWidth * trailPercent, barHeight, trailColor, barRadius);
        }

        if (healthPercent > 0.01f) {
            long elapsed = System.currentTimeMillis() - startTime;
            float waveSpeed = 1500f;
            float wavePhase = (elapsed % (long) waveSpeed) / waveSpeed * (float) Math.PI * 2f;

            int[] colors = new int[4];
            for (int i = 0; i < 2; i++) {
                float charWave = (float) Math.sin(wavePhase - i * 1.5f);
                float waveFactor = (charWave + 1f) / 2f;

                int baseGray = (int) (155 + 100 * waveFactor);

                colors[i * 2] = new Color(baseGray, baseGray, baseGray, (int) (255 * alpha)).getRGB();
                colors[i * 2 + 1] = new Color(baseGray, baseGray, baseGray, (int) (255 * alpha)).getRGB();
            }

            Render2D.gradientRect(barX, barY, barWidth * healthPercent, barHeight, colors, barRadius);
        }

        float absorptionPercent = Math.max(0, Math.min(1, absorptionAnimation));
        if (absorptionPercent > 0.01f && !Network.isFunTime()) {
            long elapsed = System.currentTimeMillis() - startTime;
            float waveSpeed = 1200f;
            float wavePhase = (elapsed % (long) waveSpeed) / waveSpeed * (float) Math.PI * 2f;

            int[] goldColors = new int[4];
            for (int i = 0; i < 2; i++) {
                float charWave = (float) Math.sin(wavePhase - i * 1.5f);
                float waveFactor = (charWave + 1f) / 2f;

                int cr = 255;
                int cg = (int) (165 + 50 * waveFactor);
                int cb = 0;

                goldColors[i * 2] = new Color(cr, cg, cb, (int) (200 * alpha)).getRGB();
                goldColors[i * 2 + 1] = new Color(cr, cg, cb, (int) (200 * alpha)).getRGB();
            }

            Render2D.gradientRect(barX, barY, barWidth * absorptionPercent, barHeight, goldColors, barRadius);
        }
    }
}
