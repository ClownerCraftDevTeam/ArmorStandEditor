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

package io.github.rypofalem.armorstandeditor;

import org.bukkit.util.EulerAngle;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public abstract class Util {

    public static final double FULL_CIRCLE = Math.PI * 2;

    public static <T extends Enum<?>> String getEnumList(Class<T> enumType) {
        return getEnumList(enumType, " | ");
    }

    public static <T extends Enum<?>> String getEnumList(Class<T> enumType, String delimiter) {
        StringBuilder list = new StringBuilder();
        boolean put = false;
        for (Enum<?> e : enumType.getEnumConstants()) {
            list.append(e.toString()).append(delimiter);
            put = true;
        }
        if (put) list = new StringBuilder(list.substring(0, list.length() - delimiter.length()));
        return list.toString();
    }

    public static double addAngle(double current, double angleChange) {
        current += angleChange;
        current = fixAngle(current, angleChange);
        return current;
    }

    public static double subAngle(double current, double angleChange) {
        current -= angleChange;
        current = fixAngle(current, angleChange);
        return current;
    }

    //clamps angle to 0 if it exceeds 2PI rad (360 degrees), is closer to 0 than angleChange value, or is closer to 2PI rad than 2PI rad - angleChange value.
    private static double fixAngle(double angle, double angleChange) {
        if (angle > FULL_CIRCLE) {
            return 0;
        }

        if (angle > 0 && angle < angleChange && angle < angleChange / 2) {
            return 0;
        }

        if (angle > FULL_CIRCLE - angle && angle > FULL_CIRCLE - (angleChange / 2)) {
            return 0;
        }

        return angle;
    }

    /**
     * Converts Euler Angles to a quaternion
     * @param euler an XYZ euler angle
     * @return a float Quaternion
     */
    public static Quaternionf eulerToQuaternion(EulerAngle euler) {
        Quaternionf out = new Quaternionf();
        out.rotateXYZ((float) euler.getX(), (float) euler.getY(), (float) euler.getZ());
        return out;
    }

    /**
     * Converts a Quaternion to Euler Angle - for Display Entities
     * @param quaternion - the quaternion to convert
     * @return an EulerAngle (XYZ order)
     */
    public static EulerAngle quaternionToEuler(Quaternionf quaternion) {
        Vector3f euler = quaternion.getEulerAnglesXYZ(new Vector3f());
        return new EulerAngle(euler.x,euler.y,euler.z);
    }
}
