package com.toddfast.mutagen.cassandra.impl;

import com.toddfast.mutagen.Plan;
import com.toddfast.mutagen.Planner;
import com.toddfast.mutagen.basic.ResourceScanner;
import com.toddfast.mutagen.cassandra.CassandraCoordinator;
import com.toddfast.mutagen.cassandra.CassandraMutagen;
import com.toddfast.mutagen.cassandra.CassandraSubject;
import com.toddfast.mutagen.cassandra.mutation.MutationParser;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 *
 * 
 * @author Todd Fast
 */
//@ServiceProvider(scope=Scope.CLIENT_MANAGED)
public class CassandraMutagenImpl implements CassandraMutagen {
	
	private Map<String, List<String>> subjectMutations = new HashMap<String, List<String>>();
	
	/**
	 * Find all cassandra version files given a root
	 * resource path to search.
	 */
	@Override
	public void initialize(String rootResourcePath)
			throws IOException {

		try {
			List<String> discoveredResources=
				ResourceScanner.getInstance().getResources(
					rootResourcePath,Pattern.compile(".*"),
					getClass().getClassLoader());

			// Make sure we found some resources
			if (discoveredResources.isEmpty()) {
				throw new IllegalArgumentException("Could not find resources "+
					"on path \""+rootResourcePath+"\"");
			}

			Collections.sort(discoveredResources,COMPARATOR);

			for (String resource: discoveredResources) {
				System.out.println("Found mutation resource \""+resource+"\"");

				if (resource.endsWith(".class")) {
					// Framework depends on unix style file paths for proper
					// class loading.
					resource = FilenameUtils.separatorsToUnix(resource);
					resource=resource.substring(
						resource.indexOf(rootResourcePath));
					if (resource.contains("$")) {
						// skip inner classes
						continue;
					}
                }
				putSubjectMutation(resource);
			}
		}
		catch (URISyntaxException e) {
			throw new IllegalArgumentException("Could not find resources on "+
				"path \""+rootResourcePath+"\"",e);
		}
	}

	@Override
	public Plan.Result<Integer> mutate(CassandraSubject subject) {
		// Do this in a VM-wide critical section. External cluster-wide 
		// synchronization is going to have to happen in the coordinator.
		synchronized (System.class) {
			CassandraCoordinator coordinator=new CassandraCoordinator(subject);

			Planner<Integer> planner=
				new CassandraPlanner(subject,subjectMutations.get(subject.getSubjectName()));
			Plan<Integer> plan=planner.getPlan(subject,coordinator);

			// Execute the plan
			Plan.Result<Integer> result=plan.execute();
			return result;
		}
	}
	
	/**
	 * Adds mutations for a specific C* Table
	 * 
	 * @param resourcePath - the path to the mutation resource
	 */
	private void putSubjectMutation(String resourcePath) {
		String subjectName = MutationParser.parseMutationSubject(resourcePath);
		
		if(!subjectMutations.containsKey(subjectName)) {
			List<String> resources = new ArrayList<String>();
			resources.add(resourcePath);
			subjectMutations.put(subjectName, resources);
		} else {
			subjectMutations.get(subjectName).add(resourcePath);
		}
	}

	/**
	 * Sorts by root file name, ignoring path and file extension
	 *
	 */
	private static final Comparator<String> COMPARATOR=
		new Comparator<String>() {
			@Override
			public int compare(String path1, String path2) {
				File file1 = new File(path1);
				File file2 = new File(path2);
				
				String filename1 = file1.getName();
				String filename2 = file2.getName();
				return filename1.compareTo(filename2);
				
			}
		};
}
