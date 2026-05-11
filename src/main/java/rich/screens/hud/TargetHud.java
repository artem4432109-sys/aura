package rich.screens.hud;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Identifier;
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
    private long lastUpdateTime = System.currentTimeMillis();

    public TargetHud() {
        super("TargetHud", 10, 80, 130, 32, true);
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

    private float getHealth(LivingEntity entity) {
        if (entity.isInvisible() && !Network.isSpookyTime() && !Network.isCopyTime()) {
            return entity.getMaxHealth();
        }
        return entity.getHealth();
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

        setWidth(130);
        setHeight(32);

        float scaleAlpha = scaleAnimation.getOutput().floatValue();

        drawBackground(x, y, scaleAlpha);
        drawFace(x, y, scaleAlpha);
        drawContent(x, y, scaleAlpha, deltaTime);
    }

    private void drawBackground(float x, float y, float alpha) {
        int alphaInt = (int) (255 * alpha);

        Render2D.gradientRect(x + 2, y + 2, getWidth() - 4, getHeight() - 4,
                new int[]{
                        new Color(40, 40, 40, alphaInt).getRGB(),
                        new Color(20, 20, 20, alphaInt).getRGB(),
                        new Color(40, 40, 40, alphaInt).getRGB(),
                        new Color(20, 20, 20, alphaInt).getRGB()
                },
                4);

        Render2D.outline(x + 2, y + 2, getWidth() - 4, getHeight() - 4, 0.3f, new Color(80, 80, 80, alphaInt).getRGB(), 4);

        int blurTint = ColorUtil.rgba(0, 0, 0, 0);
        Render2D.blur(x + 2, y + 2, 1, 1, 0f, 7, blurTint);
    }

    private void drawFace(float x, float y, float alpha) {
        EntityRenderer<? super LivingEntity, ?> baseRenderer = mc.getEntityRenderDispatcher().getRenderer(lastTarget);
        if (!(baseRenderer instanceof LivingEntityRenderer<?, ?, ?>)) {
            return;
        }

        @SuppressWarnings("unchecked")
        LivingEntityRenderer<LivingEntity, LivingEntityRenderState, ?> renderer =
                (LivingEntityRenderer<LivingEntity, LivingEntityRenderState, ?>) baseRenderer;

        LivingEntityRenderState state = renderer.getAndUpdateRenderState(lastTarget, lastTickDelta);
        Identifier textureLocation = renderer.getTexture(state);

        float faceSize = 18;
        float faceX = x + 6;
        float faceY = y + 7;

        float hurtPercent = lastTarget.hurtTime > 0 ? lastTarget.hurtTime / 10.0f : 0.0f;
        int r = 255;
        int g = (int) (255 * (1.0f - hurtPercent));
        int b = (int) (255 * (1.0f - hurtPercent));
        int color = new Color(r, g, b, (int) (255 * alpha)).getRGB();

        float u0 = 8f / 64f;
        float v0 = 8f / 64f;
        float u1 = 16f / 64f;
        float v1 = 16f / 64f;

        Render2D.texture(textureLocation, faceX, faceY, faceSize, faceSize,
                u0, v0, u1, v1, color, 0, 2f);

        float hatScale = 1.1f;
        float hatSize = faceSize * hatScale;
        float hatOffset = (hatSize - faceSize) / 2f;

        float hatU0 = 40f / 64f;
        float hatV0 = 8f / 64f;
        float hatU1 = 48f / 64f;
        float hatV1 = 16f / 64f;

        Render2D.texture(textureLocation, faceX - hatOffset, faceY - hatOffset, hatSize, hatSize,
                hatU0, hatV0, hatU1, hatV1, color, 0f, 2f);
    }

    private void drawContent(float x, float y, float alpha, float deltaTime) {
        float contentX = x + 28;
        float nameY = y + 6;

        float hp = getHealth(lastTarget);
        float maxHp = lastTarget.getMaxHealth();
        float absorp = lastTarget.getAbsorptionAmount();

        boolean isInvisible = lastTarget.isInvisible() && !Network.isSpookyTime() && !Network.isCopyTime();

        String name = lastTarget.getName().getString();
        Fonts.BOLD.draw(name, contentX, nameY, 5.5f,
                new Color(255, 255, 255, (int) (255 * alpha)).getRGB());

        float total = hp + absorp;
        String hpText;
        if (isInvisible) {
            hpText = "HP: ??";
        } else if (absorp > 0.01f) {
            hpText = "HP: " + String.format("%.1f", hp) + " (" + String.format("%.1f", total) + ")";
        } else {
            hpText = "HP: " + String.format("%.1f", hp);
        }

        float hpY = nameY + 10;
        Fonts.BOLD.draw(hpText, contentX, hpY, 5f,
                new Color(215, 215, 215, (int) (255 * alpha)).getRGB());

        float targetHealth;
        if (isInvisible) {
            targetHealth = 1.0f;
        } else {
            targetHealth = hp / maxHp;
        }
        healthAnimation = lerp(healthAnimation, targetHealth, deltaTime, 3f);

        float barX = contentX;
        float barY = hpY + 9f;
        float barWidth = getWidth() - (contentX - x) - 8;
        float barHeight = 3;
        float barRadius = 1.5f;

        Render2D.rect(barX, barY, barWidth, barHeight,
                new Color(30, 30, 30, (int) (200 * alpha)).getRGB(), barRadius);

        float healthPercent = Math.max(0, Math.min(1, healthAnimation));

        if (healthPercent > 0.01f) {
            int barColor = new Color(0, 150, 255, (int) (255 * alpha)).getRGB();
            Render2D.rect(barX, barY, barWidth * healthPercent, barHeight, barColor, barRadius);
        }
    }
}
