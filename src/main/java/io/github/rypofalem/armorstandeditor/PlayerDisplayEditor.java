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
    private UUID uuid;
    UUID armorStandID;
    DisplayEditMode eMode;
    AdjustmentMode adjMode;
    CopySlots copySlots;
    Axis axis;
    double eulerAngleChange;
    double degreeAngleChange;
    double movChange;
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
        if (adjMode == AdjustmentMode.COARSE) {
            eulerAngleChange = getManager().coarseAdj;
            movChange = getManager().coarseMov;
        } else {
            eulerAngleChange = getManager().fineAdj;
            movChange = getManager().fineMov;
        }
        degreeAngleChange = eulerAngleChange / Math.PI * 180;
        sendMessage("setadj", adjMode.toString().toLowerCase());
    }

    public void setCopySlot(byte slot) {
        copySlots.changeSlots(slot);
        sendMessage("setslot", String.valueOf((slot + 1)));
    }

    public void editDisplay(Display displayEntity) { //TODO
        if (getPlayer().hasPermission("asedit.basic")) {

            displayEntity = attemptTarget(displayEntity);
            switch (eMode) {
                case YAW:
                    rotateYaw(displayEntity);
                    break;
                case PITCH:
                    rotatePitch(displayEntity);
                    break;
                case COPY:
                    copy(displayEntity);
                    break;
                case PASTE:
                    paste(displayEntity);
                    break;
                case DELETE:
                    break;
                case PLACEMENT:
                    move(displayEntity);
                    break;
                case RESET:
                    resetPosition(displayEntity);
                    break;
                case GLOWING:
                    toggleGlowing(displayEntity);
                    break;
                case SCALE: //TODO
                    break;
                case LEFT_ROTATE: //TODO
                    break;
                case RIGHT_ROTATE: //TODO
                    break;
                case GLOW_COLOR: //TODO
                    break;
                case BILLBOARD: //TODO
                    break;
                case BLOCK_LIGHT: //TODO
                    break;
                case SKY_LIGHT: //TODO
                    break;
                case SHADOW_RADIUS: //TODO
                    break;
                case SHADOW_STRENGTH: //TODO
                    break;
                case ITEM: //TODO
                    break;
                case ITEM_MODE: //TODO
                    break;
                case BLOCK_DATA: //TODO
                    break;
                case TEXT: //TODO
                    break;
                case TEXT_ALIGN: //TODO
                    break;
                case TEXT_BACKGROUND: //TODO
                    break;
                case TEXT_LINEWIDTH: //TODO
                    break;
                case TEXT_OPACITY: //TODO
                    break;
                case TEXT_SHADOW: //TODO
                    break;
                case NONE:
                default:
                    sendMessage("nomode", null);
                    break;

            }
        }else return;
    }

    private void openEquipment(ArmorStand armorStand) {
        if (!getPlayer().hasPermission("asedit.equipment")) return;
        //if (team != null && team.hasEntry(armorStand.getName())) return; //Do not allow editing if the ArmorStand is Disabled
        //equipMenu = new EquipmentMenu(this, armorStand);
        //TODO
        equipMenu.open();
    }

    public void reverseEditDisplay(Display displayEntity) {
        if (!getPlayer().hasPermission("asedit.basic")) return;

        //Generate a new ArmorStandManipulationEvent and call it out.
        //ArmorStandManipulatedEvent event = new ArmorStandManipulatedEvent(displayEntity, getPlayer());
        //Bukkit.getPluginManager().callEvent(event); // Bukkit handles the call out //TODO: Folia Refactor
        //if (event.isCancelled()) return; //do nothing if cancelled

        displayEntity = attemptTarget(displayEntity); //TODO fix
        switch (eMode) {
            case PLACEMENT:
                reverseMove(displayEntity);
                break;
            case YAW:
                reverseRotateYaw(displayEntity);
                break;
            case PITCH:
                reverseRotatePitch(displayEntity);
                break;
            default:
                editDisplay(displayEntity);
        }
    }

    private void move(Display displayEntity) {
        if (!getPlayer().hasPermission("asedit.movement")) return;

        //Generate a new ArmorStandManipulationEvent and call it out. //TODO make a display entity version
        //ArmorStandManipulatedEvent event = new ArmorStandManipulatedEvent(displayEntity, getPlayer());
        //Bukkit.getPluginManager().callEvent(event); // Bukkit handles the call out //TODO: Folia Refactor
        //if (event.isCancelled()) return; //do nothing if cancelled

        Location loc = displayEntity.getLocation();
        switch (axis) {
            case X:
                loc.add(movChange, 0, 0);
                break;
            case Y:
                loc.add(0, movChange, 0);
                break;
            case Z:
                loc.add(0, 0, movChange);
                break;
        }
        Scheduler.teleport(displayEntity, loc);
    }

    private void reverseMove(Display display) {
        if (!getPlayer().hasPermission("asedit.movement")) return;
        Location loc = display.getLocation();
        switch (axis) {
            case X:
                loc.subtract(movChange, 0, 0);
                break;
            case Y:
                loc.subtract(0, movChange, 0);
                break;
            case Z:
                loc.subtract(0, 0, movChange);
                break;
        }
        Scheduler.teleport(display, loc);
    }

    private void rotateYaw(Display display) {
        if (!getPlayer().hasPermission("asedit.rotation")) return;
        Location loc = display.getLocation();
        float yaw = loc.getYaw();
        loc.setYaw((yaw + 180 + (float) degreeAngleChange) % 360 - 180);
        Scheduler.teleport(display, loc);
    }

    private void rotatePitch(Display display) {
        if (!getPlayer().hasPermission("asedit.rotation")) return;
        Location loc = display.getLocation();
        float pitch = loc.getPitch();
        loc.setPitch((pitch + 180 + (float) degreeAngleChange) % 360 - 180);
        Scheduler.teleport(display, loc);
    }

    private void reverseRotateYaw(Display display) {
        if (!getPlayer().hasPermission("asedit.rotation")) return;
        Location loc = display.getLocation();
        float yaw = loc.getYaw();
        loc.setYaw((yaw + 180 - (float) degreeAngleChange) % 360 - 180);
        Scheduler.teleport(display, loc);
    }

    private void reverseRotatePitch(Display display) {
        if (!getPlayer().hasPermission("asedit.rotation")) return;
        Location loc = display.getLocation();
        float pitch = loc.getPitch();
        loc.setPitch((pitch + 180 - (float) degreeAngleChange) % 360 - 180);
        Scheduler.teleport(display, loc);
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

    private void resetPosition(Display displayEntity) {
        if (getPlayer().hasPermission("asedit.reset")) {
            displayEntity.setTransformation(new Transformation(new Vector3f(),new AxisAngle4f(),new Vector3f(1),new AxisAngle4f()));
            displayEntity.setRotation(0,0);
        } else{
            sendMessage("nopermoption", "warn", "reset");
        }
    }

    void toggleGlowing(Display display){
        if(getPlayer().hasPermission("asedit.togglearmorstandglow")){
            //Will only make it glow white - Not something we can do like with Locking. Do not request this!
            //Otherwise, this simple function becomes a mess to maintain. As you would need a Team generated with each
            //Color and I ain't going to impose that on servers.
            display.setGlowing(!display.isGlowing());
        } else{
            sendMessage("nopermoption", "warn", "armorstandglow");
        }
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

    private EulerAngle addEulerAngle(EulerAngle angle) {
        switch (axis) {
            case X:
                angle = angle.setX(Util.addAngle(angle.getX(), eulerAngleChange));
                break;
            case Y:
                angle = angle.setY(Util.addAngle(angle.getY(), eulerAngleChange));
                break;
            case Z:
                angle = angle.setZ(Util.addAngle(angle.getZ(), eulerAngleChange));
                break;
            default:
                break;
        }
        return angle;
    }

    private EulerAngle subEulerAngle(EulerAngle angle) {
        switch (axis) {
            case X:
                angle = angle.setX(Util.subAngle(angle.getX(), eulerAngleChange));
                break;
            case Y:
                angle = angle.setY(Util.subAngle(angle.getY(), eulerAngleChange));
                break;
            case Z:
                angle = angle.setZ(Util.subAngle(angle.getZ(), eulerAngleChange));
                break;
            default:
                break;
        }
        return angle;
    }

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
            //      Otherwise, its unlocked and will display WHITE as its not in a team by default

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

    private void highlight(Display display) {
        if (!display.isGlowing()) {
            display.setGlowing(true);
            Bukkit.getScheduler().runTaskLater(plugin,() -> display.setGlowing(false),50);
        } //TODO folia
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
}
