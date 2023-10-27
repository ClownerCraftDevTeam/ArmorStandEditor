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

import io.github.rypofalem.armorstandeditor.api.*;
import io.github.rypofalem.armorstandeditor.menu.DisplayMenu;
import io.github.rypofalem.armorstandeditor.menu.EquipmentMenu;
import io.github.rypofalem.armorstandeditor.modes.Axis;
import io.github.rypofalem.armorstandeditor.modes.*;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.util.EulerAngle;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.UUID;

public class PlayerDisplayEditor {
    public ArmorStandEditorPlugin plugin;
    private final UUID uuid;
    DisplayEditMode eMode;
    AdjustmentMode adjMode;
    CopySlots copySlots;
    Axis axis;
    double eulerAngleChange;
    double degreeAngleChange;
    double movChange;
    float scaleChange;
    DisplayMenu chestMenu;
    Display target;
    ArrayList<Display> targetList = null;
    int targetIndex = 0;
    EquipmentMenu equipMenu;
    long lastCancelled = 0;

    public PlayerDisplayEditor(UUID uuid, ArmorStandEditorPlugin plugin) {
        this.uuid = uuid;
        this.plugin = plugin;
        eMode = DisplayEditMode.NONE;
        adjMode = AdjustmentMode.COARSE;
        axis = Axis.X;
        copySlots = new CopySlots();
        eulerAngleChange = getManager().coarseAdj;
        degreeAngleChange = eulerAngleChange / Math.PI * 180;
        movChange = getManager().coarseMov;
        chestMenu = new DisplayMenu(this);
    }

    // Mode toggles

    public void setMode(DisplayEditMode editMode) {
        this.eMode = editMode;
        sendMessage("setmode", editMode.toString().toLowerCase());
    }

    public void setAxis(Axis axis) {
        this.axis = axis;
        sendMessage("setaxis", axis.toString().toLowerCase());
    }

    public void setAdjMode(AdjustmentMode adjMode) {
        this.adjMode = adjMode;
        switch (adjMode) {
            case COARSE -> {
                eulerAngleChange = getManager().coarseAdj;
                movChange = getManager().coarseMov;
                scaleChange = (float) getManager().coarseMov; //TODO tweak this
            }
            case MEDIUM -> {
                //TODO implement
            }
            case FINE -> {
                eulerAngleChange = getManager().fineAdj;
                movChange = getManager().fineMov;
                scaleChange = (float) getManager().coarseMov; //TODO tweak this
            }
        }

        degreeAngleChange = eulerAngleChange / Math.PI * 180;
        sendMessage("setadj", adjMode.toString().toLowerCase());
    }

    public void setCopySlot(byte slot) {
        copySlots.changeSlots(slot);
        sendMessage("setslot", String.valueOf((slot + 1)));
    }

    void cycleAxis(int i) {
        int index = axis.ordinal();
        index += i;
        index = index % Axis.values().length;
        while (index < 0) {
            index += Axis.values().length;
        }
        setAxis(Axis.values()[index]);
    }

    // Do the edit

    public void performEdit(Display displayEntity, boolean reversed) {
        if (getPlayer().hasPermission("asedit.basic")) {
            displayEntity = attemptTarget(displayEntity);
            switch (eMode) {
                case YAW -> rotateYaw(displayEntity,reversed);
                case PITCH -> rotatePitch(displayEntity,reversed);
                case COPY -> copy(displayEntity);
                case PASTE -> paste(displayEntity);
                case DELETE -> delete(displayEntity);
                case PLACEMENT -> move(displayEntity,reversed);
                case RESET -> resetPosition(displayEntity);
                case GLOWING -> toggleGlowing(displayEntity);
                case SCALE -> scale(displayEntity,reversed);
                case LEFT_ROTATE -> rotateLeft(displayEntity,reversed);
                case RIGHT_ROTATE -> rotateRight(displayEntity,reversed);
                case BILLBOARD -> cycleBillboard(displayEntity);
                case BLOCK_LIGHT -> blockLight(displayEntity,reversed);
                case SKY_LIGHT -> skyLight(displayEntity,reversed);
                case SHADOW_RADIUS -> shadowRadius(displayEntity,reversed);
                case SHADOW_STRENGTH -> shadowStrength(displayEntity,reversed);
                case GLOW_COLOR -> openGlowColorMenu(displayEntity);
                case EQUIPMENT -> openEquipment(displayEntity);
                case TEXT -> openTextMenu(displayEntity);
                default -> sendMessage("nomode", null);
            }
        }
    }

    @Deprecated
    public void reverseEditDisplay(Display displayEntity) {
        if (!getPlayer().hasPermission("asedit.basic")) return;

        //Generate a new ArmorStandManipulationEvent and call it out.
        //ArmorStandManipulatedEvent event = new ArmorStandManipulatedEvent(displayEntity, getPlayer());
        //Bukkit.getPluginManager().callEvent(event); // Bukkit handles the call out //TODO: Folia Refactor
        //if (event.isCancelled()) return; //do nothing if cancelled

        displayEntity = attemptTarget(displayEntity); //TODO fix
        switch (eMode) {
            case PLACEMENT -> move(displayEntity, true);
            case YAW -> rotateYaw(displayEntity, true);
            case PITCH -> rotatePitch(displayEntity, true);
            default -> performEdit(displayEntity, false);
        }
    }

    // Menus

    public void openMenu() {
        if (!isMenuCancelled()) {
            Scheduler.runTaskLater(plugin, new OpenMenuTask(), 1);
        }
    }

    public void cancelOpenMenu() {
        lastCancelled = getManager().getTime();
    }

    boolean isMenuCancelled() {
        return getManager().getTime() - lastCancelled < 2;
    }

    private class OpenMenuTask implements Runnable {

        @Override
        public void run() {
            if (isMenuCancelled()) return;

            //API: PlayerOpenMenuEvent
            PlayerOpenMenuEvent event = new PlayerOpenMenuEvent(getPlayer());
            Bukkit.getPluginManager().callEvent(event); //TODO: Folia Refactor
            if (event.isCancelled()) return;

            chestMenu.openMenu();
        }
    }

    private void openEquipment(Display display) {
        if (!getPlayer().hasPermission("asedit.equipment")) return;
        //if (team != null && team.hasEntry(armorStand.getName())) return; //Do not allow editing if the ArmorStand is Disabled
        //equipMenu = new EquipmentMenu(this, armorStand);
        //TODO
        equipMenu.open();
    }

    private void openGlowColorMenu(Display display) {
        //TODO
    }

    private void openTextMenu(Display display) {
        //TODO
    }

    // Edit Methods
    
    private void move(Display displayEntity, boolean reverse) {
        if (!getPlayer().hasPermission("asedit.movement")) return;

        //Generate a new ArmorStandManipulationEvent and call it out. //TODO make a display entity version
        ArmorStandManipulatedEvent event = new ArmorStandManipulatedEvent(null, getPlayer());
        Bukkit.getPluginManager().callEvent(event); // Bukkit handles the call out //TODO: Folia Refactor
        if (event.isCancelled()) return; //do nothing if cancelled

        Location loc = displayEntity.getLocation();
        switch (axis) {
            case X -> {
                if (reverse) loc.subtract(movChange, 0, 0);
                else loc.add(movChange, 0, 0);
            }
            case Y -> {
                if (reverse) loc.subtract(0, movChange, 0);
                else loc.add(0, movChange, 0);
            }
            case Z -> {
                if (reverse) loc.subtract(0, 0, movChange);
                else loc.add(0, 0, movChange);
            }
        }
        Scheduler.teleport(displayEntity, loc);
    }

    private void rotateYaw(Display display, boolean reverse) {
        if (!getPlayer().hasPermission("asedit.rotation")) return;
        Location loc = display.getLocation();
        float yaw = loc.getYaw();
        if (reverse) loc.setYaw((yaw + 180 - (float) degreeAngleChange) % 360 - 180);
        else loc.setYaw((yaw + 180 + (float) degreeAngleChange) % 360 - 180);
        Scheduler.teleport(display, loc);
    }

    private void rotatePitch(Display display, boolean reverse) {
        if (!getPlayer().hasPermission("asedit.rotation")) return;
        Location loc = display.getLocation();
        float pitch = loc.getPitch();
        if (reverse) loc.setPitch((pitch + 180 - (float) degreeAngleChange) % 360 - 180);
        loc.setPitch((pitch + 180 + (float) degreeAngleChange) % 360 - 180);
        Scheduler.teleport(display, loc);
    }

    private void scale(Display display, boolean reverse) {
        if (!getPlayer().hasPermission("asedit.display.scale")) return;

        Transformation transformation = display.getTransformation();
        switch (axis) {
            case X -> {
                if (reverse) transformation.getScale().sub(scaleChange, 0, 0);
                else transformation.getScale().add(scaleChange, 0, 0);
            }
            case Y -> {
                if (reverse) transformation.getScale().sub(0, scaleChange, 0);
                else transformation.getScale().add(0, scaleChange, 0);
            }
            case Z -> {
                if (reverse) transformation.getScale().sub(0, 0, scaleChange);
                else transformation.getScale().add(0, 0, scaleChange);
            }
        }
        //TODO implement a limit on scale
    }

    private void rotateLeft(Display display, boolean reverse) {
        if (!getPlayer().hasPermission("asedit.display.rotate")) return; //TODO consistent perms

        Transformation transformation = display.getTransformation();
        switch (axis) {
            case X -> {
                if (reverse) transformation.getLeftRotation().rotateX((float) -eulerAngleChange);
                else transformation.getLeftRotation().rotateX((float) eulerAngleChange);
            }
            case Y -> {
                if (reverse) transformation.getLeftRotation().rotateY((float) -eulerAngleChange);
                else transformation.getLeftRotation().rotateY((float) eulerAngleChange);
            }
            case Z -> {
                if (reverse) transformation.getLeftRotation().rotateZ((float) -eulerAngleChange);
                else transformation.getLeftRotation().rotateZ((float) eulerAngleChange);
            }
        }
    }

    private void rotateRight(Display display, boolean reverse) {
        if (!getPlayer().hasPermission("asedit.display.rotate")) return; //TODO consistent perms

        Transformation transformation = display.getTransformation();
        switch (axis) {
            case X -> {
                if (reverse) transformation.getRightRotation().rotateX((float) -eulerAngleChange);
                else transformation.getRightRotation().rotateX((float) eulerAngleChange);
            }
            case Y -> {
                if (reverse) transformation.getRightRotation().rotateY((float) -eulerAngleChange);
                else transformation.getRightRotation().rotateY((float) eulerAngleChange);
            }
            case Z -> {
                if (reverse) transformation.getRightRotation().rotateZ((float) -eulerAngleChange);
                else transformation.getRightRotation().rotateZ((float) eulerAngleChange);
            }
        }
    }

    private void copy(Display display) {
        if (getPlayer().hasPermission("asedit.copy")) {
            //copySlots.copyDataToSlot(display);
            sendMessage("copied", "" + (copySlots.currentSlot + 1));
            setMode(DisplayEditMode.PASTE);
        }else{
            sendMessage("nopermoption", "warn", "copy");
        }

    } //TODO

    private void paste(Display display) {
        if (getPlayer().hasPermission("asedit.paste")) {
            ArmorStandData data = copySlots.getDataToPaste();
            if (data == null) return;
            //display.setHeadPose(data.headPos);
//            display.setBodyPose(data.bodyPos);
//            display.setLeftArmPose(data.leftArmPos);
//            display.setRightArmPose(data.rightArmPos);
//            display.setLeftLegPose(data.leftLegPos);
//            display.setRightLegPose(data.rightLegPos);
//            display.setSmall(data.size);
//            display.setGravity(data.gravity);
//            display.setBasePlate(data.basePlate);
//            display.setArms(data.showArms);
//            display.setVisible(data.visible);

            //Only Paste the Items on the stand if in Creative Mode
            // - Do not run elsewhere for good fecking reason!
            if (this.getPlayer().getGameMode() == GameMode.CREATIVE) {
//                display.getEquipment().setHelmet(data.head);
//                display.getEquipment().setChestplate(data.body);
//                display.getEquipment().setLeggings(data.legs);
//                display.getEquipment().setBoots(data.feetsies);
//                display.getEquipment().setItemInMainHand(data.rightHand);
//                display.getEquipment().setItemInOffHand(data.leftHand);
            }
            sendMessage("pasted", "" + (copySlots.currentSlot + 1));
        }else{
            sendMessage("nopermoption", "warn", "paste");
        }
    } //TODO

    private void delete(Display display) {
        display.remove();
        //TODO there's probably more that needs to happen here... like region checks or something
    } //TODO

    private void resetPosition(Display displayEntity) {
        if (getPlayer().hasPermission("asedit.reset")) {
            displayEntity.setTransformation(new Transformation(new Vector3f(),new AxisAngle4f(),new Vector3f(1),new AxisAngle4f()));
            displayEntity.setRotation(0,0);
        } else{
            sendMessage("nopermoption", "warn", "reset");
        }
    }
    
    private void cycleBillboard(Display displayEntity) {
        if (!getPlayer().hasPermission("asedit.display.billboard")) return;
        
        switch (displayEntity.getBillboard()) {
            case FIXED -> displayEntity.setBillboard(Display.Billboard.VERTICAL);
            case VERTICAL -> displayEntity.setBillboard(Display.Billboard.HORIZONTAL);
            case HORIZONTAL -> displayEntity.setBillboard(Display.Billboard.CENTER);
            case CENTER -> displayEntity.setBillboard(Display.Billboard.FIXED);
        }
        //TODO notify player?
    }
    
    private void blockLight(Display displayEntity, boolean reverse) {
        if (!getPlayer().hasPermission("asedit.display.light")) return;

        Display.Brightness current = displayEntity.getBrightness();
        assert current != null;
        displayEntity.setBrightness(new Display.Brightness(
                current.getBlockLight() + ((reverse)? -1:1),
                current.getSkyLight()));
    }

    private void skyLight(Display displayEntity, boolean reverse) {
        if (!getPlayer().hasPermission("asedit.display.light")) return;

        Display.Brightness current = displayEntity.getBrightness();
        assert current != null;
        displayEntity.setBrightness(new Display.Brightness(
                current.getBlockLight(),
                current.getSkyLight() + ((reverse)? -1:1)));
    }

    private void shadowRadius(Display displayEntity, boolean reverse) {
        if (!getPlayer().hasPermission("asedit.display.shadow")) return;

        //TODO this may need to scale with COARSE/FINE
        displayEntity.setShadowRadius(displayEntity.getShadowRadius() + ((reverse)? -1:1));
    }

    private void shadowStrength(Display displayEntity, boolean reverse) {
        if (!getPlayer().hasPermission("asedit.display.shadow")) return;

        //TODO this may need to scale with COARSE/FINE
        displayEntity.setShadowStrength(displayEntity.getShadowStrength() + ((reverse)? -0.1f:0.1f));
    }

    private void toggleGlowing(Display display) {
        if(getPlayer().hasPermission("asedit.togglearmorstandglow")){
            display.setGlowing(!display.isGlowing());
        } else{
            sendMessage("nopermoption", "warn", "armorstandglow");
        }
    }

    //Angle tools

    private EulerAngle addEulerAngle(EulerAngle angle) {
        switch (axis) {
            case X -> angle = angle.setX(Util.addAngle(angle.getX(), eulerAngleChange));
            case Y -> angle = angle.setY(Util.addAngle(angle.getY(), eulerAngleChange));
            case Z -> angle = angle.setZ(Util.addAngle(angle.getZ(), eulerAngleChange));
            default -> {
            }
        }
        return angle;
    }

    private EulerAngle subEulerAngle(EulerAngle angle) {
        switch (axis) {
            case X -> angle = angle.setX(Util.subAngle(angle.getX(), eulerAngleChange));
            case Y -> angle = angle.setY(Util.subAngle(angle.getY(), eulerAngleChange));
            case Z -> angle = angle.setZ(Util.subAngle(angle.getZ(), eulerAngleChange));
            default -> {
            }
        }
        return angle;
    }

    // Targetting

    public void setTarget(ArrayList<Display> displays) {
        if (displays == null || displays.isEmpty()) {
            target = null;
            targetList = null;
            sendMessage("notarget", "armorstand");
        } else {
            if (targetList == null) {
                targetList = displays;
                targetIndex = 0;
                sendMessage("target", null);
            } else {
                boolean same = targetList.size() == displays.size();
                if (same) for (Display as : displays) {
                    same = targetList.contains(as);
                    if (!same) break;
                }

                if (same) {
                    targetIndex = ++targetIndex % targetList.size();
                } else {
                    targetList = displays;
                    targetIndex = 0;
                    sendMessage("target", null);
                }
            }

            //API: ArmorStandTargetedEvent //TODO - display entity version
            //ArmorStandTargetedEvent e = new ArmorStandTargetedEvent(targetList.get(targetIndex), getPlayer());
            //Bukkit.getPluginManager().callEvent(e); //TODO: Folia Refactor
            //if (e.isCancelled()) return;

            target = targetList.get(targetIndex);
            highlight(target); //NOTE: If Targeted and Locked, it displays the TEAM Color Glow: RED
            //      Otherwise, its unlocked and will display WHITE as it's not in a team by default

        }
    }

    Display attemptTarget(Display display) {
        if (target == null
            || !target.isValid()
            || target.getWorld() != getPlayer().getWorld()
            || target.getLocation().distanceSquared(getPlayer().getLocation()) > 100)
            return display;
        display = target;
        return display;
    }

    private void highlight(Display display) {
        if (!display.isGlowing()) {
            display.setGlowing(true);
            Bukkit.getScheduler().runTaskLater(plugin,() -> display.setGlowing(false),50);
        } //TODO folia
    }

    // Misc

    void sendMessage(String path, String format, String option) {
        String message = plugin.getLang().getMessage(path, format, option);
        if (plugin.sendToActionBar) {
            if (ArmorStandEditorPlugin.instance().getHasPaper() || ArmorStandEditorPlugin.instance().getHasSpigot()) { //Paper and Spigot having the same Interaction for sendToActionBar
                plugin.getServer().getPlayer(getUUID()).spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(message));
            } else {
                String rawText = plugin.getLang().getRawMessage(path, format, option);
                String command = "title %s actionbar %s".formatted(plugin.getServer().getPlayer(getUUID()).getName(), rawText);
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
            }
        } else {
            plugin.getServer().getPlayer(getUUID()).sendMessage(message);
        }
    }

    void sendMessage(String path, String option) {
        sendMessage(path, "info", option);
    }

    public PlayerEditorManager getManager() {
        return plugin.editorManager;
    }

    public Player getPlayer() {
        return plugin.getServer().getPlayer(getUUID());
    }

    public UUID getUUID() {
        return uuid;
    }
}
