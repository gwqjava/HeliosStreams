/**
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
 */
package com.heliosapm.streams.collector.groovy;

import java.io.File;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.customizers.ImportCustomizer;
import org.codehaus.groovy.control.messages.WarningMessage;

import com.heliosapm.utils.classload.HeliosURLClassLoader;
import com.heliosapm.utils.config.ConfigurationHelper;
import com.heliosapm.utils.url.URLHelper;

import groovy.lang.GroovyClassLoader;

/**
 * <p>Title: ManagedScriptFactory</p>
 * <p>Description: The factory for creating {@link ManagedScript} instances</p> 
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.streams.collector.groovy.ManagedScriptFactory</code></p>
 */

public class ManagedScriptFactory {
	/** The singleton instance */
	private static volatile ManagedScriptFactory instance;
	/** The singleton instance ctor lock */
	private static final Object lock = new Object();
	
	/** The configuration key for the collector service root directory */
	public static final String CONFIG_ROOT_DIR = "collector.service.rootdir";
	/** The default collector service root directory */
	public static final String DEFAULT_ROOT_DIR = new File(new File(System.getProperty("user.home")), ".heliosapm-collector").getAbsolutePath();
	
	/** The expected directory names under the collector-service root */
	public static final Set<String> DIR_NAMES = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(
			"lib", "bin", "conf", "datasources", "web", "collectors", "cache", "db", "chronicle", "ssh", "fixtures"
	)));
	
	/** Instance logger */
	protected final Logger log = LogManager.getLogger(getClass());
	/** The collector service root directory */
	protected final File rootDirectory;
	/** The lib (jar) directory class loader */
	protected final HeliosURLClassLoader libDirClassLoader;
	/** The groovy class loader for dynamic managed scripts */
	protected final GroovyClassLoader groovyClassLoader;
	/** The groovy compiler configuration */
	protected final CompilerConfiguration compilerConfig = new CompilerConfiguration(CompilerConfiguration.DEFAULT);
	/** The initial and default imports customizer for the compiler configuration */
	protected final ImportCustomizer importCustomizer = new ImportCustomizer(); 

	
	/**
	 * Acquires and returns the ManagedScriptFactory singleton instance
	 * @return the ManagedScriptFactory singleton instance
	 */
	public static ManagedScriptFactory getInstance() {
		if(instance==null) {
			synchronized(lock) {
				if(instance==null) {
					instance = new ManagedScriptFactory();
				}
			}
		}
		return instance;
	}
	
	/**
	 * Creates a new ManagedScriptFactory
	 */
	private ManagedScriptFactory() {
		log.info(">>>>> Starting ManagedScriptFactory...");
		final String rootDirName = ConfigurationHelper.getSystemThenEnvProperty(CONFIG_ROOT_DIR, DEFAULT_ROOT_DIR);
		rootDirectory = new File(rootDirName);
		log.info("Collector Service root directory: [{}]", rootDirectory);
		rootDirectory.mkdirs();
		if(!rootDirectory.isDirectory()) throw new RuntimeException("Failed to create root directory [" + rootDirectory + "]");
		initSubDirs();
		libDirClassLoader = HeliosURLClassLoader.getOrCreateLoader(getClass().getSimpleName() + "LibClassLoader", listLibJarUrls(new File(rootDirectory, "lib"), new HashSet<URL>()));
		groovyClassLoader = new GroovyClassLoader(libDirClassLoader, compilerConfig, false);
		groovyClassLoader.addClasspath(new File(rootDirectory, "fixtures").getAbsolutePath());
		groovyClassLoader.addClasspath(new File(rootDirectory, "conf").getAbsolutePath());
		customizeCompiler();		
		log.info("<<<<< ManagedScriptFactory started.");
	}

	/**
	 * Creates any missing subdirectories
	 */
	private void initSubDirs() {
		for(String dirName: DIR_NAMES) {
			final File f = new File(rootDirectory, dirName);
			if(!f.isDirectory()) {
				if(!f.mkdir()) {
					log.warn("Failed to create subdirectory [{}]", f);
				}
			}
		}		
	}
	
	private void customizeCompiler() {
		compilerConfig.setDebug(true);
		compilerConfig.setMinimumRecompilationInterval(5);
		compilerConfig.setRecompileGroovySource(true);
		compilerConfig.setOptimizationOptions(Collections.singletonMap("indy", true));
		compilerConfig.setScriptBaseClass(ManagedScript.class.getName());
		compilerConfig.setTargetDirectory(rootDirectory);
		compilerConfig.setTolerance(0);
		compilerConfig.setVerbose(true);
		compilerConfig.setWarningLevel(WarningMessage.PARANOIA);
	}
	
	/**
	 * Applies the configured imports to the compiler configuration
	 * @param impCustomizer The import customizer to add the imports to
	 * @param imps  The imports to add
	 */
	private void applyImports(final String...imps) {		
		for(String imp: imps) {
			String _imp = imp.trim().replaceAll("\\s+", " ");
			if(!_imp.startsWith("import")) {
				log.warn("Unrecognized import [", imp, "]");
				continue;
			}
			if(_imp.startsWith("import static ")) {
				if(_imp.endsWith(".*")) {
					importCustomizer.addStaticStars(_imp.replace("import static ", "").replace(".*", ""));
				} else {
					String cleaned = _imp.replace("import static ", "").replace(".*", "");
					int index = cleaned.lastIndexOf('.');
					if(index==-1) {
						log.warn("Failed to parse non-star static import [", imp, "]");
						continue;
					}
					importCustomizer.addStaticImport(cleaned.substring(0, index), cleaned.substring(index+1));
				}
			} else {
				if(_imp.endsWith(".*")) {
					importCustomizer.addStarImports(_imp.replace("import ", "").replace(".*", ""));
				} else {
					importCustomizer.addImports(_imp.replace("import ", ""));
				}
			}
		}
		compilerConfig.addCompilationCustomizers(importCustomizer);
	}
	
	
	private URL[] listLibJarUrls(final File dir, final Set<URL> accum) {
		final Set<URL> _accum = accum==null ? new HashSet<URL>() : accum;
		for(File f: dir.listFiles()) {
			if(f.isDirectory()) {
				listLibJarUrls(f, _accum);
			} else {
				if(f.getName().toLowerCase().endsWith(".jar")) {
					_accum.add(URLHelper.toURL(f));
				}
			}
		}
		return _accum.toArray(new URL[_accum.size()]);		
	}

}