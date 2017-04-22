package com.toddfast.mutagen.cassandra.impl;

import com.toddfast.mutagen.cassandra.CassandraMutagen;
import com.toddfast.mutagen.cassandra.CassandraSubject;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.HostDistance;
import com.datastax.driver.core.PoolingOptions;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.SocketOptions;
import com.toddfast.mutagen.Plan;
import com.toddfast.mutagen.State;

import java.io.IOException;

import static org.junit.Assert.*;

/**
 *
 * @author Todd Fast
 */
public class CassandraMutagenImplIT {
	
	private static Session session;
	
	private static final String KEY_SPACE = "TEST_KEYSPACE";
	
	public CassandraMutagenImplIT() {
	}

	@Before
	public void setUp() {
		defineSession();
		createKeyspace();
	}


	@After
	public void tearDown() {
		session.execute("DROP KEYSPACE " + KEY_SPACE + ";");
		session.close();
		System.out.println("Dropped keyspace "+KEY_SPACE);
	}


	/**
	 * This is it!
	 *
	 */
	private Plan.Result<Integer> mutate()
			throws IOException {

		// Get an instance of CassandraMutagen
		// Using Nu: CassandraMutagen mutagen=$(CassandraMutagen.class);
		CassandraMutagen mutagen=new CassandraMutagenImpl();

		// Initialize the list of mutations
		String rootResourcePath="com/toddfast/mutagen/cassandra/test/mutations";
		mutagen.initialize(rootResourcePath);

		// Mutate!
		CassandraSubject subject = new CassandraSubject(session, "Table1");
		Plan.Result<Integer> result=mutagen.mutate(subject);

		return result;
	}

	@Test
	public void testInitialize() throws Exception {

		Plan.Result<Integer> result = mutate();

		// Check the results
		State<Integer> state=result.getLastState();

		System.out.println("Mutation complete: "+result.isMutationComplete());
		System.out.println("Exception: "+result.getException());
		if (result.getException()!=null) {
			result.getException().printStackTrace();
		}
		System.out.println("Completed mutations: "+result.getCompletedMutations());
		System.out.println("Remining mutations: "+result.getRemainingMutations());
		System.out.println("Last state: "+(state!=null ? state.getID() : "null"));

		assertTrue(result.isMutationComplete());
		assertNull(result.getException());
		assertEquals((state!=null ? state.getID() : (Integer)(-1)),(Integer)7);
	}


	/**
	 *
	 *
	 */
	@Test
	public void testData() throws Exception {
		
		mutate();
		
		Row row1 = session.execute("SELECT * FROM Table1 WHERE key = 'row1';" ).one();

		assertEquals("foo1", row1.getString("value1"));
		assertEquals("bar1", row1.getString("value2"));
		assertEquals("new1", row1.getString("value3"));

		Row row2 = session.execute("SELECT * FROM Table1 WHERE key = 'row2';" ).one();

		assertEquals("chickens",row2.getString("value1"));
		assertEquals("sneezes",row2.getString("value2"));
		assertEquals("new2", row2.getString("value3"));

		Row row3 = session.execute("SELECT * FROM Table1 WHERE key = 'row3';" ).one();
		
		assertEquals("bar",row3.getString("value1"));
		assertEquals("baz",row3.getString("value2"));
	}

	private static void defineSession() {
		int maxConnections = 1;
		int maxSimultaneousRequests = 2; 
		
		PoolingOptions pools = new PoolingOptions();
		pools.setMaxRequestsPerConnection(HostDistance.LOCAL, maxSimultaneousRequests);
		pools.setCoreConnectionsPerHost(HostDistance.LOCAL, maxConnections);
		pools.setMaxConnectionsPerHost(HostDistance.LOCAL, maxConnections);
		pools.setCoreConnectionsPerHost(HostDistance.REMOTE, maxConnections);
		pools.setMaxConnectionsPerHost(HostDistance.REMOTE, maxConnections);
		
		Cluster cluster = new Cluster.Builder()
			.withPoolingOptions(pools)
			.addContactPoint("localhost")
			.withPort(9042)
			.withSocketOptions(new SocketOptions().setTcpNoDelay(true))
			.build();
		
		session = cluster.connect();
	}
	
	private static void createKeyspace() {

		System.out.println("Creating keyspace " + session + "...");

		String create_keyspace = 
				"CREATE keyspace " + KEY_SPACE
				+ " WITH Replication = {'class': 'SimpleStrategy'" +
				", 'replication_factor': '1'};";
		
		// Create the keyspace using CQL
		session.execute(create_keyspace);

		// Mark the new keyspace as in use
		session.execute("use " + KEY_SPACE + ";");
		

		System.out.println("Created keyspace " + KEY_SPACE);
	}

}
