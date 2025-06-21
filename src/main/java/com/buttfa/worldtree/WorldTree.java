package com.buttfa.worldtree;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Comparator;

import org.slf4j.Logger;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(WorldTree.MODID)
public class WorldTree {
    // Define mod id in a common place for everything to reference
    public static final String MODID = "worldtree";
    // Directly reference a slf4j logger
    private static final Logger LOGGER = LogUtils.getLogger();
    // Create a Deferred Register to hold Blocks which will all be registered under
    // the "worldtree" namespace
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MODID);
    // Create a Deferred Register to hold Items which will all be registered under
    // the "worldtree" namespace
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    // Create a Deferred Register to hold CreativeModeTabs which will all be
    // registered under the "worldtree" namespace
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister
            .create(Registries.CREATIVE_MODE_TAB, MODID);

    // Creates a new Block with the id "examplemod:example_block", combining the
    // namespace and path
    public static final RegistryObject<Block> EXAMPLE_BLOCK = BLOCKS.register("example_block",
            () -> new Block(BlockBehaviour.Properties.of()
                    .setId(BLOCKS.key("example_block"))
                    .mapColor(MapColor.STONE)));
    // Creates a new BlockItem with the id "examplemod:example_block", combining the
    // namespace and path
    public static final RegistryObject<Item> EXAMPLE_BLOCK_ITEM = ITEMS.register("example_block",
            () -> new BlockItem(EXAMPLE_BLOCK.get(), new Item.Properties().setId(ITEMS.key("example_block"))));

    // Creates a new food item with the id "examplemod:example_id", nutrition 1 and
    // saturation 2
    public static final RegistryObject<Item> EXAMPLE_ITEM = ITEMS.register("example_item",
            () -> new Item(new Item.Properties()
                    .setId(ITEMS.key("example_item"))
                    .food(new FoodProperties.Builder()
                            .alwaysEdible()
                            .nutrition(1)
                            .saturationModifier(2f)
                            .build())));

    // Creates a creative tab with the id "examplemod:example_tab" for the example
    // item, that is placed after the combat tab
    public static final RegistryObject<CreativeModeTab> EXAMPLE_TAB = CREATIVE_MODE_TABS.register("example_tab",
            () -> CreativeModeTab.builder()
                    .withTabsBefore(CreativeModeTabs.COMBAT)
                    .icon(() -> EXAMPLE_ITEM.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        output.accept(EXAMPLE_ITEM.get()); // Add the example item to the tab. For your own tabs, this
                                                           // method is preferred over the event
                    }).build());

    public WorldTree(FMLJavaModLoadingContext context) {
        IEventBus modEventBus = context.getModEventBus();

        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);

        // Register the Deferred Register to the mod event bus so blocks get registered
        BLOCKS.register(modEventBus);
        // Register the Deferred Register to the mod event bus so items get registered
        ITEMS.register(modEventBus);
        // Register the Deferred Register to the mod event bus so tabs get registered
        CREATIVE_MODE_TABS.register(modEventBus);

        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);

        // Register the item to a creative tab
        modEventBus.addListener(this::addCreative);

        // Register our mod's ForgeConfigSpec so that Forge can create and load the
        // config file for us
        context.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        // Some common setup code
        LOGGER.info("HELLO FROM COMMON SETUP");

        if (Config.logDirtBlock)
            LOGGER.info("DIRT BLOCK >> {}", ForgeRegistries.BLOCKS.getKey(Blocks.DIRT));

        LOGGER.info(Config.magicNumberIntroduction + Config.magicNumber);

        Config.items.forEach((item) -> LOGGER.info("ITEM >> {}", item.toString()));
    }

    // Add the example block item to the building blocks tab
    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS)
            event.accept(EXAMPLE_BLOCK_ITEM);
    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        // Do something when the server starts
        LOGGER.info("HELLO from server starting");
    }

    // You can use EventBusSubscriber to automatically register all static methods
    // in the class annotated with @SubscribeEvent
    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            // Some client setup code
            LOGGER.info("HELLO FROM CLIENT SETUP");
            LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());
        }
    }

    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class WorldTreeEvents {
        @SubscribeEvent
        public static void onScreenInit(ScreenEvent.Init event) {
            if (event.getScreen() instanceof PauseScreen pauseScreen) {

                // Find the last button on the pause page and add the 'World Tree' button below
                // it.
                List<? extends GuiEventListener> children = pauseScreen.children();
                for (int i = children.size() - 1; i >= 0; i--) {
                    GuiEventListener widget = children.get(i);
                    if (widget instanceof Button lastButton) {
                        // Add the 'World Tree' button below the last button
                        event.addListener(Button.builder(
                                // The text of the button
                                Component.translatable("World Tree"),
                                // The action when the button is clicked
                                button -> {
                                    // Check if player is null
                                    if (Minecraft.getInstance().player != null) {
                                        // Jump to the page of 'World Tree' mod
                                        WorldTree.loadWorldTree();
                                        Minecraft.getInstance().setScreen(new WorldTreeScreen());
                                    } else {
                                        LOGGER.warn("Player is null when the 'World Tree' button was clicked.");
                                    }
                                })
                                .bounds((// X
                                pauseScreen.width - lastButton.getWidth()) / 2,
                                        // Y
                                        lastButton.getY() + lastButton.getHeight() + 5,
                                        // Width
                                        lastButton.getWidth(),
                                        // Height
                                        20)
                                .build());
                        break;
                    }
                }
            }
        }
    }

    private static File worldDir = null;
    private static File worldTreeDir = null;
    private static File worldTreeFile = null;

    // File operation utility class
    private static class FileUtils {
        // Recursively copy a directory
        public static void copyDirectory(Path source, Path target) throws IOException {
            Files.walkFileTree(source, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    Path relative = source.relativize(dir);
                    Path dest = target.resolve(relative);
                    Files.createDirectories(dest);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Path relative = source.relativize(file);
                    Path dest = target.resolve(relative);
                    Files.copy(file, dest, StandardCopyOption.REPLACE_EXISTING);
                    return FileVisitResult.CONTINUE;
                }
            });
        }

        // Recursively delete a directory
        public static void deleteDirectory(Path directory) throws IOException {
            Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
        }
    }

    // World tree node data structure
    public static class TreeNode {
        public final String id; // Unique identifier (timestamp + name)
        public final String displayName; // User-friendly display name
        public Component name;
        public final List<TreeNode> children = new ArrayList<>();
        public TreeNode parent;
        public int depth;
        public int x, y; // Logical position of the node in the tree
        public int width = 100, height = 20; // Node dimensions
        public int subtreeWidth = 0; // Subtree width (including all child nodes)
        public boolean expanded = true; // Whether to expand child nodes
        public int padding = 10; // Text padding for dynamic width calculation
        public long timestamp; // Node creation timestamp
        public String folderName; // Folder name corresponding to the node

        public TreeNode(String id, String displayName) {
            this.id = id;
            this.displayName = displayName;
            this.name = Component.literal(displayName);
            this.timestamp = System.currentTimeMillis(); // Initialize timestamp
        }

        // Constructor allowing custom name
        public TreeNode(String id, String displayName, Component name) {
            this.id = id;
            this.displayName = displayName;
            this.name = name;
            this.timestamp = System.currentTimeMillis();
        }

        public void addChild(TreeNode child) {
            children.add(child);
            child.parent = this;
            child.depth = this.depth + 1;
            child.timestamp = System.currentTimeMillis(); // Set child node timestamp
        }

        public boolean isRoot() {
            return parent == null;
        }

        // Get node center X coordinate
        public int getCenterX() {
            return x + width / 2;
        }

        // Get node bottom Y coordinate
        public int getBottomY() {
            return y + height;
        }

        // Get node top Y coordinate
        public int getTopY() {
            return y;
        }
    }

    // World tree root node
    public static TreeNode rootNode;
    public static final Map<String, TreeNode> nodeMap = new HashMap<>();
    public static final String DUMMY_NODE_ID = "+ Add Save"; // Dummy node ID

    private static void updateWorldTreeInfo() {
        // Get the world name
        String worldName = Optional.ofNullable(Minecraft.getInstance().getSingleplayerServer())
                .map(server -> server.getWorldData().getLevelName())
                .orElse(null);
        if (worldName == null) {
            LOGGER.warn("World name is null.");
            return;
        }
        worldDir = new File(Minecraft.getInstance().gameDirectory, "saves/" + worldName);

        // Get the saves directory
        worldTreeDir = new File(
                Minecraft.getInstance().gameDirectory.toPath().resolve("saves").toString() + "/"
                        + worldName + "-WorldTree");
        if (!worldTreeDir.exists()) {
            worldTreeDir.mkdirs();
        }

        // World tree information file
        worldTreeFile = new File(worldTreeDir, "WorldTree.json");
        if (!worldTreeFile.exists()) {
            try {
                worldTreeFile.createNewFile();
            } catch (IOException e) {
                LOGGER.warn(e.getMessage());
            }
        }
    }

    public static void loadWorldTree() {
        // Update the world tree information
        updateWorldTreeInfo();

        // Check if the file exists
        if (worldTreeFile == null || !worldTreeFile.exists() || worldTreeFile.length() == 0) {
            LOGGER.info("World tree file not found or empty, using default tree");
            createDefaultTree();
            return;
        }

        try (FileReader reader = new FileReader(worldTreeFile)) {
            Gson gson = new Gson();
            Type type = new TypeToken<Map<String, Object>>() {
            }.getType();
            Map<String, Object> savedData = gson.fromJson(reader, type);

            if (savedData == null) {
                LOGGER.warn("World tree data is null, creating default tree");
                createDefaultTree();
                return;
            }

            // Clear the current tree structure
            nodeMap.clear();
            rootNode = null;

            // Read node data
            if (savedData.containsKey("nodes")) {
                Map<String, Map<String, Object>> nodesData = (Map<String, Map<String, Object>>) savedData.get("nodes");

                // First pass: create all nodes
                for (String nodeId : nodesData.keySet()) {
                    Map<String, Object> nodeData = nodesData.get(nodeId);
                    String displayName = (String) nodeData.get("displayName");
                    TreeNode node = new TreeNode(nodeId, displayName);
                    node.expanded = (boolean) nodeData.get("expanded");
                    // Read timestamp (if exists)
                    if (nodeData.containsKey("timestamp")) {
                        node.timestamp = ((Double) nodeData.get("timestamp")).longValue();
                    }
                    // Read folder name (if exists)
                    if (nodeData.containsKey("folderName")) {
                        node.folderName = (String) nodeData.get("folderName");
                    }
                    nodeMap.put(nodeId, node);

                    if (nodeId.equals("root")) {
                        rootNode = node;
                    }
                }

                // Second pass: establish parent-child relationships
                for (String nodeId : nodesData.keySet()) {
                    Map<String, Object> nodeData = nodesData.get(nodeId);
                    TreeNode node = nodeMap.get(nodeId);
                    String parentId = (String) nodeData.get("parentId");

                    if (parentId != null && !parentId.isEmpty()) {
                        TreeNode parent = nodeMap.get(parentId);
                        if (parent != null) {
                            parent.addChild(node);
                        } else {
                            LOGGER.warn("Parent node {} not found for {}", parentId, nodeId);
                        }
                    }
                }
            } else {
                LOGGER.warn("No nodes data found in world tree file, creating default tree");
                createDefaultTree();
                return;
            }

            // Ensure root node exists
            if (rootNode == null) {
                rootNode = new TreeNode("root", "root");
                nodeMap.put("root", rootNode);
                LOGGER.warn("Root node not found, created new root node");
            }

            // Check if dummy node exists
            if (!nodeMap.containsKey(DUMMY_NODE_ID)) {
                LOGGER.info("Dummy node not found, creating new dummy node as child of root");
                // Create dummy node with custom name
                TreeNode dummyNode = new TreeNode(DUMMY_NODE_ID, "+ Add New", Component.literal("+ Add New"));
                rootNode.addChild(dummyNode);
                nodeMap.put(DUMMY_NODE_ID, dummyNode);
            }

            LOGGER.info("World tree loaded successfully with {} nodes", nodeMap.size());
        } catch (Exception e) {
            LOGGER.error("Failed to load world tree: {}", e.getMessage());
            // Create default tree on load failure
            createDefaultTree();
        }
    }

    // Create default tree structure
    private static void createDefaultTree() {
        nodeMap.clear();
        rootNode = new TreeNode("root", "root");
        nodeMap.put("root", rootNode);

        // Add dummy node (with custom name)
        TreeNode dummyNode = new TreeNode(DUMMY_NODE_ID, "+ Add New", Component.literal("+ Add New"));
        rootNode.addChild(dummyNode);
        nodeMap.put(DUMMY_NODE_ID, dummyNode);

        // Create folder for root node and copy the current world
        copyWorldForNode(rootNode);

        saveWorldTree();

        LOGGER.info("Created default world tree with root and dummy node");
    }

    // Copy current world for node
    private static void copyWorldForNode(TreeNode node) {
        if (worldDir == null || !worldDir.exists()) {
            LOGGER.warn("World directory not found, cannot copy for node: {}", node.id);
            return;
        }

        // Generate node folder name: node name-timestamp
        node.folderName = node.displayName + "-" + System.currentTimeMillis();
        Path targetPath = worldTreeDir.toPath().resolve(node.folderName);

        try {
            // Save current world
            if (Minecraft.getInstance().getSingleplayerServer() != null) {
                Minecraft.getInstance().getSingleplayerServer().saveAllChunks(true, true, true);
            }

            // Copy world
            FileUtils.copyDirectory(worldDir.toPath(), targetPath);
            LOGGER.info("Copied world to node folder: {}", targetPath);
        } catch (IOException e) {
            LOGGER.error("Failed to copy world for node {}: {}", node.id, e.getMessage());
        }
    }

    public static void saveWorldTree() {
        // Update the world tree information
        updateWorldTreeInfo();

        if (worldTreeFile == null) {
            LOGGER.warn("World tree file is null, cannot save");
            return;
        }

        try (FileWriter writer = new FileWriter(worldTreeFile)) {
            Gson gson = new Gson();
            Map<String, Object> saveData = new HashMap<>();

            // Save node data
            Map<String, Map<String, Object>> nodesData = new HashMap<>();
            for (TreeNode node : nodeMap.values()) {
                Map<String, Object> nodeData = new HashMap<>();
                nodeData.put("expanded", node.expanded);
                nodeData.put("timestamp", node.timestamp); // Save timestamp
                nodeData.put("folderName", node.folderName); // Save folder name
                nodeData.put("displayName", node.displayName); // Save display name

                // Record parent node ID (root has no parent)
                if (node.parent != null) {
                    nodeData.put("parentId", node.parent.id);
                } else {
                    nodeData.put("parentId", "");
                }

                nodesData.put(node.id, nodeData);
            }
            saveData.put("nodes", nodesData);

            gson.toJson(saveData, writer);
            LOGGER.info("World tree saved successfully with {} nodes", nodeMap.size());
        } catch (IOException e) {
            LOGGER.error("Failed to save world tree: {}", e.getMessage());
        }
    }

    public static void addSave(String parentId, String displayName) {
        TreeNode parent = nodeMap.get(parentId);
        if (parent != null) {
            // Generate unique ID: timestamp + display name
            String uniqueId = System.currentTimeMillis() + "-" + displayName;
            TreeNode newNode = new TreeNode(uniqueId, displayName);
            parent.addChild(newNode);
            nodeMap.put(uniqueId, newNode);

            // Create folder for node and copy current world
            copyWorldForNode(newNode);

            // Move dummy node to become child of new node
            TreeNode dummyNode = nodeMap.get(DUMMY_NODE_ID);
            if (dummyNode != null) {
                // Remove from original parent
                if (dummyNode.parent != null) {
                    dummyNode.parent.children.remove(dummyNode);
                }
                // Add under new node
                newNode.addChild(dummyNode);
            }

            saveWorldTree();
        }
    }

    public static void loadSave(String saveId) {
        TreeNode node = nodeMap.get(saveId);
        if (node != null && node.folderName != null) {
            switchWorld(node);
        } else {
            LOGGER.warn("Cannot load save: node not found or no folder name");
        }
    }

    // Recursively collect node and all its children (excluding dummy node)
    private static List<TreeNode> collectAllChildrenExcludingDummy(TreeNode node) {
        List<TreeNode> nodes = new ArrayList<>();
        // Add current node if not dummy
        if (!node.id.equals(DUMMY_NODE_ID)) {
            nodes.add(node);
            // Recursively collect all non-dummy children
            for (TreeNode child : node.children) {
                if (!child.id.equals(DUMMY_NODE_ID)) {
                    nodes.addAll(collectAllChildrenExcludingDummy(child));
                }
            }
        }
        return nodes;
    }

    public static void removeSave(String saveId) {
        TreeNode node = nodeMap.get(saveId);
        if (node != null && !node.isRoot()) {
            // Collect all nodes to be deleted (including current node and all non-dummy
            // children)
            List<TreeNode> nodesToRemove = collectAllChildrenExcludingDummy(node);

            // Remove current node from parent's children list
            node.parent.children.remove(node);

            // Iterate over all nodes to be deleted
            for (TreeNode nodeToRemove : nodesToRemove) {
                // Remove from node map
                nodeMap.remove(nodeToRemove.id);

                // Delete corresponding folder
                if (nodeToRemove.folderName != null) {
                    Path folderPath = worldTreeDir.toPath().resolve(nodeToRemove.folderName);
                    try {
                        if (Files.exists(folderPath)) {
                            FileUtils.deleteDirectory(folderPath);
                            LOGGER.info("Deleted node folder: {}", folderPath);
                        }
                    } catch (IOException e) {
                        LOGGER.error("Failed to delete node folder: {}", e.getMessage());
                    }
                }
            }

            saveWorldTree();
        }
    }

    // Move dummy node to specified node
    public static void moveDummyNode(TreeNode newParent) {
        TreeNode dummyNode = nodeMap.get(DUMMY_NODE_ID);
        if (dummyNode == null) {
            LOGGER.warn("Dummy node not found");
            return;
        }

        // Remove from original parent
        if (dummyNode.parent != null) {
            dummyNode.parent.children.remove(dummyNode);
        }

        // Add to new parent
        newParent.addChild(dummyNode);
        saveWorldTree();
        LOGGER.info("Moved dummy node to {}", newParent.id);
    }

    // Switch to world of specified node
    public static void switchWorld(TreeNode node) {
        if (node == null || node.folderName == null) {
            LOGGER.warn("Cannot switch world: invalid node");
            return;
        }

        Path sourcePath = worldTreeDir.toPath().resolve(node.folderName);
        if (!Files.exists(sourcePath)) {
            LOGGER.warn("Node folder not found: {}", sourcePath);
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        String worldName = Optional.ofNullable(mc.getSingleplayerServer())
                .map(server -> server.getWorldData().getLevelName())
                .orElse("unknown_world");

        Path savesDir = mc.gameDirectory.toPath().resolve("saves");
        Path targetDir = savesDir.resolve(worldName);

        try {
            // Save current world
            if (mc.getSingleplayerServer() != null) {
                mc.getSingleplayerServer().saveAllChunks(true, true, true);
            }

            // Delete current world content
            if (Files.exists(targetDir)) {
                FileUtils.deleteDirectory(targetDir);
                LOGGER.info("Deleted current world: {}", targetDir);
            }

            // Copy node world to current world
            FileUtils.copyDirectory(sourcePath, targetDir);
            LOGGER.info("Copied node world to current: {}", targetDir);

            // Reload world
            mc.execute(() -> {
                if (mc.level != null) {
                    mc.level.disconnect();
                }

                try {
                    // Reopen world
                    mc.createWorldOpenFlows().openWorld(worldName, null);
                    LOGGER.info("World reloaded: {}", worldName);
                } catch (Exception e) {
                    LOGGER.error("Failed to reload world: ", e);
                }
            });

        } catch (IOException e) {
            LOGGER.error("Failed to switch world: ", e);
        }
    }
}

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