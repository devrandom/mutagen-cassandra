package com.toddfast.mutagen.cassandra.mutation;

import com.datastax.driver.core.Statement;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Update;
import com.toddfast.mutagen.MutagenException;
import com.toddfast.mutagen.State;
import com.toddfast.mutagen.cassandra.CassandraSubject;
import io.netty.util.internal.StringUtil;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * This class handles the parsing of CSV files that contain
 * seed data for Column Family (table) in a specific Keyspace.
 * 
 * Filename Format: 	V{number}_{ColumnFamily}
 * Filename Example:	V0043_AblumsByArtist
 * 
 * CSV Format:
 * 	Header: 	{rowKey}, {columnName},...,{columnName}
 * 	Example:	artistName, albumName, yearReleased, numberOfTracks
 * 				'Pink Floyd',  'The Wall', '1979', '26'
 * 
 * @author Andrew From
 */
public class CSVMutation extends AbstractCassandraMutation {
	
	private StringBuilder source = new StringBuilder();
	
	private State<Integer> state;
	
	private List<Statement> updateStatements;
	
	private String updateTableName;
	
	private static final String CSV_DELIM = ",";
	
	private static final String RESOUCE_NAME_DELIM = "_";

	public CSVMutation(CassandraSubject subject, String resourceName) {
		super(subject);
		this.state=super.parseVersion(resourceName);
		this.updateStatements = new ArrayList<Statement>();
		loadCSVData(resourceName);
	}

	@Override
	public State<Integer> getResultingState() {
		return state;
	}

	@Override
	protected String getChangeSummary() {
		return source.toString();
	}

	@Override
	protected void performMutation(com.toddfast.mutagen.Mutation.Context context) {
		
		context.debug("Executing mutation {}",state.getID());
		
		for(Statement statement : updateStatements) {
			try {
				getSubject().getSession().execute(statement);
			} catch (Exception e) {
				context.error("Exception executing update from CSV\"{}\"", e);
				throw new MutagenException("Exception executing update from csv\"",e);
			}
		}
	}
	
	private void loadCSVData(String resourceName) {
		BufferedReader br = null;
		String currentLine = null;
		String[] columnNames = null;
		
		updateTableName = MutationParser.parseMutationSubject(resourceName);
		
		try {
			br = new BufferedReader(new FileReader(resourceName));
			
			if((currentLine = br.readLine()) != null) {
				columnNames = currentLine.split(CSV_DELIM);
			}
			
			while((currentLine = br.readLine()) != null) {
				String[] rowValues = currentLine.split(CSV_DELIM);
				
				addBatchWrite(columnNames, rowValues);
			}
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if(br != null) {
				try{
					br.close();
				}catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	private void addBatchWrite(String[] columnNames, String[] rowValues) {
		
		if((columnNames.length < 2) || (rowValues.length < 2)) {
			throw new MutagenException("CSV Update for Table: " + updateTableName + " is malformed: "
					+ " there must be at least 2 header names and 2 row values.");
		}
		
		Update.Where where = QueryBuilder.update(updateTableName).where();
		// where rowKeyName = rowKeyValue
		where = where.and(QueryBuilder.eq(columnNames[0], rowValues[0]));
		
		Update.Assignments assingments = where.with(QueryBuilder.set(columnNames[1], rowValues[1]));
				
		for(int i = 2; i < columnNames.length; i++) {
			assingments.and(QueryBuilder.set(columnNames[i], rowValues[i]));
		}			
			
		source.append(StringUtil.NEWLINE).append(assingments.toString());
			
		updateStatements.add(assingments);
	}
}
