/********************************************************************************
 * Copyright (c) 2006 IBM Corporation. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is 
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Initial Contributors:
 * The following IBM employees contributed the initial implementation:
 * David McKnight, David Dykstal.
 * 
 * Contributors:
 * {Name} (company) - description of contribution.
 ********************************************************************************/
package org.eclipse.rse.internal.persistence;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.rse.persistence.IRSEPersistenceProvider;
import org.eclipse.rse.persistence.dom.IRSEDOMConstants;
import org.eclipse.rse.persistence.dom.RSEDOM;
import org.eclipse.rse.persistence.dom.RSEDOMNode;
import org.eclipse.rse.persistence.dom.RSEDOMNodeAttribute;

/**
 * This is class is used to restore an RSE DOM from disk and import it into RSE.
 * It stores the DOM as a tree of folders and .properties files.
 */
public class PropertyFileProvider implements IRSEPersistenceProvider {
	
	private static final String NULL_VALUE_STRING = "null"; //$NON-NLS-1$
	private static final String PROPERTIES_FILE_NAME = "node.properties"; //$NON-NLS-1$

	/* 
	 * Metatype names 
	 * each entry is an array. The first is the preferred name.
	 * The other names are acceptable alternates.
	 * Names must not contain periods or whitespace.
	 * Lowercase letters, numbers and dashes (-) are preferred.
	 */
	private static final String[] MT_ATTRIBUTE_TYPE = new String[] {"04-attr-type", "attr-type"}; //$NON-NLS-1$ //$NON-NLS-2$
	private static final String[] MT_ATTRIBUTE = new String[] {"03-attr", "attr"}; //$NON-NLS-1$ //$NON-NLS-2$
	private static final String[] MT_CHILD = new String[] {"06-child", "child"}; //$NON-NLS-1$ //$NON-NLS-2$
	private static final String[] MT_NODE_TYPE = new String[] {"01-type", "01-node-type", "n-type"}; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	private static final String[] MT_NODE_NAME = new String[] {"00-name", "00-node-name", "n-name"}; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	private static final String[] MT_REFERENCE = new String[] {"05-ref", "ref"}; //$NON-NLS-1$ //$NON-NLS-2$

	/* Type abbreviations */
	private static final String AB_SUBSYSTEM = "SS"; //$NON-NLS-1$
	private static final String AB_SERVICE_LAUNCHER = "SL"; //$NON-NLS-1$
	private static final String AB_PROPERTY_SET = "PS"; //$NON-NLS-1$
	private static final String AB_PROPERTY = "P"; //$NON-NLS-1$
	private static final String AB_HOST = "H"; //$NON-NLS-1$
	private static final String AB_FILTER_STRING = "FS"; //$NON-NLS-1$
	private static final String AB_FILTER_POOL_REFERENCE = "FPR"; //$NON-NLS-1$
	private static final String AB_FILTER_POOL = "FP"; //$NON-NLS-1$
	private static final String AB_FILTER = "F"; //$NON-NLS-1$
	private static final String AB_CONNECTOR_SERVICE = "CS"; //$NON-NLS-1$
	private static final String AB_PROFILE = "PRF"; //$NON-NLS-1$
	
	private Pattern period = Pattern.compile("\\."); //$NON-NLS-1$
	private Pattern suffixPattern = Pattern.compile("_(\\d+)$"); //$NON-NLS-1$
	private Pattern unicodePattern = Pattern.compile("#(\\p{XDigit}+)#"); //$NON-NLS-1$

	private Map typeQualifiers = getTypeQualifiers();

	/* (non-Javadoc)
	 * @see org.eclipse.rse.persistence.IRSEPersistenceProvider#getSavedProfileNames()
	 */
	public String[] getSavedProfileNames() {
		List names = new Vector(10);
		IFolder providerFolder = getProviderFolder();
		try {
			IResource[] profileCandidates = providerFolder.members();
			for (int i = 0; i < profileCandidates.length; i++) {
				IResource profileCandidate = profileCandidates[i];
				if (profileCandidate.getType() == IResource.FOLDER) {
					String candidateName = profileCandidate.getName();
					String[] parts = split(candidateName, 2);
					if (parts[0].equals(AB_PROFILE)) {
						String name = thaw(parts[1]);
						names.add(name);
					}
				}
			}
		} catch (CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		String[] result = new String[names.size()];
		names.toArray(result);
		return result;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.rse.persistence.IRSEPersistenceProvider#saveRSEDOM(org.eclipse.rse.persistence.dom.RSEDOM, org.eclipse.core.runtime.IProgressMonitor)
	 */
	public boolean saveRSEDOM(RSEDOM dom, IProgressMonitor monitor) {
		IFolder providerFolder = getProviderFolder();
		try {
			int n = countNodes(dom);
			if (monitor != null) monitor.beginTask(Messages.PropertyFileProvider_SavingTaskName, n);
			saveNode(dom, providerFolder, monitor);
			if (monitor != null) monitor.done();
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.rse.persistence.IRSEPersistenceProvider#deleteProfile(java.lang.String, org.eclipse.core.runtime.IProgressMonitor)
	 */
	public IStatus deleteProfile(String profileName, IProgressMonitor monitor) {
		IStatus result = Status.OK_STATUS;
		IFolder profileFolder = getProfileFolder(profileName);
		if (profileFolder.exists()) {
			try {
				profileFolder.delete(IResource.FORCE, monitor);
			} catch (CoreException e) {
				result = new Status(IStatus.ERROR, null, 0, Messages.PropertyFileProvider_UnexpectedException, e);
			}
		}
		return result;
	}

	
	/**
	 * Saves a node from the DOM to the file system.
	 * @param node The node to save.
	 * @param parentFolder The folder in which to save this node. The node will be a
	 * subfolder of this folder.
	 * @param monitor The progress monitor. If the monitor has been cancel then
	 * this method will do nothing and return null.
	 * @return The name of the folder saving this node. Can be used for constructing 
	 * references to children. May return null if the monitor has been canceled.
	 */
	private String saveNode(RSEDOMNode node, IFolder parentFolder, IProgressMonitor monitor) {
		if (monitor != null && monitor.isCanceled()) return null;
		String nodeFolderName = getSaveFolderName(node);
		IFolder nodeFolder = getFolder(parentFolder, nodeFolderName);
		Properties properties = getProperties(node, false, monitor);
		RSEDOMNode[] children = node.getChildren();
		Set childFolderNames = new HashSet();
		for (int i = 0; i < children.length; i++) {
			RSEDOMNode child = children[i];
			String index = getIndexString(i);
			if (!isNodeEmbedded(child)) {
				String key = combine(MT_REFERENCE[0], index);
				String childFolderName = saveNode(child, nodeFolder, monitor);
				if (childFolderName != null) properties.put(key, childFolderName);
				childFolderNames.add(childFolderName);
			}
		}
		removeFolders(nodeFolder, childFolderNames);
		String propertiesFileName = PROPERTIES_FILE_NAME;
		IFile propertiesFile = nodeFolder.getFile(propertiesFileName);
		writeProperties(properties, "RSE DOM Node", propertiesFile); //$NON-NLS-1$
		return nodeFolderName;
	}
	
	/**
	 * Removes childFolders from the parent folder that are not in the keep set.
	 * Typically used to clean renamed nodes from the tree on a save operation.
	 * @param parentFolder The folder whose subfolders are to be examined.
	 * @param keepSet The names of the folders that should be kept. Others are discarded.
	 */
	private void removeFolders(IFolder parentFolder, Set keepSet) {
		try {
			IResource[] children = parentFolder.members();
			for (int i = 0; i < children.length; i++) {
				IResource child = children[i];
				if (child.getType() == IResource.FOLDER) {
					String childFolderName = child.getName();
					if (!keepSet.contains(childFolderName)) {
						child.delete(true, null);
					}
				}
			}
		} catch (CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * Returns the name of a folder that can be used to store a node of a particular 
	 * type. Since this is a folder, its name must conform to the rules of the file
	 * system. The names are derived from the name and type of the node. Note that the 
	 * actual name of the node is also stored as a property so we need not attempt
	 * to recover the node name from the folder name.
	 * @param node The node that will eventually be stored in this folder.
	 * @return The name of the folder to store this node.
	 */
	private String getSaveFolderName(RSEDOMNode node) {
		String type = node.getType();
		type = (String) typeQualifiers.get(type);
		String name = node.getName();
		name = freeze(name);
		String result = combine(type, name);
		return result;
	}
	
	private static final String VALID = "abcdefghijklmnopqrstuvwxyz0123456789-._"; //$NON-NLS-1$
	private static final String UPPER = "ABCDEFGHIJKLMNOPQRTSUVWXYZ"; //$NON-NLS-1$
	/**
	 * Transforms an arbitrary name into one that can be used in any file system
	 * that supports long names. The transformation appends a number to the name
	 * that captures the case of the letters in the name. If a character falls
	 * outside the range of understood characters, it is converted to its hexadecimal unicode
	 * equivalent. Spaces are converted to underscores.
	 * @param name The name to be transformed
	 * @return The transformed name
	 * @see #thaw(String)
	 */
	private String freeze(String name) {
		int p = name.indexOf(':');
		if (p >= 0) {
			name = name.substring(p + 1);
		}
		StringBuffer buf = new StringBuffer(name.length());
		char[] chars = name.toCharArray();
		long suffix = 0;
		for (int i = 0; i < chars.length; i++) {
			char c = chars[i];
			suffix *= 2;
			if (VALID.indexOf(c) >= 0) {
				buf.append(c);
			} else if (UPPER.indexOf(c) >= 0) { // if uppercase
				buf.append(Character.toLowerCase(c));
				suffix += 1;
			} else if (c == ' ') { // if space
				buf.append('_');
				suffix += 1;
			} else { // if out of range
				buf.append('#');
				buf.append(Integer.toHexString(c));
				buf.append('#');
			}			
		}
		name = buf.toString() + "_" + Long.toString(suffix); //$NON-NLS-1$
		return name;
	}

	/**
	 * Recovers an arbitrary name from its frozen counterpart.
	 * @param name The name to be transformed
	 * @return The transformed name
	 * @see #freeze(String)
	 */
	private String thaw(String name) {
		String result = name;
		Matcher m = suffixPattern.matcher(name);
		if (m.find()) {
			String root = name.substring(0, m.start());
			String suffix = m.group(1);
			long caseCode = Long.parseLong(suffix);
			root = thawUnicode(root);
			root = thawCase(root, caseCode);
			result = root;
		}
		return result;
	}

	private String thawUnicode(String name) {
		Matcher m = unicodePattern.matcher(name);
		StringBuffer b = new StringBuffer();
		int p0 = 0;
		while (m.find()) {
			int p1 = m.start();
			String chunk0 = name.substring(p0, p1);
			String digits = m.group(1);
			int codePoint = Integer.valueOf(digits, 16).intValue();
			char ch = (char) codePoint;
			String chunk1 = Character.toString(ch);
			b.append(chunk0);
			b.append(chunk1);
			p0 = m.end();
		}
		b.append(name.substring(p0));
		String result = b.toString();
		return result;
	}
	
	private String thawCase(String name, long caseCode) {
		StringBuffer b = new StringBuffer();
		char[] chars = name.toCharArray();
		for (int i = chars.length - 1; i >= 0; i--) {
			char ch = chars[i];
			boolean shift = (caseCode & 1L) == 1;
			if (shift) { 
				ch = (ch == '_') ? ' ' : Character.toUpperCase(ch);
			}
			b.append(ch);
			caseCode = caseCode >> 1;
		}
		String result = b.reverse().toString();
		return result;
	}
	
	private Map getTypeQualifiers() {
		Map typeQualifiers = new HashMap();
		typeQualifiers.put(IRSEDOMConstants.TYPE_CONNECTOR_SERVICE, AB_CONNECTOR_SERVICE);
		typeQualifiers.put(IRSEDOMConstants.TYPE_FILTER, AB_FILTER);
		typeQualifiers.put(IRSEDOMConstants.TYPE_FILTER_POOL, AB_FILTER_POOL);
		typeQualifiers.put(IRSEDOMConstants.TYPE_FILTER_POOL_REFERENCE, AB_FILTER_POOL_REFERENCE);
		typeQualifiers.put(IRSEDOMConstants.TYPE_FILTER_STRING, AB_FILTER_STRING);
		typeQualifiers.put(IRSEDOMConstants.TYPE_HOST, AB_HOST);
		typeQualifiers.put(IRSEDOMConstants.TYPE_PROFILE, AB_PROFILE);
		typeQualifiers.put(IRSEDOMConstants.TYPE_PROPERTY, AB_PROPERTY);
		typeQualifiers.put(IRSEDOMConstants.TYPE_PROPERTY_SET, AB_PROPERTY_SET);
		typeQualifiers.put(IRSEDOMConstants.TYPE_SERVER_LAUNCHER, AB_SERVICE_LAUNCHER);
		typeQualifiers.put(IRSEDOMConstants.TYPE_SUBSYSTEM, AB_SUBSYSTEM);
		return typeQualifiers;
	}

	/**
	 * Write a set of properties to a file.
	 * @param properties The Properties object to write.
	 * @param header The header to include in the properties file.
	 * @param file The IFile which will contain the properties.
	 * @param monitor The progress monitor.
	 */
	private void writeProperties(Properties properties, String header, IFile file) {
//		System.out.println("writing "+file.getFullPath()+"...");
		ByteArrayOutputStream outStream = new ByteArrayOutputStream(500);
		PrintWriter out = new PrintWriter(outStream);
		out.println("# " + header); //$NON-NLS-1$
		Map map = new TreeMap(properties);
		Set keys = map.keySet();
		
		for (Iterator z = keys.iterator(); z.hasNext();) {
			String key = (String) z.next();
			String value = (String)map.get(key);
			String keyvalue = key + "=" + escapeValue(value); //$NON-NLS-1$
//			System.out.println("writing "+keyvalue);
			out.println(keyvalue);
		}
		out.close();
//		System.out.println("...wrote "+file.getFullPath());
		ByteArrayInputStream inStream = new ByteArrayInputStream(outStream.toByteArray());
		try {
			if (!file.exists()) {
				file.create(inStream, true, null);
			} else {
				file.setContents(inStream, true, true, null);
			}
		} catch (CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * Tests if a node's definition should be embedded in its parent's definition or
	 * if it should be a separate definition. The test is usually based on node type.
	 * Currently only filter strings are embedded in their parent filter definition.
	 * @param node The node to be tested.
	 * @return true if the node is to be embedded.
	 */
	private boolean isNodeEmbedded(RSEDOMNode node) {
		Set embeddedTypes = new HashSet();
		embeddedTypes.add(IRSEDOMConstants.TYPE_FILTER);
		embeddedTypes.add(IRSEDOMConstants.TYPE_CONNECTOR_SERVICE);
		boolean result = embeddedTypes.contains(node.getType());
		return result;
	}

	/**
	 * Transforms an integer into its five digit counterpart complete with leading 
	 * zeroes.
	 * @param i an integer from 0 to 99999.
	 * @return a string equivalent from "00000" to "99999"
	 */
	private String getIndexString(int i) {
		assert (i >= 0 && i <=99999);
		String index = "00000" + Integer.toString(i); //$NON-NLS-1$
		index = index.substring(index.length() - 5);
		return index;
	}

	/**
	 * "Fixes" a value. Values in Properties objects may not be null. Changes all
	 * null values to the string "null" and escapes the characters in other values.
	 * @param value The value to check.
	 * @return The fixed value
	 */
	private String fixValue(String value) {
		if (value == null) return NULL_VALUE_STRING;
		return value;
	}

	/**
	 * Escapes the characters in the supplied string according to the rules
	 * for properties files.
	 * @param value The value to examine.
	 * @return The equivalent value with escapes.
	 */
	private String escapeValue(String value) {
		StringBuffer buffer = new StringBuffer(value.length() + 20);
		char[] characters = value.toCharArray();
		for (int i = 0; i < characters.length; i++) {
			char c = characters[i];
			if (c == '\\') {
				buffer.append("\\\\"); //$NON-NLS-1$
			} else if (c == '\t') {
				buffer.append("\\t"); //$NON-NLS-1$
			} else if (c == '\f') {
				buffer.append("\\f"); //$NON-NLS-1$
			} else if (c == '\n') {
				buffer.append("\\n"); //$NON-NLS-1$
			} else if (c == '\r') {
				buffer.append("\\r"); //$NON-NLS-1$
			} else if ((c < '\u0020' && c > '\u007E')) {
				String cString = "0000" + Integer.toHexString(c); //$NON-NLS-1$
				cString = cString.substring(cString.length() - 4);
				cString = "\\u" + cString; //$NON-NLS-1$
				buffer.append(cString);
			} else if ("=!#:".indexOf(c) >= 0) { //$NON-NLS-1$
				buffer.append('\\');
				buffer.append(c);
			} else {
				buffer.append(c);
			}
		}
		return buffer.toString();
	}

	/**
	 * Constructs a properties object containing all the properties for this node.
	 * The following properties exist:
	 * name property, type property, embedded child properties, and referenced 
	 * child properties. Each property has its own key format in the Properties
	 * object.  
	 * @param node The node to extract the properties from.
	 * @param force Force children to be embedded rather than reference.
	 * @param monitor The progress monitor. The work count is increased by one each time
	 * this method is invoked.
	 * @return The Properties object containing that node definition.
	 */
	private Properties getProperties(RSEDOMNode node, boolean force, IProgressMonitor monitor) {
		Properties properties = new Properties();
		properties.put(MT_NODE_NAME[0], node.getName());
		properties.put(MT_NODE_TYPE[0], node.getType());
		properties.putAll(getAttributes(node));
		RSEDOMNode[] children = node.getChildren();
		for (int i = 0; i < children.length; i++) {
			RSEDOMNode child = children[i];
			String index = getIndexString(i);
			if (force || isNodeEmbedded(child)) {
				String prefix = combine(MT_CHILD[0], index);
				Properties childProperties = getProperties(child, true, monitor); 
				Enumeration e = childProperties.keys();
				while (e.hasMoreElements()) {
					String key = (String) e.nextElement();
					String value = childProperties.getProperty(key);
					String newKey = combine(prefix, key);
					properties.put(newKey, value);
				}
			}
		}
		if (monitor != null) monitor.worked(1);
		return properties;
	}
	
	/**
	 * Constructs a Properties object from the attributes present in a DOM node.
	 * @param node The node containing the attributes. Keys for attributes are of the
	 * form "attribute.[attribute-name]". If the attribute has a type then there
	 * will also be an "attribute-type.[attribute-name]" property.
	 * [attribute-name] may contain periods but must not be null.
	 * @return The newly constructed Properties object.
	 */
	private Properties getAttributes(RSEDOMNode node) {
		Properties properties = new Properties();
		RSEDOMNodeAttribute[] attributes = node.getAttributes();
		for (int i = 0; i < attributes.length; i++) {
			RSEDOMNodeAttribute attribute = attributes[i];
			String attributeName = attribute.getKey();
			String propertyKey = combine(MT_ATTRIBUTE[0], attributeName);
			properties.put(propertyKey, fixValue(attribute.getValue()));
			String attributeType = attribute.getType();
			if (attributeType != null) {
				propertyKey = combine(MT_ATTRIBUTE_TYPE[0], attributeName);
				properties.put(propertyKey, attributeType);
			} 
		}
		return properties;
	}
	
	/**
	 * Count the number of nodes in a tree rooted in the supplied node. The
	 * supplied node is counted so the mininum result is one.
	 * @param node The root of the tree.
	 * @return The node count.
	 */
	private int countNodes(RSEDOMNode node) {
		RSEDOMNode[] children = node.getChildren();
		int result = 1;
		for (int i = 0; i < children.length; i++) {
			RSEDOMNode child = children[i];
			result += countNodes(child);
		}
		return result;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.rse.persistence.IRSEPersistenceProvider#loadRSEDOM(org.eclipse.rse.model.ISystemProfileManager, java.lang.String, org.eclipse.core.runtime.IProgressMonitor)
	 */
	public RSEDOM loadRSEDOM(String profileName, IProgressMonitor monitor) {
		RSEDOM dom = null;
		IFolder profileFolder = getProfileFolder(profileName);
		if (profileFolder.exists()) {
			//System.out.println("loading from " + profileFolder.getFullPath().toString() + "...");
			int n = countPropertiesFiles(profileFolder);
			if (monitor != null) monitor.beginTask(Messages.PropertyFileProvider_LoadingTaskName, n);
			dom = (RSEDOM) loadNode(null, profileFolder, monitor);
			if (monitor != null) monitor.done();
		} else {
			//System.out.println(profileFolder.getFullPath().toString() + " does not exist.");
		}
		return dom;
	}

	/**
	 * Counts the number of properties files in this folder and below. This provides
	 * a lower bound to the number of nodes that have to be created from this 
	 * persistent form of a DOM.
	 * @param folder
	 * @return the number of properties files found.
	 */
	private int countPropertiesFiles(IFolder folder) {
		int result = 0;
		IFile propertiesFile = folder.getFile(PROPERTIES_FILE_NAME);
		if (propertiesFile.exists()) {
			result += 1;
			try {
				IResource[] members = folder.members();
				for (int i = 0; i < members.length; i++) {
					IResource member = members[i];
					if (member.getType() == IResource.FOLDER) {
						IFolder childFolder = (IFolder) member;
						result += countPropertiesFiles(childFolder);
					}
				}
			} catch (CoreException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return result;
	}

	/**
	 * Loads a node from a folder.
	 * @param parent The parent of the node to be created. If null then this node is assumed to
	 * be a DOM root node (RSEDOM).
	 * @param nodeFolder The folder in which the node.properties file of this node is found.
	 * @param monitor The monitor used to report progress and cancelation. If the monitor is
	 * in canceled state then it this does method does nothing and returns null. If the monitor
	 * is not canceled then its work count is incremented by one.
	 * @return The newly loaded node.
	 */
	private RSEDOMNode loadNode(RSEDOMNode parent, IFolder nodeFolder, IProgressMonitor monitor) {
		RSEDOMNode node = null;
		if (monitor == null || !monitor.isCanceled()) {
			Properties properties = loadProperties(nodeFolder);
			if (properties != null) {
				node = makeNode(parent, nodeFolder, properties, monitor);
			}
			if (monitor != null) monitor.worked(1);
		}
		return node;
	}
	
	/**
	 * Loads the properties found in the folder. Returns null if no properties
	 * file was found.
	 * @param folder The folder in which to look for properties.
	 * @return The Properties object.
	 */
	private Properties loadProperties(IFolder folder) {
		Properties properties = null;
		IFile attributeFile = folder.getFile(PROPERTIES_FILE_NAME);
		if (attributeFile.exists()) {
			properties = new Properties();
			try {
				InputStream inStream = attributeFile.getContents();
				try {
					properties.load(inStream);
					inStream.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} catch (CoreException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return properties;
	}
	
	/**
	 * Makes a new RSEDOMNode from a set of properties. The properties must (at least) include
	 * a "name" property and a "type" property. Any child nodes are created and attached as well.
	 * @param parent The parent node of the node to be created.
	 * @param nodeFolder The folder in which referenced child folders can be found. This will
	 * almost always be the folder in which the properties for the node to be created were found.
	 * @param properties The properties from which to create the node.
	 * @param monitor a monitor to support cancelation and progress reporting.
	 * @return the newly created DOM node and its children.
	 */
	private RSEDOMNode makeNode(RSEDOMNode parent, IFolder nodeFolder, Properties properties, IProgressMonitor monitor) {
		String nodeType = getProperty(properties, MT_NODE_TYPE);
		String nodeName = getProperty(properties, MT_NODE_NAME);
		RSEDOMNode node = (parent == null) ? new RSEDOM(nodeName) : new RSEDOMNode(parent, nodeType, nodeName);
		node.setRestoring(true);
		Set keys = properties.keySet();
		Map attributes = new HashMap();
		Map attributeTypes = new HashMap();
		Map childPropertiesMap = new HashMap();
		Set childNames = new TreeSet(); // child names are 5 digit strings, a tree set is used to maintain ordering
		Set referenceKeys = new TreeSet(); // ditto for reference keys, "<reference-metatype>.<index>"
		// since the properties are in no particular  order, we make a first pass to gather info on what's there
		for (Iterator z = keys.iterator(); z.hasNext();) {
			String key = (String) z.next();
			String[] words = split(key, 2);
			String metatype = words[0];
			if (find(metatype, MT_ATTRIBUTE)) {
				String value = properties.getProperty(key);
				attributes.put(words[1], value);
			} else if (find(metatype, MT_ATTRIBUTE_TYPE)) {
				String type = properties.getProperty(key);
				attributeTypes.put(words[1], type);
			} else if (find(metatype, MT_REFERENCE)) {
				referenceKeys.add(key);
			} else if (find(metatype, MT_CHILD)) {
				String value = properties.getProperty(key);
				words = split(words[1], 2);
				String childName = words[0];
				childNames.add(childName);
				String newKey = words[1]; 
				Properties p = getProperties(childPropertiesMap, childName);
				p.put(newKey, value);
			}
		}
		Set attributeNames = attributes.keySet();
		for (Iterator z = attributeNames.iterator(); z.hasNext();) {
			String attributeName = (String) z.next();
			String attributeValue = (String) attributes.get(attributeName);
			if (attributeValue.equals(NULL_VALUE_STRING)) attributeValue = null;
			String attributeType = (String) attributeTypes.get(attributeName);
			node.addAttribute(attributeName, attributeValue, attributeType);
		}
		for (Iterator z = childNames.iterator(); z.hasNext();) {
			String childName = (String) z.next();
			Properties p = getProperties(childPropertiesMap, childName);
			makeNode(node, nodeFolder, p, monitor);
		}
		for (Iterator z = referenceKeys.iterator(); z.hasNext();) {
			String key = (String) z.next();
			String childFolderName = properties.getProperty(key);
			IFolder childFolder = getFolder(nodeFolder, childFolderName);
			loadNode(node, childFolder, monitor);
		}
		node.setRestoring(false);
		return node;
	}
	
	/**
	 * Gets a property given a "multi-key"
	 * @param properties The properties object to search
	 * @param keys the "multi-key" for that property
	 * @return The first property found using one of the elements of the multi-key.
	 */
	private String getProperty(Properties properties, String[] keys) {
		String result = null;
		for (int i = 0; i < keys.length && result == null; i++) {
			String key = keys[i];
			result = properties.getProperty(key);
		}
		return result;
	}
	
	/**
	 * Finds a key (needle) in an array of values (the haystack)
	 * @param needle The value to look for
	 * @param haystack the values to search
	 * @return true if the value was found
	 */
	private boolean find(String needle, String[] haystack) {
		for (int i = 0; i < haystack.length; i++) {
			String value = haystack[i];
			if (value.equals(needle)) return true;
		}
		return false;
	}

	/**
	 * Returns a Properties object from the given Map that holds them using the 
	 * selector String. Creates a new Properties object and places it in the map
	 * if one does not exist for the selector.
	 * @param propertiesMap The map in which to look for Properties objects.
	 * @param selector The name of the Properties object
	 * @return a Properties object.
	 */
	private Properties getProperties(Map propertiesMap, String selector) {
		Properties p = (Properties)propertiesMap.get(selector);
		if (p == null) {
			p = new Properties();
			propertiesMap.put(selector, p);
		}
		return p;
	}

	/**
	 * Returns the IFolder in which this persistence provider stores its profiles.
	 * This will create the folder if the folder was not found.
	 * @return The folder that was created or found.
	 */
	private IFolder getProviderFolder() {
		IProject project = RSEPersistenceManager.getRemoteSystemsProject();
		try {
			project.refreshLocal(IResource.DEPTH_INFINITE, null); 
		} catch (Exception e) {
		}
		//IFolder providerFolder = getFolder(project, "org.eclipse.rse.dom.properties");
		IFolder providerFolder = getFolder(project, "dom.properties"); //$NON-NLS-1$
		return providerFolder;
	}
	

	/**
	 * Returns the IFolder in which a profile is stored. 
	 * @return The folder that was created or found.
	 */
	private IFolder getProfileFolder(String profileName) {
		String profileFolderName = combine(AB_PROFILE, freeze(profileName));
		IFolder providerFolder = getProviderFolder();
		IFolder profileFolder = getFolder(providerFolder, profileFolderName);
		return profileFolder;
	}
	
	/**
	 * Returns the specified folder of the parent container. If the folder does
	 * not exist it creates it.
	 * @param parent the parent container - typically a project or folder
	 * @param name the name of the folder to find or create
	 * @return the found or created folder
	 */
	private IFolder getFolder(IContainer parent, String name) {
		IPath path = new Path(name);
		IFolder folder = parent.getFolder(path);
		if (!folder.exists()) {
			try {
				folder.create(IResource.NONE, true, null);
			} catch (CoreException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return folder;
	}
	
	/**
	 * Convenience method to combine two names into one. The individual names in the
	 * combined name are separated by periods.
	 * @param typeName The first name.
	 * @param nodeName The second name
	 * @return the combined name.
	 */
	private String combine(String typeName, String nodeName) {
		return combine(new String[] {typeName, nodeName});
	}
	
	/**
	 * The generic method for creating a qualified name from a string of segments.
	 * The individual names are separated by periods.
	 * @param names The names to combine
	 * @return The combined name.
	 */
	private String combine(String[] names) {
		StringBuffer buf = new StringBuffer(100);
		for (int i = 0; i < names.length; i++) {
			String name = names[i];
			if (i > 0) buf.append('.');
			buf.append(name);
		}
		return buf.toString();
	}
	
	/**
	 * Splits a combined name into its component parts. The period is used as the name 
	 * separator. If a limit > 0 is specified the return value will contain at most that
	 * number of segments. The last segment may, in fact, be split some more.
	 * @param longName The name to be split
	 * @param limit The number of parts to split the name into.
	 * @return The parts of the name.
	 */
	private String[] split(String longName, int limit) {
		return period.split(longName, limit);
	}
}