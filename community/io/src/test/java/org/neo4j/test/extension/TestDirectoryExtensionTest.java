/*
 * Copyright (c) 2002-2018 "Neo4j,"
 * Neo4j Sweden AB [http://neo4j.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.test.extension;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.neo4j.io.fs.DefaultFileSystemAbstraction;
import org.neo4j.test.rule.TestDirectory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectMethod;
import static org.neo4j.test.extension.DirectoryExtensionLifecycleVerificationTest.ConfigurationParameterCondition.TEST_TOGGLE;
import static org.neo4j.test.extension.ExecutionSharedContext.CONTEXT;
import static org.neo4j.test.extension.ExecutionSharedContext.FAILED_TEST_FILE_KEY;
import static org.neo4j.test.extension.ExecutionSharedContext.SUCCESSFUL_TEST_FILE_KEY;

@ExtendWith( {DefaultFileSystemExtension.class, TestDirectoryExtension.class} )
class TestDirectoryExtensionTest
{
    @Inject
    TestDirectory testDirectory;
    @Inject
    DefaultFileSystemAbstraction fileSystem;

    @Test
    void testDirectoryInjectionWorks()
    {
        assertNotNull( testDirectory );
    }

    @Test
    void testDirectoryInitialisedForUsage()
    {
        File directory = testDirectory.directory();
        assertNotNull( directory );
        assertTrue( directory.exists() );
        Path targetTestData = Paths.get( "target", "test data" );
        assertTrue( directory.getAbsolutePath().contains( targetTestData.toString() ) );
    }

    @Test
    void testDirectoryUsesFileSystemFromExtension()
    {
        assertSame( fileSystem, testDirectory.getFileSystem() );
    }

    @Test
    void createTestFile()
    {
        File file = testDirectory.createFile( "a" );
        assertEquals( "a", file.getName() );
        assertTrue( fileSystem.fileExists( file ) );
    }

    @Test
    void failedTestShouldKeepDirectory()
    {
        CONTEXT.clear();
        execute( "failAndKeepDirectory" );
        File failedFile = CONTEXT.getValue( FAILED_TEST_FILE_KEY );
        assertNotNull( failedFile );
        assertTrue( failedFile.exists() );
    }

    @Test
    void successfulTestShouldCleanupDirectory()
    {
        CONTEXT.clear();
        execute( "executeAndCleanupDirectory" );
        File greenTestFail = CONTEXT.getValue( SUCCESSFUL_TEST_FILE_KEY );
        assertNotNull( greenTestFail );
        assertFalse( greenTestFail.exists() );
    }

    private static void execute( String testName )
    {
        LauncherDiscoveryRequest discoveryRequest = LauncherDiscoveryRequestBuilder.request()
                .selectors( selectMethod( DirectoryExtensionLifecycleVerificationTest.class, testName ))
                .configurationParameter( TEST_TOGGLE, "true" )
                .build();
        Launcher launcher = LauncherFactory.create();
        launcher.execute( discoveryRequest );
    }
}
