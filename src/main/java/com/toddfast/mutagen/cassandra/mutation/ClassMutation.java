package com.toddfast.mutagen.cassandra.mutation;

import com.toddfast.mutagen.MutagenException;
import com.toddfast.mutagen.Mutation;
import com.toddfast.mutagen.cassandra.CassandraSubject;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

public class ClassMutation {
	
	/**
	 * Loads classes the do mutations using only the resource name
	 * 
	 * @param subject
	 * @param resource
	 * @return
	 * 
	 * @author todd fast
	 */
	@SuppressWarnings("unchecked")
	public static Mutation<Integer> loadMutationClass(
			CassandraSubject subject, String resource) {

		assert resource.endsWith(".class"):
			"Class resource name \""+resource+"\" should end with .class";

		int index=resource.indexOf(".class");
		String className=resource.substring(0,index).replace('/','.');

		// Load the class specified by the resource
		Class<?> clazz=null;
		try {
			clazz=Class.forName(className);
		}
		catch (ClassNotFoundException e) {
			// Should never happen
			throw new MutagenException("Could not load mutagen class \""+
				resource+"\"",e);
		}

		// Instantiate the class
		try {
			Constructor<?> constructor;
			Mutation<Integer> mutation=null;
			try {
				constructor=clazz.getConstructor(CassandraSubject.class);
				mutation=(Mutation<Integer>)constructor.newInstance(subject);
			}
			catch (NoSuchMethodException e) {
				// Wrong assumption
			}

			if (mutation==null) {
				// Try the null constructor
				try {
					constructor=clazz.getConstructor();
					mutation=(Mutation<Integer>)constructor.newInstance();
				}
				catch (NoSuchMethodException e) {
					throw new MutagenException("Could not find comparible "+
						"constructor for class \""+className+"\"",e);
				}
			}

			return mutation;
		}
		catch (InstantiationException e) {
			throw new MutagenException("Could not instantiate class \""+
				className+"\"",e);
		}
		catch (InvocationTargetException e) {
			if (e.getTargetException() instanceof RuntimeException) {
				throw (RuntimeException)e.getTargetException();
			}
			else {
				throw new MutagenException("Exception instantiating class \""+
					className+"\"",e);
			}
		}
		catch (IllegalAccessException e) {
			throw new MutagenException("Could not access constructor for "+
				"mutation class \""+className+"\"",e);
		}
	}
}
