/********************************************************************************
 * Copyright (c) 2002, 2007 IBM Corporation and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is 
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Initial Contributors:
 * The following IBM employees contributed to the Remote System Explorer
 * component that contains this file: David McKnight, Kushal Munir, 
 * Michael Berger, David Dykstal, Phil Coulthard, Don Yantzi, Eric Simpson, 
 * Emily Bruner, Mazen Faraj, Adrian Storisteanu, Li Ding, and Kent Hawley.
 * 
 * Contributors:
 * Martin Oberhuber (Wind River) - [168975] Move RSE Events API to Core
 ********************************************************************************/

package org.eclipse.rse.core.events;

/**
 * The event IDs sent when remote resources in the model change
 * These IDs are used when creating SystemRemoteChangeEvent objects.  
 * 
 */
public interface ISystemRemoteChangeEvents 
{
	/**
	 * Event Type: a remote resource was added
	 * 
	 * The event stores the following event parameters:
	 * <ul> 
	 *  <li>resource - the remote resource object, or absolute name of the resource as would be given by calling getAbsoluteName on its remote adapter
	 *  <li>resourceParent - the remote resource's parent object, or absolute name, if that is known. If it is non-null, this will aid in refreshing occurrences of that parent.
	 *  <li>subsystem - the subsystem which contains this remote resource. This allows the search for impacts to be 
	 *   limited to subsystems of the same parent factory, and to connections with the same hostname as the subsystem's connection.
	 *  <li>oldName - on a rename operation, this is the absolute name of the resource prior to the rename
	 *  <li>originatingViewer - optional. If set, this gives the viewer a clue that it should select the affected resource after refreshing its parent. 
	 *    This saves sending a separate event to reveal and select the new created resource on a create event, for example.
	 * </ul>
	 */
	public static final int SYSTEM_REMOTE_RESOURCE_CREATED = 1;	

	/**
	 * Event Type: a remote resource was removed
	 * 
	 *  The event stores the following event parameters:
	 * <ul> 
	 *  <li>resource - the remote resource object, or absolute name of the resource as would be given by calling getAbsoluteName on its remote adapter
	 *  <li>resourceParent - the remote resource's parent object, or absolute name, if that is known. If it is non-null, this will aid in refreshing occurrences of that parent.
	 *  <li>subsystem - the subsystem which contains this remote resource. This allows the search for impacts to be 
	 *   limited to subsystems of the same parent factory, and to connections with the same hostname as the subsystem's connection.
	 *  <li>oldName - on a rename operation, this is the absolute name of the resource prior to the rename
	 *  <li>originatingViewer - optional. If set, this gives the viewer a clue that it should select the affected resource after refreshing its parent. 
	 *    This saves sending a separate event to reveal and select the new created resource on a create event, for example.
	 * </ul>
	 */
	public static final int SYSTEM_REMOTE_RESOURCE_DELETED = 2;	

	/**
	 * Event Type: a remote resource was changed
	 * 
	 *  The event stores the following event parameters:
	 * <ul> 
	 *  <li>resource - the remote resource object, or absolute name of the resource as would be given by calling getAbsoluteName on its remote adapter
	 *  <li>resourceParent - the remote resource's parent object, or absolute name, if that is known. If it is non-null, this will aid in refreshing occurrences of that parent.
	 *  <li>subsystem - the subsystem which contains this remote resource. This allows the search for impacts to be 
	 *   limited to subsystems of the same parent factory, and to connections with the same hostname as the subsystem's connection.
	 *  <li>oldName - on a rename operation, this is the absolute name of the resource prior to the rename
	 *  <li>originatingViewer - optional. If set, this gives the viewer a clue that it should select the affected resource after refreshing its parent. 
	 *    This saves sending a separate event to reveal and select the new created resource on a create event, for example.
	 * </ul>
	 */
	public static final int SYSTEM_REMOTE_RESOURCE_CHANGED = 4;	

	/**
	 * Event Type: a remote resource was renamed
	 * 
	 * The event stores the following event parameters:
	 * <ul> 
	 *  <li>resource - the remote resource object, or absolute name of the resource as would be given by calling getAbsoluteName on its remote adapter
	 *  <li>resourceParent - the remote resource's parent object, or absolute name, if that is known. If it is non-null, this will aid in refreshing occurrences of that parent.
	 *  <li>subsystem - the subsystem which contains this remote resource. This allows the search for impacts to be 
	 *   limited to subsystems of the same parent factory, and to connections with the same hostname as the subsystem's connection.
	 *  <li>oldName - on a rename operation, this is the absolute name of the resource prior to the rename
	 *  <li>originatingViewer - optional. If set, this gives the viewer a clue that it should select the affected resource after refreshing its parent. 
	 *    This saves sending a separate event to reveal and select the new created resource on a create event, for example.
	 * </ul>
	 */
	public static final int SYSTEM_REMOTE_RESOURCE_RENAMED = 8;
	

}