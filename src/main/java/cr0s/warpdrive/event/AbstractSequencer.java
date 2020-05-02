package cr0s.warpdrive.event;

import javax.annotation.Nonnull;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import net.minecraft.nbt.NBTTagCompound;

public abstract class AbstractSequencer {
	
	private static AtomicBoolean isUpdating = new AtomicBoolean(false);
	private static ConcurrentHashMap<AbstractSequencer, Boolean> sequencers = new ConcurrentHashMap<>(10);
	
	public static void updateTick() {
		if (sequencers.isEmpty()) {
			return;
		}
		while (!isUpdating.compareAndSet(false, true)) {
			Thread.yield();
		}
		for (final Iterator<Entry<AbstractSequencer, Boolean>> iterator = sequencers.entrySet().iterator(); iterator.hasNext(); ) {
			final Entry<AbstractSequencer, Boolean> entry = iterator.next();
			final boolean doContinue = entry.getKey().onUpdate();
			if (!doContinue) {
				iterator.remove();
			}
		}
		isUpdating.set(false);
	}
	
	protected void register() {
		while (!isUpdating.compareAndSet(false, true)) {
			Thread.yield();
		}
		sequencers.put(this, true);
		isUpdating.set(false);
	}
	
	protected void unregister() {
		sequencers.put(this, false);
	}
	
	abstract public boolean onUpdate();

	abstract protected void readFromNBT(@Nonnull final NBTTagCompound tagCompound);

	abstract protected NBTTagCompound writeToNBT(@Nonnull final NBTTagCompound tagCompound);
	
}
