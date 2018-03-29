/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.console;

import java.io.IOException;

import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.RepositoryLockedException;
import org.eclipse.rdf4j.repository.config.RepositoryConfigException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Open command
 * 
 * @author Dale Visser
 */
public class Open implements Command {
	private static final Logger LOGGER = LoggerFactory.getLogger(Open.class);

	private final ConsoleIO consoleIO;
	private final ConsoleState state;
	private final Close close;
	private final LockRemover lockRemover;

	@Override
	public  String getName() {
		return "open";
	}
	
	@Override
	public String getHelpShort() {
		return "Opens a repository to work on, takes a repository ID as argument";
	}
	
	@Override
	public String getHelpLong() {
		return  PrintHelp.USAGE
			+ "open <repositoryID>   Opens the repository with the specified ID\n";
	}
	
	/**
	 * Constructor
	 * 
	 * @param consoleIO
	 * @param state
	 * @param close
	 * @param lockRemover 
	 */
	Open(ConsoleIO consoleIO, ConsoleState state, Close close, LockRemover lockRemover) {
		this.consoleIO = consoleIO;
		this.state = state;
		this.close = close;
		this.lockRemover = lockRemover;
	}

	@Override
	public void execute(String... tokens) {
		if (tokens.length == 2) {
			openRepository(tokens[1]);
		} else {
			consoleIO.writeln(getHelpLong());
		}
	}

	private static final String OPEN_FAILURE = "Failed to open repository";

	/**
	 * Open a repository
	 * 
	 * @param repoID repository ID 
	 */
	protected void openRepository(final String repoID) {
		try {
			final Repository newRepository = state.getManager().getRepository(repoID);

			if (newRepository == null) {
				consoleIO.writeError("Unknown repository: '" + repoID + "'");
			} else {
				// Close current repository, if any
				close.closeRepository(false);
				state.setRepository(newRepository);
				state.setRepositoryID(repoID);
				consoleIO.writeln("Opened repository '" + repoID + "'");
			}
		} catch (RepositoryLockedException e) {
			try {
				if (lockRemover.tryToRemoveLock(e)) {
					openRepository(repoID);
				} else {
					consoleIO.writeError(OPEN_FAILURE);
					LOGGER.error(OPEN_FAILURE, e);
				}
			} catch (IOException e1) {
				consoleIO.writeError("Unable to remove lock: " + e1.getMessage());
			}
		} catch (RepositoryConfigException e) {
			consoleIO.writeError(e.getMessage());
			LOGGER.error(OPEN_FAILURE, e);
		} catch (RepositoryException e) {
			consoleIO.writeError(e.getMessage());
			LOGGER.error(OPEN_FAILURE, e);
		}
	}
}
