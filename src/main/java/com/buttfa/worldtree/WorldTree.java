package com.buttfa.worldtree;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
                @SuppressWarnings("unchecked")
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
    @SuppressWarnings("null")
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
    @SuppressWarnings("null")
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
