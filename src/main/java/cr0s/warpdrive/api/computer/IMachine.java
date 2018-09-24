package cr0s.warpdrive.api.computer;

public interface IMachine extends IInterfaced {
	
	String[] name(final Object[] arguments);
	
	Object[] enable(final Object[] arguments);
	
	Object[] isAssemblyValid();
}
