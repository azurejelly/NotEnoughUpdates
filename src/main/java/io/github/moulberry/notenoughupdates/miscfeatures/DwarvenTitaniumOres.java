/*
 * Copyright (C) 2024 NotEnoughUpdates contributors
 *
 * This file is part of NotEnoughUpdates.
 *
 * NotEnoughUpdates is free software: you can redistribute it
 * and/or modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation, either
 * version 3 of the License, or (at your option) any later version.
 *
 * NotEnoughUpdates is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with NotEnoughUpdates. If not, see <https://www.gnu.org/licenses/>.
 */

package io.github.moulberry.notenoughupdates.miscfeatures;

import io.github.moulberry.notenoughupdates.NotEnoughUpdates;
import io.github.moulberry.notenoughupdates.overlays.MiningOverlay;
import io.github.moulberry.notenoughupdates.util.SBInfo;
import io.github.moulberry.notenoughupdates.util.SpecialColour;
import io.github.moulberry.notenoughupdates.util.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.shader.Framebuffer;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL30;

import java.util.HashMap;
import java.util.Map;

public class DwarvenTitaniumOres {

	private static Framebuffer framebufferBlocksTo = null;
	private static Framebuffer framebufferBlocksFrom = null;

	public static boolean textureExists() {
		return framebufferBlocksFrom != null && isOverriding();
	}

	public static void bindTextureIfExists() {
		if (textureExists()) {
			framebufferBlocksFrom.bindFramebufferTexture();
		}
	}

	public static boolean isOverriding() {
		if (!OpenGlHelper.isFramebufferEnabled()) {
			return false;
		}

		if (!NotEnoughUpdates.INSTANCE.config.world.highlightTitaniumOres) {
			return false;
		}

		if (NotEnoughUpdates.INSTANCE.config.world.highlightTitaniumOnlyWhenRequiredByCommissions) {
			boolean hasCommissions = false;

			for (Map.Entry<String, Float> entry : MiningOverlay.commissionProgress.entrySet()) {
				String key = entry.getKey();

				if (key.contains("Titanium") && entry.getValue() != 1f) {
					if (!key.contains(SBInfo.getInstance().getScoreboardLocation()) && !key.contains("Miner")) {
						continue;
					}

					hasCommissions = true;
					break;
				}
			}

			if (!hasCommissions) {
				return false;
			}
		}

		return SBInfo.getInstance().getLocation() != null && "mining_3".equals(SBInfo.getInstance().getLocation());
	}

	public static void tick() {
		if (!isOverriding() || Minecraft.getMinecraft().theWorld == null) {
			return;
		}

		Minecraft.getMinecraft().getTextureManager().bindTexture(TextureMap.locationBlocksTexture);
		int w = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_WIDTH);
		int h = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_HEIGHT);

		Framebuffer to = checkFramebufferSizes(framebufferBlocksTo, w, h);

		try {
			GL11.glPushMatrix();

			GlStateManager.matrixMode(5889);
			GlStateManager.loadIdentity();
			GlStateManager.ortho(0.0D, w, h, 0.0D, 1000.0D, 3000.0D);
			GlStateManager.matrixMode(5888);
			GlStateManager.loadIdentity();
			GlStateManager.translate(0.0F, 0.0F, -2000.0F);

			to.bindFramebuffer(true);
			GlStateManager.clear(GL11.GL_COLOR_BUFFER_BIT);

			GlStateManager.disableBlend();
			GlStateManager.disableLighting();
			GlStateManager.disableFog();

			Minecraft.getMinecraft().getTextureManager().bindTexture(TextureMap.locationBlocksTexture);
			GlStateManager.color(1, 1, 1, 1);
			Utils.drawTexturedRectNoBlend(0, 0, w, h, 0, 1, 1, 0, GL11.GL_LINEAR);

			HashMap<TextureAtlasSprite, Integer> spriteMap = new HashMap<TextureAtlasSprite, Integer>() {{
				put(
					Minecraft.getMinecraft().getTextureMapBlocks().getAtlasSprite("minecraft:blocks/stone_diorite_smooth"),
					SpecialColour.specialToChromaRGB(NotEnoughUpdates.INSTANCE.config.world.titaniumOresColor2)
				);
			}};

			for (Map.Entry<TextureAtlasSprite, Integer> entry : spriteMap.entrySet()) {
				if (((entry.getValue() >> 24) & 0xFF) < 10) continue;

				TextureAtlasSprite tas = entry.getKey();
				Gui.drawRect((int) (w * tas.getMinU()), h - (int) (h * tas.getMaxV()) - 1,
					(int) (w * tas.getMaxU()) + 1, h - (int) (h * tas.getMinV()), entry.getValue()
				);
			}

			ScaledResolution scaledResolution = new ScaledResolution(Minecraft.getMinecraft());
			GlStateManager.matrixMode(5889);
			GlStateManager.loadIdentity();
			GlStateManager.ortho(0.0D, scaledResolution.getScaledWidth_double(), scaledResolution.getScaledHeight_double(),
				0.0D, 1000.0D, 3000.0D
			);
			GlStateManager.matrixMode(5888);
			GlStateManager.loadIdentity();
			GlStateManager.translate(0.0F, 0.0F, -2000.0F);

			GL11.glPopMatrix();

			to.bindFramebufferTexture();
			if (Minecraft.getMinecraft().gameSettings.mipmapLevels >= 0) {
				GL11.glTexParameteri(
					GL11.GL_TEXTURE_2D,
					GL12.GL_TEXTURE_MAX_LEVEL,
					Minecraft.getMinecraft().gameSettings.mipmapLevels
				);
				GL11.glTexParameterf(GL11.GL_TEXTURE_2D, GL12.GL_TEXTURE_MIN_LOD, 0.0F);
				GL11.glTexParameterf(
					GL11.GL_TEXTURE_2D,
					GL12.GL_TEXTURE_MAX_LOD,
					(float) Minecraft.getMinecraft().gameSettings.mipmapLevels
				);
				GL11.glTexParameterf(GL11.GL_TEXTURE_2D, GL14.GL_TEXTURE_LOD_BIAS, 0.0F);
				GL30.glGenerateMipmap(GL11.GL_TEXTURE_2D);
			}

			Framebuffer from = checkFramebufferSizes(framebufferBlocksFrom, w, h);
			framebufferBlocksFrom = to;
			framebufferBlocksTo = from;
		} catch (Exception e) {
			e.printStackTrace();
		}
		Minecraft.getMinecraft().getFramebuffer().bindFramebuffer(true);
		GlStateManager.enableBlend();
	}

	private static Framebuffer checkFramebufferSizes(Framebuffer framebuffer, int width, int height) {
		if (framebuffer == null || framebuffer.framebufferWidth != width || framebuffer.framebufferHeight != height) {
			if (framebuffer == null) {
				framebuffer = new Framebuffer(width, height, false);
				framebuffer.framebufferColor[0] = 1f;
				framebuffer.framebufferColor[1] = 0f;
				framebuffer.framebufferColor[2] = 0f;
				framebuffer.framebufferColor[3] = 0;
			} else {
				framebuffer.createBindFramebuffer(width, height);
			}

			framebuffer.setFramebufferFilter(GL11.GL_NEAREST);
		}
		return framebuffer;
	}
}
