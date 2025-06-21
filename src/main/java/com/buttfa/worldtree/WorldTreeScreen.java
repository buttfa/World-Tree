package com.buttfa.worldtree;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.network.chat.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Queue;

// World tree interface
class WorldTreeScreen extends net.minecraft.client.gui.screens.Screen {
    private static final int HORIZONTAL_SPACING = 20; // Horizontal spacing between nodes
    private static final int VERTICAL_SPACING = 30; // Vertical spacing between nodes
    private static final int LINE_COLOR = 0xFFAAAAAA; // Connection line color
    private static final int ROOT_Y = 30; // Root node Y coordinate
    private static final int DUMMY_COLOR = 0x40FFFFFF; // Dummy node background color
    private static final int DUMMY_BORDER_COLOR = 0xFFAAAAAA; // Dummy node border color (dashed)
    private static final int DUMMY_LINE_COLOR = 0x80AAAAAA; // Dummy node connection line color
    private static final int DUMMY_PARENT_COLOR = 0x60FFA500; // Special color for dummy node's parent (orange)

    private float zoom = 1.0f; // Current zoom level
    private int offsetX = 0; // X-axis offset
    private int offsetY = 0; // Y-axis offset
    private int dragStartX, dragStartY; // Drag start coordinates
    private int startOffsetX, startOffsetY; // Offset at start of dragging
    private boolean isDragging = false; // Whether dragging is in progress
    private WorldTree.TreeNode selectedNode; // Currently selected node

    // Store all node buttons
    private final Map<WorldTree.TreeNode, NodeButton> nodeButtons = new HashMap<>();

    protected WorldTreeScreen() {
        super(Component.literal("World Tree Screen"));
    }

    @SuppressWarnings("null")
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);

        // Update positions of all node buttons
        updateNodeButtonsPosition();

        // Draw connection lines (including dummy nodes)
        drawTreeLines(guiGraphics);

        // Draw node buttons
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        // Draw zoom hint
        guiGraphics.drawString(font, "Zoom: " + String.format("%.1f", zoom) +
                " | Use mouse wheel to zoom | Drag to pan | Right-click to expand/collapse", 10, 10, 0xFFFFFF);
    }

    // Update positions of all node buttons
    private void updateNodeButtonsPosition() {
        for (Map.Entry<WorldTree.TreeNode, NodeButton> entry : nodeButtons.entrySet()) {
            WorldTree.TreeNode node = entry.getKey();
            NodeButton button = entry.getValue();

            // Apply offset and zoom
            int x = (int) ((node.x - offsetX) * zoom);
            int y = (int) ((node.y - offsetY) * zoom);
            int width = (int) (node.width * zoom);
            int height = (int) (node.height * zoom);

            button.setPosition(x, y);
            button.setWidth(width);
            button.setHeight(height);
        }
    }

    @Override
    protected void init() {
        super.init();
        clearWidgets();
        nodeButtons.clear();

        // Reset selected node
        selectedNode = null;

        // Calculate tree layout (including dummy nodes)
        calculateTreeLayout();

        // Create node buttons
        createNodeButtons();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Handle right-click first (for node buttons)
        if (button == 1) { // Right-click
            for (GuiEventListener widget : children()) {
                if (widget instanceof NodeButton buttonWidget && buttonWidget.isMouseOver(mouseX, mouseY)) {
                    buttonWidget.node.expanded = !buttonWidget.node.expanded;
                    init(); // Reinitialize interface
                    return true;
                }
            }
        }

        // Call super to handle other clicks (left-click buttons, etc.)
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }

        // Handle left-click on blank area (start dragging)
        if (button == 0) {
            isDragging = true;
            dragStartX = (int) mouseX;
            dragStartY = (int) mouseY;
            startOffsetX = offsetX;
            startOffsetY = offsetY;
            return true;
        }

        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button == 0 && isDragging) {
            offsetX = startOffsetX + (dragStartX - (int) mouseX);
            offsetY = startOffsetY + (dragStartY - (int) mouseY);
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            isDragging = false;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        float zoomFactor = 0.1f;
        if (deltaY > 0) {
            zoom += zoomFactor;
        } else if (deltaY < 0) {
            zoom = Math.max(0.5f, zoom - zoomFactor);
        }
        return true;
    }

    // Calculate tree layout (using bottom-up approach for subtree widths)
    private void calculateTreeLayout() {
        // First calculate all node widths (based on text content)
        calculateNodeWidths(WorldTree.rootNode);

        // Then calculate subtree widths
        calculateSubtreeWidths(WorldTree.rootNode);

        // Finally calculate each node's position
        calculateNodePositions(WorldTree.rootNode, width / 2, ROOT_Y);
    }

    // Calculate node width (based on text content and padding)
    private void calculateNodeWidths(WorldTree.TreeNode node) {
        // Calculate text width
        int textWidth = font.width(node.displayName);
        // Set node width = text width + left/right padding
        node.width = textWidth + node.padding * 2;

        // If there are children, add extra space for expand/collapse marker
        if (!node.children.isEmpty()) {
            node.width += 15; // Reserve space for marker
        }

        // Recursively calculate child nodes (only when node is expanded)
        if (node.expanded) {
            for (WorldTree.TreeNode child : node.children) {
                calculateNodeWidths(child);
            }
        }
    }

    // Recursively calculate subtree widths
    private int calculateSubtreeWidths(WorldTree.TreeNode node) {
        if (node.expanded && !node.children.isEmpty()) {
            int totalWidth = 0;
            for (WorldTree.TreeNode child : node.children) {
                totalWidth += calculateSubtreeWidths(child) + HORIZONTAL_SPACING;
            }
            // Remove extra spacing for last child
            totalWidth = Math.max(0, totalWidth - HORIZONTAL_SPACING);

            // Subtree width takes max between current node width and children total width
            node.subtreeWidth = Math.max(node.width, totalWidth);
        } else {
            // Not expanded or no children, subtree width is node's own width
            node.subtreeWidth = node.width;
        }
        return node.subtreeWidth;
    }

    // Recursively calculate node positions (children symmetric about parent's
    // vertical center)
    private void calculateNodePositions(WorldTree.TreeNode node, int x, int y) {
        // Set node position: x is center minus half node width
        node.x = x - node.width / 2;
        node.y = y;

        if (!node.expanded || node.children.isEmpty())
            return;

        // Calculate child nodes' starting Y position
        int childY = y + node.height + VERTICAL_SPACING;

        // Calculate children total width (including spacing)
        int childrenTotalWidth = 0;
        for (WorldTree.TreeNode child : node.children) {
            childrenTotalWidth += child.subtreeWidth;
        }
        if (node.children.size() > 1) {
            childrenTotalWidth += HORIZONTAL_SPACING * (node.children.size() - 1);
        }

        // Calculate children's starting X position (symmetric about parent's center)
        int childX = x - childrenTotalWidth / 2;

        // Place each child node
        for (WorldTree.TreeNode child : node.children) {
            // Calculate child's center position (children arranged left to right)
            int childCenterX = childX + child.subtreeWidth / 2;

            // Recursively calculate child position
            calculateNodePositions(child, childCenterX, childY);

            // Move to next child's start position
            childX += child.subtreeWidth + HORIZONTAL_SPACING;
        }
    }

    private void createNodeButtons() {
        Queue<WorldTree.TreeNode> queue = new java.util.LinkedList<>();
        queue.add(WorldTree.rootNode);

        while (!queue.isEmpty()) {
            WorldTree.TreeNode node = queue.poll();

            // Create node button
            NodeButton button;
            if (node.id.equals(WorldTree.DUMMY_NODE_ID)) {
                button = new DummyNodeButton(
                        0, // Position updated in render
                        0,
                        (int) (node.width * zoom),
                        (int) (node.height * zoom),
                        node,
                        btn -> {
                            // Open add node interface, passing dummy node's parent
                            Minecraft.getInstance().setScreen(new DummyNodeScreen(node.parent));
                        });
            } else {
                button = new NodeButton(
                        0, // Position updated in render
                        0,
                        (int) (node.width * zoom),
                        (int) (node.height * zoom),
                        node,
                        btn -> {
                            selectedNode = node;
                            // Open new interface for operation options
                            Minecraft.getInstance().setScreen(new NodeOperationScreen(node));
                        });
            }

            addRenderableWidget(button);
            nodeButtons.put(node, button);

            // Add children to queue (if expanded)
            if (node.expanded) {
                queue.addAll(node.children);
            }
        }
    }

    private void drawTreeLines(GuiGraphics guiGraphics) {
        Queue<WorldTree.TreeNode> queue = new java.util.LinkedList<>();
        queue.add(WorldTree.rootNode);

        while (!queue.isEmpty()) {
            WorldTree.TreeNode node = queue.poll();

            // Draw connection lines to children
            if (node.expanded) {
                for (WorldTree.TreeNode child : node.children) {
                    // Calculate connection points using node centers and bottoms/tops
                    int startX = (int) ((node.getCenterX() - offsetX) * zoom);
                    int startY = (int) ((node.getBottomY() - offsetY) * zoom);
                    int endX = (int) ((child.getCenterX() - offsetX) * zoom);
                    int endY = (int) ((child.getTopY() - offsetY) * zoom);

                    // Draw smooth connection line
                    drawSmoothLine(guiGraphics, startX, startY, endX, endY,
                            node.id.equals(WorldTree.DUMMY_NODE_ID) || child.id.equals(WorldTree.DUMMY_NODE_ID)
                                    ? DUMMY_LINE_COLOR
                                    : LINE_COLOR);
                }
                queue.addAll(node.children);
            }
        }
    }

    // Draw smooth line (with anti-aliasing)
    private void drawSmoothLine(GuiGraphics guiGraphics, int x1, int y1, int x2, int y2, int color) {
        // Calculate line direction
        int dx = Math.abs(x2 - x1);
        int dy = Math.abs(y2 - y1);
        int sx = (x1 < x2) ? 1 : -1;
        int sy = (y1 < y2) ? 1 : -1;
        int err = dx - dy;
        int e2;

        // Main color (75% opacity)
        int mainColor = (color & 0x00FFFFFF) | 0xBF000000; // BF = 75% opacity

        // Auxiliary color (50% opacity)
        int auxColor = (color & 0x00FFFFFF) | 0x80000000; // 80 = 50% opacity

        while (true) {
            // Draw main pixel
            guiGraphics.fill(x1, y1, x1 + 1, y1 + 1, mainColor);

            // Draw auxiliary pixels (anti-aliasing)
            if (Math.abs(err) * 2 >= dx + dy) {
                // Horizontal anti-aliasing
                guiGraphics.fill(x1 + sx, y1, x1 + sx + 1, y1 + 1, auxColor);
            } else {
                // Vertical anti-aliasing
                guiGraphics.fill(x1, y1 + sy, x1 + 1, y1 + sy + 1, auxColor);
            }

            if (x1 == x2 && y1 == y2)
                break;

            e2 = 2 * err;
            if (e2 > -dy) {
                err -= dy;
                x1 += sx;
            }
            if (e2 < dx) {
                err += dx;
                y1 += sy;
            }
        }
    }

    // Custom button class
    private class NodeButton extends Button {
        protected final WorldTree.TreeNode node;

        public NodeButton(int x, int y, int width, int height, WorldTree.TreeNode node, OnPress onPress) {
            super(x, y, width, height, node.name, onPress, DEFAULT_NARRATION);
            this.node = node;
        }

        @SuppressWarnings("null")
        @Override
        public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            // Check in real time if current node is selected
            boolean isSelected = (this.node == WorldTreeScreen.this.selectedNode);

            // Check if current node is parent of dummy node
            boolean isDummyParent = false;
            for (WorldTree.TreeNode child : node.children) {
                if (child.id.equals(WorldTree.DUMMY_NODE_ID)) {
                    isDummyParent = true;
                    break;
                }
            }

            int bgColor;
            int borderColor;

            if (isDummyParent) {
                // Special color for dummy node parent
                bgColor = isSelected ? DUMMY_PARENT_COLOR | 0x20000000 : DUMMY_PARENT_COLOR;
                borderColor = 0xFFFFA500; // Orange border
            } else {
                // Normal node
                bgColor = isSelected ? 0x60FFFF00 : (isHovered() ? 0x6000FF00 : 0x60000000);
                borderColor = isSelected ? 0xFFFFFF00 : (isHovered() ? 0xFF00FF00 : 0xFFFFFFFF);
            }

            // Draw button background
            guiGraphics.fill(getX(), getY(), getX() + width, getY() + height, bgColor);

            // Draw button border
            guiGraphics.renderOutline(getX(), getY(), width, height, borderColor);

            // Calculate text position (consider padding)
            int textX = getX() + node.padding;
            int textWidth = font.width(getMessage());
            int availableWidth = width - node.padding * 2;

            // Reserve space for expand/collapse marker if needed
            if (!node.children.isEmpty()) {
                availableWidth -= 15;
            }

            // Draw button text (add ellipsis if too long)
            if (textWidth > availableWidth) {
                String text = getMessage().getString();
                String clipped = font.plainSubstrByWidth(text, availableWidth - 4) + "...";
                guiGraphics.drawString(font, clipped, textX, getY() + (height - 8) / 2, 0xFFFFFF);
            } else {
                guiGraphics.drawString(font, getMessage(), textX, getY() + (height - 8) / 2, 0xFFFFFF);
            }

            // Draw expand/collapse marker if node has children
            if (!node.children.isEmpty()) {
                int markerX = getX() + width - 15;
                int markerY = getY() + height / 2;
                int markerSize = 4;
                int markerColor = node.expanded ? 0xFFFF0000 : 0xFF00FF00; // Red=expanded, Green=collapsed

                // Draw expand/collapse marker (+/- symbol)
                if (node.expanded) {
                    // Draw minus sign (-)
                    guiGraphics.fill(markerX, markerY - markerSize, markerX + markerSize * 2, markerY + markerSize,
                            markerColor);
                } else {
                    // Draw plus sign (+)
                    guiGraphics.fill(markerX, markerY - markerSize, markerX + markerSize * 2, markerY + markerSize,
                            markerColor);
                    guiGraphics.fill(markerX - markerSize, markerY, markerX + markerSize, markerY + markerSize * 2,
                            markerColor);
                }
            }

            // Add special marker (star) for dummy node parent
            if (isDummyParent) {
                int starSize = 3;
                int starX = getX() + 5;
                int starY = getY() + (height - starSize * 2) / 2;

                // Draw star
                guiGraphics.fill(starX, starY + starSize, starX + starSize, starY + starSize * 2, 0xFFFFA500);
                guiGraphics.fill(starX + starSize, starY, starX + starSize * 2, starY + starSize * 2, 0xFFFFA500);
                guiGraphics.fill(starX + starSize * 2, starY + starSize, starX + starSize * 3, starY + starSize * 2,
                        0xFFFFA500);
            }
        }
    }

    // Dummy node button (subclass of NodeButton)
    private class DummyNodeButton extends NodeButton {
        public DummyNodeButton(int x, int y, int width, int height, WorldTree.TreeNode node, OnPress onPress) {
            super(x, y, width, height, node, onPress);
        }

        @Override
        public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            int bgColor = isHovered() ? DUMMY_COLOR | 0x20000000 : DUMMY_COLOR;

            // Draw dashed border
            int step = 3; // Dash step size
            for (int i = 0; i < width; i += step * 2) {
                // Top border
                guiGraphics.fill(getX() + i, getY(), getX() + Math.min(i + step, width), getY() + 1,
                        DUMMY_BORDER_COLOR);
                // Bottom border
                guiGraphics.fill(getX() + i, getY() + height - 1, getX() + Math.min(i + step, width),
                        getY() + height, DUMMY_BORDER_COLOR);
                // Left border
                if (i < height) {
                    guiGraphics.fill(getX(), getY() + i, getX() + 1, getY() + Math.min(i + step, height),
                            DUMMY_BORDER_COLOR);
                }
                // Right border
                if (i < height) {
                    guiGraphics.fill(getX() + width - 1, getY() + i, getX() + width,
                            getY() + Math.min(i + step, height), DUMMY_BORDER_COLOR);
                }
            }

            // Draw background
            guiGraphics.fill(getX(), getY(), getX() + width, getY() + height, bgColor);

            // Use same text rendering as normal nodes
            // Calculate text position (consider padding)
            int textX = getX() + node.padding;
            int textWidth = font.width(getMessage());
            int availableWidth = width - node.padding * 2;

            // Draw button text (add ellipsis if too long)
            if (textWidth > availableWidth) {
                String text = getMessage().getString();
                String clipped = font.plainSubstrByWidth(text, availableWidth - 4) + "...";
                guiGraphics.drawString(font, clipped, textX, getY() + (height - 8) / 2, 0xFFFFFF);
            } else {
                guiGraphics.drawString(font, getMessage(), textX, getY() + (height - 8) / 2, 0xFFFFFF);
            }
        }
    }
}

// Node operation interface
class NodeOperationScreen extends net.minecraft.client.gui.screens.Screen {
    private final WorldTree.TreeNode node;
    private Button switchButton;
    private Button deleteButton;
    private Button backButton;

    protected NodeOperationScreen(WorldTree.TreeNode node) {
        super(Component.literal("Node Operation"));
        this.node = node;
    }

    @Override
    protected void init() {
        super.init();
        int centerX = this.width / 2;
        int buttonWidth = 100;
        int buttonHeight = 20;
        int spacing = 10;
        int totalHeight = buttonHeight * 3 + spacing * 2;

        // Calculate starting Y position (vertical center)
        int startY = (this.height - totalHeight) / 2;

        // Add "Switch" button
        switchButton = Button.builder(Component.literal("Switch"), button -> {
            // Move dummy node to current node
            WorldTree.moveDummyNode(node);
            // Load save
            WorldTree.loadSave(node.id);
            this.onClose();
        }).bounds(centerX - buttonWidth / 2, startY, buttonWidth, buttonHeight).build();
        addRenderableWidget(switchButton);

        // Add "Delete" button
        deleteButton = Button.builder(Component.literal("Delete"), button -> {
            WorldTree.removeSave(node.id);
            this.onClose();
        }).bounds(centerX - buttonWidth / 2, startY + buttonHeight + spacing, buttonWidth, buttonHeight).build();
        addRenderableWidget(deleteButton);

        // Add "Back" button
        backButton = Button.builder(Component.literal("Back"), button -> {
            this.onClose();
        }).bounds(centerX - buttonWidth / 2, startY + (buttonHeight + spacing) * 2, buttonWidth, buttonHeight).build();
        addRenderableWidget(backButton);
    }

    @SuppressWarnings("null")
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        // Draw title
        guiGraphics.drawCenteredString(font, "Operation: " + node.displayName, width / 2, 30, 0xFFFFFF);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(new WorldTreeScreen());
    }
}

// Dummy node operation interface
class DummyNodeScreen extends net.minecraft.client.gui.screens.Screen {
    private final WorldTree.TreeNode parentNode;
    private EditBox nameInput;
    private Button saveButton;
    private Button backButton;

    protected DummyNodeScreen(WorldTree.TreeNode parentNode) {
        super(Component.literal("Add New Node"));
        this.parentNode = parentNode;
    }

    @Override
    protected void init() {
        super.init();
        int centerX = this.width / 2;
        int buttonWidth = 100;
        int buttonHeight = 20;
        int spacing = 10;

        // Create text input box
        nameInput = new EditBox(font, centerX - 100, this.height / 2 - 30, 200, 20, Component.literal("Node name"));
        nameInput.setMaxLength(32);
        addRenderableWidget(nameInput);

        // Add "Save" button
        saveButton = Button.builder(Component.literal("Save"), button -> {
            String displayName = nameInput.getValue();
            if (!displayName.trim().isEmpty()) {
                // Add new node under parent
                WorldTree.addSave(parentNode.id, displayName);
                this.onClose();
            }
        }).bounds(centerX - buttonWidth / 2, this.height / 2, buttonWidth, buttonHeight).build();
        addRenderableWidget(saveButton);

        // Add "Back" button
        backButton = Button.builder(Component.literal("Back"), button -> {
            this.onClose();
        }).bounds(centerX - buttonWidth / 2, this.height / 2 + buttonHeight + spacing, buttonWidth, buttonHeight)
                .build();
        addRenderableWidget(backButton);

        // Set initial focus
        setInitialFocus(nameInput);
    }

    @SuppressWarnings("null")
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        // Draw title
        guiGraphics.drawCenteredString(font, "Add New Node to: " + parentNode.displayName, width / 2, 30, 0xFFFFFF);
        // Draw hint
        guiGraphics.drawCenteredString(font, "Enter new node name:", width / 2, this.height / 2 - 50, 0xFFFFFF);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(new WorldTreeScreen());
    }
}