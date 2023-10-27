/*
 * ArmorStandEditor: Bukkit plugin to allow editing armor stand attributes
 * Copyright (C) 2016-2023  RypoFalem
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package io.github.rypofalem.armorstandeditor.modes;

public enum DisplayEditMode {
    NONE("None"),
    COPY("Copy"),
    PASTE("Paste"),
    RESET("Reset"),
    DELETE("DeleteDisplayEntity"),

    PLACEMENT("Placement"),
    SCALE("Scale"),
    LEFT_ROTATE("LeftRotate"),
    RIGHT_ROTATE("RightRotate"),

    YAW("Yaw"),
    PITCH("Pitch"),

    GLOWING("Glowing"),
    GLOW_COLOR("GlowColor"),
    BILLBOARD("Billboard"),
    BLOCK_LIGHT("BlockLight"),
    SKY_LIGHT("SkyLight"),
    SHADOW_RADIUS("ShadowRadius"),
    SHADOW_STRENGTH("ShadowStrength"),

    EQUIPMENT("Equipment"),
    TEXT("Text");

    private String name;

    DisplayEditMode(String name) {
        this.name = name;
    }

    public String toString() {
        return name;
    }
}
