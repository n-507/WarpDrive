package cr0s.warpdrive.core;

import javax.annotation.Nonnull;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Field;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;

public class ClassTransformer implements net.minecraft.launchwrapper.IClassTransformer {
	
	private static final HashMap<String, String> nodeMap = new HashMap<>();
	public static ConcurrentSkipListMap<String, Integer> countClass = new ConcurrentSkipListMap<>();
	public static ConcurrentHashMap<String, Long> sizeClass = new ConcurrentHashMap<>(8192);
	
	private static final String GRAVITY_MANAGER_CLASS = "cr0s/warpdrive/data/GravityManager";
	private static final String CLOAK_MANAGER_CLASS = "cr0s/warpdrive/data/CloakManager";
	
	private static final boolean debugLog = false;
	private static final String ASM_DUMP_BEFORE = "asm/warpdrive.before";
	private static final String ASM_DUMP_AFTER  = "asm/warpdrive.after";
	private static final String ASM_DUMP_FAILED = "asm/warpdrive.failed";
	
	public ClassTransformer() {
		nodeMap.put("EntityLivingBase.class", "vn");
		nodeMap.put("travel.name", "func_191986_a");
		nodeMap.put("travel.desc", "(FFF)V");
		
		nodeMap.put("EntityItem.class", "acj");
		nodeMap.put("onUpdate.name", "func_70071_h_");
		nodeMap.put("onUpdate.desc", "()V");
		
		nodeMap.put("WorldClient.class", "brz");
		nodeMap.put("invalidateRegionAndSetBlock.name", "func_180503_b");
		nodeMap.put("invalidateRegionAndSetBlock.desc", "(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/state/IBlockState;)Z");
		nodeMap.put("setBlockState.name", "func_180501_a");
		
		nodeMap.put("Chunk.class", "axu");
		nodeMap.put("read.name", "func_186033_a");
		nodeMap.put("read.desc", "(Lnet/minecraft/network/PacketBuffer;IZ)V");
		nodeMap.put("generateHeightMap.name", "func_76590_a");
		nodeMap.put("generateHeightMap.desc", "()V");
		
		nodeMap.put("AdvancementManager.class", "nq");
		nodeMap.put("loadBuiltInAdvancements.name", "func_192777_a");
		nodeMap.put("loadBuiltInAdvancements.desc", "(Ljava/util/Map;)V");
		
		nodeMap.put("ForgeHooks.class", "ForgeHooks");
		nodeMap.put("loadAdvancements.name", "lambda$loadAdvancements$0");
		nodeMap.put("loadAdvancements.desc", "(Lnet/minecraftforge/fml/common/ModContainer;Ljava/util/Map;Ljava/nio/file/Path;Ljava/nio/file/Path;)Ljava/lang/Boolean;");
	}
	
	@Override
	public byte[] transform(@Nonnull final String name, @Nonnull final String transformedName, final byte[] bytesOld) {
		if (bytesOld == null) {
			if (debugLog) {
				FMLLoadingPlugin.logger.trace(String.format("bytes is null, transformation cancelled for %s", name));
			}
			return null;
		}
		
		if (debugLog) {
			// FMLLoadingPlugin.logger.info(String.format("Checking %s", name));
			saveClassToFile(ASM_DUMP_BEFORE, transformedName, bytesOld);
		}
		
		final byte[] bytesNew;
		switch (transformedName) {
		case "net.minecraft.entity.EntityLivingBase":
			bytesNew = transformMinecraftEntityLivingBase(bytesOld);
			break;
			
		case "net.minecraft.entity.item.EntityItem":
			bytesNew = transformMinecraftEntityItem(bytesOld);
			break;
			
		case "com.creativemd.itemphysic.physics.ServerPhysic":
			bytesNew = transformItemPhysicEntityItem(bytesOld);
			break;
			
		case "micdoodle8.mods.galacticraft.core.util.WorldUtil":
			bytesNew = transformGalacticraftWorldUtil(bytesOld);
			break;
			
		case "net.minecraft.client.multiplayer.WorldClient":
			bytesNew = transformMinecraftWorldClient(bytesOld);
			break;
			
		case "net.minecraft.world.chunk.Chunk":
			bytesNew = transformMinecraftChunk(bytesOld);
			break;
			
		case "net.minecraft.advancements.AdvancementManager":
			bytesNew = transformMinecraftAdvancementManager(bytesOld);
			break;
			
		case "net.minecraftforge.common.ForgeHooks":
			bytesNew = transformMinecraftForgeHooks(bytesOld);
			break;
			
		default:
			bytesNew = null;
		}
		
		try {
			collectClientValidation(transformedName, bytesOld);
		} catch (final Exception exception) {
			// nop
		}
		
		if (bytesNew == null) {// ignored that class
			return bytesOld;
		}
		if (bytesNew == bytesOld) {// transformation failed
			saveClassToFile(ASM_DUMP_FAILED, transformedName, bytesOld);
			return bytesOld;
		}
		
		// all good
		saveClassToFile(ASM_DUMP_AFTER, transformedName, bytesNew);
		return bytesNew;
	}
	
	private static void collectClientValidation(@Nonnull final String transformedName, @Nonnull final byte[] bytes) {
		final String[] names = transformedName.split("[.$]");
		final String shortName = names[0] + "." + (names.length > 1 ? names[1] : "");
		Integer count = countClass.get(shortName);
		Long size = sizeClass.get(shortName);
		if (count == null) {
			count = 0;
			size = 0L;
		}
		countClass.put(shortName, count + 1);
		sizeClass.put(shortName, size + bytes.length);
	}
	
	public static String getClientValidation() {
		final StringBuilder result = new StringBuilder().append(new Date().toString());
		for (final String key : countClass.keySet()) {
			result.append("\n").append(key)
			      .append("\t").append(countClass.get(key))
			      .append("\t").append(sizeClass.get(key));
		}
		return result.toString();
	}
	
	private byte[] transformMinecraftEntityLivingBase(@Nonnull final byte[] bytes) {
		final ClassNode classNode = new ClassNode();
		final ClassReader classReader = new ClassReader(bytes);
		classReader.accept(classNode, 0);
		
		final int countExpected = 2;
		int countTransformed = 0;
		for (final MethodNode methodNode : classNode.methods) {
			// if (debugLog) { FMLLoadingPlugin.logger.info(String.format("- Method %s %s", methodNode.name, methodNode.desc)); }
			
			if ( (methodNode.name.equals(nodeMap.get("travel.name")) || methodNode.name.equals("travel"))
			  && methodNode.desc.equals(nodeMap.get("travel.desc")) ) {
				FMLLoadingPlugin.logger.debug(String.format("Found method to transform: %s %s",
				                                           methodNode.name, methodNode.desc));
				
				int indexInstruction = 0;
				
				while (indexInstruction < methodNode.instructions.size()) {
					final AbstractInsnNode abstractNode = methodNode.instructions.get(indexInstruction);
					
					if (abstractNode instanceof LdcInsnNode) {
						final LdcInsnNode nodeAt = (LdcInsnNode) abstractNode;
						
						if (nodeAt.cst.equals(-0.080000000000000002D)) {
							final VarInsnNode beforeNode = new VarInsnNode(Opcodes.ALOAD, 0);
							final MethodInsnNode overwriteNode = new MethodInsnNode(
									Opcodes.INVOKESTATIC,
									GRAVITY_MANAGER_CLASS,
									"getNegGravityForEntity",
									"(Lnet/minecraft/entity/Entity;)D",
									false);
							methodNode.instructions.insertBefore(nodeAt, beforeNode);
							methodNode.instructions.set(nodeAt, overwriteNode);
							if (debugLog) { FMLLoadingPlugin.logger.info(String.format("Injecting into %s.%s %s", classNode.name, methodNode.name, methodNode.desc)); }
							countTransformed++;
							
						} else if (nodeAt.cst.equals(0.080000000000000002D)) {
							final VarInsnNode beforeNode = new VarInsnNode(Opcodes.ALOAD, 0);
							final MethodInsnNode overwriteNode = new MethodInsnNode(
									Opcodes.INVOKESTATIC,
									GRAVITY_MANAGER_CLASS,
									"getGravityForEntity",
									"(Lnet/minecraft/entity/Entity;)D",
									false);
							methodNode.instructions.insertBefore(nodeAt, beforeNode);
							methodNode.instructions.set(nodeAt, overwriteNode);
							if (debugLog) { FMLLoadingPlugin.logger.info(String.format("Injecting into %s.%s %s", classNode.name, methodNode.name, methodNode.desc)); }
							countTransformed++;
						}
					}
					
					indexInstruction++;
				}
			}
		}
		
		if (countTransformed != countExpected) {
			FMLLoadingPlugin.logger.error(String.format("Transformation failed for %s (%d/%d), aborting...", classNode.name, countTransformed, countExpected));
			return bytes;
		}
		
		final ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS); // | ClassWriter.COMPUTE_FRAMES);
		classNode.accept(writer);
		final byte[] bytesNew = writer.toByteArray();
		FMLLoadingPlugin.logger.info(String.format("Successful injection in %s", classNode.name));
		return bytesNew;
	}
	
	private byte[] transformMinecraftEntityItem(@Nonnull final byte[] bytes) {
		final ClassNode classNode = new ClassNode();
		final ClassReader classReader = new ClassReader(bytes);
		classReader.accept(classNode, 0);
		
		final int countExpected = 2;
		int countTransformed = 0;
		for (final MethodNode methodNode : classNode.methods) {
			// if (debugLog) { FMLLoadingPlugin.logger.info(String.format("- Method %s %s", methodNode.name, methodNode.desc)); }
			
			if ( (methodNode.name.equals(nodeMap.get("onUpdate.name")) || methodNode.name.equals("onUpdate"))
			  && methodNode.desc.equals(nodeMap.get("onUpdate.desc")) ) {
				FMLLoadingPlugin.logger.debug(String.format("Found method to transform: %s %s",
				                                           methodNode.name, methodNode.desc));
				
				int indexInstruction = 0;
				
				while (indexInstruction < methodNode.instructions.size()) {
					final AbstractInsnNode abstractNode = methodNode.instructions.get(indexInstruction);
					if (debugLog) { decompile(abstractNode); }
					
					if (abstractNode instanceof LdcInsnNode) {
						final LdcInsnNode nodeAt = (LdcInsnNode) abstractNode;
						
						if (nodeAt.cst.equals(0.039999999105930328D)) {
							final VarInsnNode beforeNode = new VarInsnNode(Opcodes.ALOAD, 0);
							final MethodInsnNode overwriteNode = new MethodInsnNode(
									Opcodes.INVOKESTATIC,
									GRAVITY_MANAGER_CLASS,
									"getItemGravity",
									"(L" + "net/minecraft/entity/item/EntityItem" + ";)D",
									false);
							methodNode.instructions.insertBefore(nodeAt, beforeNode);
							methodNode.instructions.set(nodeAt, overwriteNode);
							if (debugLog) { FMLLoadingPlugin.logger.info(String.format("Injecting into %s.%s %s", classNode.name, methodNode.name, methodNode.desc)); }
							countTransformed++;
						}
						
						if (nodeAt.cst.equals(0.98000001907348633D)) {
							final VarInsnNode beforeNode = new VarInsnNode(Opcodes.ALOAD, 0);
							final MethodInsnNode overwriteNode = new MethodInsnNode(
									Opcodes.INVOKESTATIC,
									GRAVITY_MANAGER_CLASS,
									"getItemGravity2",
									"(L" + "net/minecraft/entity/item/EntityItem" + ";)D",
									false);
							methodNode.instructions.insertBefore(nodeAt, beforeNode);
							methodNode.instructions.set(nodeAt, overwriteNode);
							if (debugLog) { FMLLoadingPlugin.logger.info(String.format("Injecting into %s.%s %s", classNode.name, methodNode.name, methodNode.desc)); }
							countTransformed++;
						}
					}
					
					indexInstruction++;
				}
			}
		}
		
		if (countTransformed != countExpected) {
			FMLLoadingPlugin.logger.error(String.format("Transformation failed for %s (%d/%d), aborting...", classNode.name, countTransformed, countExpected));
			return bytes;
		}
		
		final ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS); // | ClassWriter.COMPUTE_FRAMES);
		classNode.accept(writer);
		final byte[] bytesNew = writer.toByteArray();
		FMLLoadingPlugin.logger.info(String.format("Successful injection in %s", classNode.name));
		return bytesNew;
	}
	
	private byte[] transformItemPhysicEntityItem(@Nonnull final byte[] bytes) {
		final ClassNode classNode = new ClassNode();
		final ClassReader classReader = new ClassReader(bytes);
		classReader.accept(classNode, 0);
		
		final int countExpected = 2;
		int countTransformed = 0;
		for (final MethodNode methodNode : classNode.methods) {
			// if (debugLog) { FMLLoadingPlugin.logger.info(String.format("- Method %s %s", methodNode.name, methodNode.desc)); }
			
			if ( (methodNode.name.equals("update"))
			  && methodNode.desc.equals("(Lnet/minecraft/entity/item/EntityItem;)V") ) {
				FMLLoadingPlugin.logger.debug(String.format("Found method to transform: %s %s",
				                                           methodNode.name, methodNode.desc));
				
				int indexInstruction = 0;
				
				while (indexInstruction < methodNode.instructions.size()) {
					final AbstractInsnNode abstractNode = methodNode.instructions.get(indexInstruction);
					
					if (abstractNode instanceof LdcInsnNode) {
						final LdcInsnNode nodeAt = (LdcInsnNode) abstractNode;
						
						if (nodeAt.cst.equals(0.04D)) {
							final VarInsnNode beforeNode = new VarInsnNode(Opcodes.ALOAD, 0);
							final MethodInsnNode overwriteNode = new MethodInsnNode(
									Opcodes.INVOKESTATIC,
									GRAVITY_MANAGER_CLASS,
									"getItemGravity",
									"(L" + "net/minecraft/entity/item/EntityItem" + ";)D",
									false);
							methodNode.instructions.insertBefore(nodeAt, beforeNode);
							methodNode.instructions.set(nodeAt, overwriteNode);
							if (debugLog) { FMLLoadingPlugin.logger.info(String.format("Injecting into %s.%s %s", classNode.name, methodNode.name, methodNode.desc)); }
							countTransformed++;
						}
						
						if (nodeAt.cst.equals(0.98D)) {
							final VarInsnNode beforeNode = new VarInsnNode(Opcodes.ALOAD, 0);
							final MethodInsnNode overwriteNode = new MethodInsnNode(
									Opcodes.INVOKESTATIC,
									GRAVITY_MANAGER_CLASS,
									"getItemGravity2",
									"(L" + "net/minecraft/entity/item/EntityItem" + ";)D",
									false);
							methodNode.instructions.insertBefore(nodeAt, beforeNode);
							methodNode.instructions.set(nodeAt, overwriteNode);
							if (debugLog) { FMLLoadingPlugin.logger.info(String.format("Injecting into %s.%s %s", classNode.name, methodNode.name, methodNode.desc)); }
							countTransformed++;
						}
					}
					
					indexInstruction++;
				}
			}
		}
		
		if (countTransformed != countExpected) {
			FMLLoadingPlugin.logger.error(String.format("Transformation failed for %s (%d/%d), aborting...", classNode.name, countTransformed, countExpected));
			return bytes;
		}
		
		final ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS); // | ClassWriter.COMPUTE_FRAMES);
		classNode.accept(writer);
		final byte[] bytesNew = writer.toByteArray();
		FMLLoadingPlugin.logger.info(String.format("Successful injection in %s", classNode.name));
		return bytesNew;
	}
	
	private byte[] transformGalacticraftWorldUtil(@Nonnull final byte[] bytes) {
		final ClassNode classNode = new ClassNode();
		final ClassReader classReader = new ClassReader(bytes);
		classReader.accept(classNode, 0);
		
		final int countExpected = 5; // 3 + 2 + 0
		int countTransformed = 0;
		for (final MethodNode methodNode : classNode.methods) {
			// if (debugLog) { FMLLoadingPlugin.logger.info(String.format("- Method %s %s", methodNode.name, methodNode.desc)); }

			// Entities gravity
			if ( (methodNode.name.equals("getGravityForEntity"))
			  && methodNode.desc.equals("(Lnet/minecraft/entity/Entity;)D") ) {
				FMLLoadingPlugin.logger.debug(String.format("Found method to transform: %s %s",
				                                           methodNode.name, methodNode.desc));
				
				int indexInstruction = 0;
				
				while (indexInstruction < methodNode.instructions.size()) {
					final AbstractInsnNode abstractNode = methodNode.instructions.get(indexInstruction);
					
					if (abstractNode instanceof LdcInsnNode) {
						final LdcInsnNode nodeAt = (LdcInsnNode) abstractNode;
						
						if (nodeAt.cst.equals(0.08D)) {
							final VarInsnNode beforeNode = new VarInsnNode(Opcodes.ALOAD, 0);
							final MethodInsnNode overwriteNode = new MethodInsnNode(
									Opcodes.INVOKESTATIC,
									GRAVITY_MANAGER_CLASS,
									"getGravityForEntity",
									"(Lnet/minecraft/entity/Entity;)D",
									false);
							methodNode.instructions.insertBefore(nodeAt, beforeNode);
							methodNode.instructions.set(nodeAt, overwriteNode);
							if (debugLog) { FMLLoadingPlugin.logger.info(String.format("Injecting into %s.%s %s", classNode.name, methodNode.name, methodNode.desc)); }
							countTransformed++;
						}
					}
					
					indexInstruction++;
				}
			}
			
			// Items gravity
			if ( (methodNode.name.equals("getItemGravity"))
			  && methodNode.desc.equals("(Lnet/minecraft/entity/item/EntityItem;)D") ) {
				FMLLoadingPlugin.logger.debug(String.format("Found method to transform: %s %s",
				                                           methodNode.name, methodNode.desc));
				
				int indexInstruction = 0;
				
				while (indexInstruction < methodNode.instructions.size()) {
					final AbstractInsnNode abstractNode = methodNode.instructions.get(indexInstruction);
					
					if (abstractNode instanceof LdcInsnNode) {
						final LdcInsnNode nodeAt = (LdcInsnNode) abstractNode;
						
						if (nodeAt.cst.equals(0.03999999910593033D)) {
							final VarInsnNode beforeNode = new VarInsnNode(Opcodes.ALOAD, 0);
							final MethodInsnNode overwriteNode = new MethodInsnNode(
									Opcodes.INVOKESTATIC,
									GRAVITY_MANAGER_CLASS,
									"getItemGravity",
									"(L" + "net/minecraft/entity/item/EntityItem" + ";)D",
									false);
							methodNode.instructions.insertBefore(nodeAt, beforeNode);
							methodNode.instructions.set(nodeAt, overwriteNode);
							if (debugLog) { FMLLoadingPlugin.logger.info(String.format("Injecting into %s.%s %s", classNode.name, methodNode.name, methodNode.desc)); }
							countTransformed++;
						}
						/*
						if (nodeAt.cst.equals(Double.valueOf(0.98D))) {
							final VarInsnNode beforeNode = new VarInsnNode(Opcodes.ALOAD, 0);
							final MethodInsnNode overwriteNode = new MethodInsnNode(
									Opcodes.INVOKESTATIC,
									GRAVITY_MANAGER_CLASS,
									"getItemGravity2",
									"(L" + "net/minecraft/entity/item/EntityItem" + ";)D",
									false);
							methodNode.instructions.insertBefore(nodeAt, beforeNode);
							methodNode.instructions.set(nodeAt, overwriteNode);
							if (debugLog) { FMLLoadingPlugin.logger.info(String.format("Injecting into %s.%s %s", classNode.name, methodNode.name, methodNode.desc)); }
							countTransformed++;
						}
						/**/
					}
					
					indexInstruction++;
				}
			}
		}
		
		if (countTransformed != countExpected) {
			FMLLoadingPlugin.logger.error(String.format("Transformation failed for %s (%d/%d), aborting...", classNode.name, countTransformed, countExpected));
			return bytes;
		}
		
		final ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS); // | ClassWriter.COMPUTE_FRAMES);
		classNode.accept(writer);
		final byte[] bytesNew = writer.toByteArray();
		FMLLoadingPlugin.logger.info(String.format("Successful injection in %s", classNode.name));
		return bytesNew;
	}
	
	private byte[] transformMinecraftWorldClient(@Nonnull final byte[] bytes) {
		final ClassNode classNode = new ClassNode();
		final ClassReader classReader = new ClassReader(bytes);
		classReader.accept(classNode, 0);
		
		final int countExpected = 1;
		int countTransformed = 0;
		for (final MethodNode methodNode : classNode.methods) {
			// if (debugLog) { FMLLoadingPlugin.logger.info(String.format("- Method %s %s", methodNode.name, methodNode.desc)); }
			
			if ( (methodNode.name.equals(nodeMap.get("invalidateRegionAndSetBlock.name")) || methodNode.name.equals("invalidateRegionAndSetBlock"))
			  && methodNode.desc.equals(nodeMap.get("invalidateRegionAndSetBlock.desc")) ) {
				FMLLoadingPlugin.logger.debug(String.format("Found method to transform: %s %s",
				                                           methodNode.name, methodNode.desc));
				
				int indexInstruction = 0;
				
				while (indexInstruction < methodNode.instructions.size()) {
					final AbstractInsnNode abstractNode = methodNode.instructions.get(indexInstruction);
					
					if (abstractNode instanceof MethodInsnNode) {
						final MethodInsnNode nodeAt = (MethodInsnNode) abstractNode;
						
						if (nodeAt.name.equals(nodeMap.get("setBlockState.name")) || nodeAt.name.equals("setBlockState")) {
							final MethodInsnNode overwriteNode = new MethodInsnNode(
									Opcodes.INVOKESTATIC,
									CLOAK_MANAGER_CLASS,
									"WorldClient_invalidateRegionAndSetBlock_setBlockState",
									"(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/state/IBlockState;I)Z",
									false);
							methodNode.instructions.set(nodeAt, overwriteNode);
							if (debugLog) { FMLLoadingPlugin.logger.info(String.format("Injecting into %s.%s %s", classNode.name, methodNode.name, methodNode.desc)); }
							countTransformed++;
						}
					}
					
					indexInstruction++;
				}
			}
		}
		
		if (countTransformed != countExpected) {
			FMLLoadingPlugin.logger.error(String.format("Transformation failed for %s (%d/%d), aborting...", classNode.name, countTransformed, countExpected));
			return bytes;
		}
		
		final ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS); // | ClassWriter.COMPUTE_FRAMES);
		classNode.accept(writer);
		final byte[] bytesNew = writer.toByteArray();
		FMLLoadingPlugin.logger.info(String.format("Successful injection in %s", classNode.name));
		return bytesNew;
	}
	
	private byte[] transformMinecraftChunk(@Nonnull final byte[] bytes) {
		final ClassNode classNode = new ClassNode();
		final ClassReader classReader = new ClassReader(bytes);
		classReader.accept(classNode, 0);
		
		final int countExpected = 1;
		int countTransformed = 0;
		for (final MethodNode methodNode : classNode.methods) {
			if (debugLog) { FMLLoadingPlugin.logger.info(String.format("- Method %s %s", methodNode.name, methodNode.desc)); }
			
			if ( (methodNode.name.equals(nodeMap.get("read.name")) || methodNode.name.equals("read"))
			  && methodNode.desc.equals(nodeMap.get("read.desc")) ) {
				FMLLoadingPlugin.logger.debug(String.format("Found method to transform: %s %s",
				                                           methodNode.name, methodNode.desc));
				
				int indexInstruction = 0;
				
				while (indexInstruction < methodNode.instructions.size()) {
					final AbstractInsnNode abstractNode = methodNode.instructions.get(indexInstruction);
					if (debugLog) { decompile(abstractNode); }
					
					if (abstractNode instanceof MethodInsnNode) {
						final MethodInsnNode nodeAt = (MethodInsnNode) abstractNode;
						
						if ( (nodeAt.name.equals(nodeMap.get("generateHeightMap.name")) || nodeAt.name.equals("generateHeightMap"))
						  && nodeAt.desc.equals(nodeMap.get("generateHeightMap.desc")) ) {
							final MethodInsnNode insertMethodNode = new MethodInsnNode(
									Opcodes.INVOKESTATIC,
									CLOAK_MANAGER_CLASS,
									"Chunk_read",
									"(Lnet/minecraft/world/chunk/Chunk;)V",
									false);
							methodNode.instructions.insertBefore(nodeAt, insertMethodNode);
							indexInstruction++;
							
							final VarInsnNode insertVarNode = new VarInsnNode(Opcodes.ALOAD, 0);
							methodNode.instructions.insertBefore(nodeAt, insertVarNode);
							indexInstruction++;
							
							if (debugLog) { FMLLoadingPlugin.logger.info(String.format("Injecting into %s.%s %s", classNode.name, methodNode.name, methodNode.desc)); }
							countTransformed++;
						}
					}
					
					indexInstruction++;
				}
			}
		}
		
		if (countTransformed != countExpected) {
			FMLLoadingPlugin.logger.error(String.format("Transformation failed for %s (%d/%d), aborting...", classNode.name, countTransformed, countExpected));
			return bytes;
		}
		
		final ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS); // | ClassWriter.COMPUTE_FRAMES);
		classNode.accept(writer);
		final byte[] bytesNew = writer.toByteArray();
		FMLLoadingPlugin.logger.info(String.format("Successful injection in %s", classNode.name));
		return bytesNew;
	}
	
	private byte[] transformMinecraftAdvancementManager(@Nonnull final byte[] bytes) {
		final ClassNode classNode = new ClassNode();
		final ClassReader classReader = new ClassReader(bytes);
		classReader.accept(classNode, 0);
		
		final int countExpected = 1;
		int countTransformed = 0;
		for (final MethodNode methodNode : classNode.methods) {
			// if (debugLog) { FMLLoadingPlugin.logger.info(String.format("- Method %s %s", methodNode.name, methodNode.desc)); }
			
			if ( (methodNode.name.equals(nodeMap.get("loadBuiltInAdvancements.name")) || methodNode.name.equals("loadBuiltInAdvancements"))
			  && methodNode.desc.equals(nodeMap.get("loadBuiltInAdvancements.desc")) ) {
				FMLLoadingPlugin.logger.debug(String.format("Found method to transform: %s %s",
				                                           methodNode.name, methodNode.desc));
				
				int indexInstruction = 0;
				
				while (indexInstruction < methodNode.instructions.size()) {
					final AbstractInsnNode abstractNode = methodNode.instructions.get(indexInstruction);
					if (debugLog) { decompile(abstractNode); }
					
					if (abstractNode instanceof LdcInsnNode) {
						final LdcInsnNode nodeAt = (LdcInsnNode) abstractNode;
						
						if ( nodeAt.cst instanceof String
						  && ((String) nodeAt.cst).contains("Parsing error loading built-in advancement ") ) {
							final AbstractInsnNode abstractNodeToRemove = methodNode.instructions.get(indexInstruction - 4);
							if ( (abstractNodeToRemove instanceof FieldInsnNode)
							  && ((FieldInsnNode) abstractNodeToRemove).desc.equals("Lorg/apache/logging/log4j/Logger;") ) {
								indexInstruction -= 4;
								removeInstruction(methodNode, indexInstruction);
								removeInstruction(methodNode, indexInstruction);
								removeInstruction(methodNode, indexInstruction);
								removeInstruction(methodNode, indexInstruction);
								removeInstruction(methodNode, indexInstruction);
								removeInstruction(methodNode, indexInstruction);
								removeInstruction(methodNode, indexInstruction);
								removeInstruction(methodNode, indexInstruction);
								removeInstruction(methodNode, indexInstruction);
								removeInstruction(methodNode, indexInstruction);
								removeInstruction(methodNode, indexInstruction);
								indexInstruction--;
								
								if (debugLog) { FMLLoadingPlugin.logger.info(String.format("Injecting into %s.%s %s", classNode.name, methodNode.name, methodNode.desc)); }
								countTransformed++;
							}
						}
					}
					
					indexInstruction++;
				}
			}
		}
		
		if (countTransformed != countExpected) {
			FMLLoadingPlugin.logger.error(String.format("Transformation failed for %s (%d/%d), aborting...", classNode.name, countTransformed, countExpected));
			return bytes;
		}
		
		final ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS); // | ClassWriter.COMPUTE_FRAMES);
		classNode.accept(writer);
		final byte[] bytesNew = writer.toByteArray();
		FMLLoadingPlugin.logger.info(String.format("Successful injection in %s", classNode.name));
		return bytesNew;
	}
	
	private byte[] transformMinecraftForgeHooks(@Nonnull final byte[] bytes) {
		final ClassNode classNode = new ClassNode();
		final ClassReader classReader = new ClassReader(bytes);
		classReader.accept(classNode, 0);
		
		final int countExpected = 1;
		int countTransformed = 0;
		for (final MethodNode methodNode : classNode.methods) {
			// if (debugLog) { FMLLoadingPlugin.logger.info(String.format("- Method %s %s", methodNode.name, methodNode.desc)); }
			
			if ( (methodNode.name.equals(nodeMap.get("loadAdvancements.name")) || methodNode.name.equals("loadAdvancements"))
			  && methodNode.desc.equals(nodeMap.get("loadAdvancements.desc")) ) {
				FMLLoadingPlugin.logger.debug(String.format("Found method to transform: %s %s",
				                                           methodNode.name, methodNode.desc));
				
				int indexInstruction = 0;
				
				while (indexInstruction < methodNode.instructions.size()) {
					final AbstractInsnNode abstractNode = methodNode.instructions.get(indexInstruction);
					if (debugLog) { decompile(abstractNode); }
					
					if (abstractNode instanceof LdcInsnNode) {
						final LdcInsnNode nodeAt = (LdcInsnNode) abstractNode;
						
						if ( nodeAt.cst instanceof String
						  && ((String) nodeAt.cst).contains("Parsing error loading built-in advancement ") ) {
							final AbstractInsnNode abstractNodeToRemove = methodNode.instructions.get(indexInstruction - 4);
							if ( (abstractNodeToRemove instanceof FieldInsnNode)
							  && ((FieldInsnNode) abstractNodeToRemove).desc.equals("Lorg/apache/logging/log4j/Logger;") ) {
								indexInstruction -= 4;
								removeInstruction(methodNode, indexInstruction);
								removeInstruction(methodNode, indexInstruction);
								removeInstruction(methodNode, indexInstruction);
								removeInstruction(methodNode, indexInstruction);
								removeInstruction(methodNode, indexInstruction);
								removeInstruction(methodNode, indexInstruction);
								removeInstruction(methodNode, indexInstruction);
								removeInstruction(methodNode, indexInstruction);
								removeInstruction(methodNode, indexInstruction);
								removeInstruction(methodNode, indexInstruction);
								removeInstruction(methodNode, indexInstruction);
								indexInstruction--;
								
								if (debugLog) { FMLLoadingPlugin.logger.info(String.format("Injecting into %s.%s %s", classNode.name, methodNode.name, methodNode.desc)); }
								countTransformed++;
							}
						}
					}
					
					indexInstruction++;
				}
			}
		}
		
		if (countTransformed != countExpected) {
			FMLLoadingPlugin.logger.error(String.format("Transformation failed for %s (%d/%d), aborting...", classNode.name, countTransformed, countExpected));
			return bytes;
		}
		
		final ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS); // | ClassWriter.COMPUTE_FRAMES);
		classNode.accept(writer);
		final byte[] bytesNew = writer.toByteArray();
		FMLLoadingPlugin.logger.info(String.format("Successful injection in %s", classNode.name));
		return bytesNew;
	}
	
	private void removeInstruction(@Nonnull final MethodNode methodNode, final int indexInstruction) {
		final AbstractInsnNode abstractNodeToRemove = methodNode.instructions.get(indexInstruction);
		if (debugLog) {
			FMLLoadingPlugin.logger.info("Removing instruction:");
			decompile(abstractNodeToRemove);
		}
		methodNode.instructions.remove(abstractNodeToRemove);
	}
	
	private void saveClassToFile(final String path, final String nameClass, final byte[] bytes) {
		try {
			// create folder
			final File fileDir = new File(path);
			if (!fileDir.exists() || !fileDir.isDirectory()) {
				if (!fileDir.mkdirs()) {
					FMLLoadingPlugin.logger.error("Unable to create ASM dump folder, skipping...");
					return;
				}
			}
			
			// sanitize class name
			final String nameClass_clean = nameClass.replace("/", "_").replace("\\", "_").replace(" ", "_");
			
			// save class file
			final File fileClass = new File(fileDir, nameClass_clean + ".class");
			final FileOutputStream fileOutputStream = new FileOutputStream(fileClass);
			final DataOutputStream dataOutputStream = new DataOutputStream(fileOutputStream);
			dataOutputStream.write(bytes);
			dataOutputStream.flush();
			dataOutputStream.close();
		} catch (final Exception exception) {
			exception.printStackTrace();
		}
	}
	
	private static boolean opcodeToString_firstDump = true;
	private static String opcodeToString(final int opcode) {
		final Field[] fields = Opcodes.class.getFields();
		for (final Field field : fields) {
			if (field.getType() == int.class) {
				try {
					if (field.getInt(null) == opcode){
						return field.getName();
					}
				} catch (final Throwable throwable){
					if (opcodeToString_firstDump) {
						throwable.printStackTrace();
						opcodeToString_firstDump = false;
					}
				}
			}
		}
		return String.format("0x%x", opcode);
	}
	
	private static void decompile(final AbstractInsnNode abstractNode) {
		if (abstractNode == null) {
			FMLLoadingPlugin.logger.error(String.format("%20s %-20s %s", "NULL", "NULL", "null node"));
			return;
		}
		
		final String opcode = opcodeToString(abstractNode.getOpcode());
		if (abstractNode instanceof VarInsnNode) {
			final VarInsnNode node = (VarInsnNode) abstractNode;
			FMLLoadingPlugin.logger.info(String.format("%20s %-20s %s", opcode, "Var", node.var));
			
		} else if (abstractNode instanceof LabelNode) {
			final LabelNode node = (LabelNode) abstractNode;
			FMLLoadingPlugin.logger.info(String.format("%20s %-20s %s", opcode, "Label", node.getLabel()));
			
		} else if (abstractNode instanceof LineNumberNode) {
			final LineNumberNode node = (LineNumberNode) abstractNode;
			FMLLoadingPlugin.logger.info(String.format("%20s %-20s %s", opcode, "Line", node.line));
			
		} else if (abstractNode instanceof TypeInsnNode) {
			final TypeInsnNode node = (TypeInsnNode) abstractNode;
			FMLLoadingPlugin.logger.info(String.format("%20s %-20s %s", opcode, "Typed instruction", node.desc));
			
		} else if (abstractNode instanceof JumpInsnNode) {
			final JumpInsnNode node = (JumpInsnNode) abstractNode;
			FMLLoadingPlugin.logger.info(String.format("%20s %-20s %s", opcode, "Jump", node.label.getLabel()));
			
		} else if (abstractNode instanceof FrameNode) {
			final FrameNode node = (FrameNode) abstractNode;
			FMLLoadingPlugin.logger.info(String.format("%20s %-20s %d %s %s", opcode, "Frame", node.type, node.local, node.stack));
			
		} else if (abstractNode instanceof InsnNode) {
			final InsnNode node = (InsnNode) abstractNode;
			FMLLoadingPlugin.logger.info(String.format("%20s %-20s %s", opcode, "Instruction", node));
			
		} else if (abstractNode instanceof LdcInsnNode) {
			final LdcInsnNode node = (LdcInsnNode) abstractNode;
			FMLLoadingPlugin.logger.info(String.format("%20s %-20s %s", opcode, "Load", node.cst));
			
		} else if (abstractNode instanceof FieldInsnNode) {
			final FieldInsnNode node = (FieldInsnNode) abstractNode;
			FMLLoadingPlugin.logger.info(String.format("%20s %-20s %s %s %s", opcode, "Field", node.owner, node.name, node.desc));
			
		} else if (abstractNode instanceof MethodInsnNode) {
			final MethodInsnNode node = (MethodInsnNode) abstractNode;
			FMLLoadingPlugin.logger.info(String.format("%20s %-20s %s %s %s %s", opcode, "Method", node.owner, node.name, node.desc, node.itf));
			
		} else {
			FMLLoadingPlugin.logger.info(String.format("%20s %-20s %s %s %s", opcode, "Instruction", abstractNode.getOpcode(), abstractNode.getType(), abstractNode));
		}
	}
}
