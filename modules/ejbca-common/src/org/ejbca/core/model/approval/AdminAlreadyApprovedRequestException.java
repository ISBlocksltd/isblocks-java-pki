/*************************************************************************
 *                                                                       *
 *  EJBCA Community: The OpenSource Certificate Authority                *
 *                                                                       *
 *  This software is free software; you can redistribute it and/or       *
 *  modify it under the terms of the GNU Lesser General Public           *
 *  License as published by the Free Software Foundation; either         *
 *  version 2.1 of the License, or any later version.                    *
 *                                                                       *
 *  See terms of license at gnu.org.                                     *
 *                                                                       *
 *************************************************************************/
package org.ejbca.core.model.approval;

/**
 * Exception thrown  to show that an administrator has already approved a request.
 * 
 * @version $Id$
 */
public class AdminAlreadyApprovedRequestException extends Exception {

	private static final long serialVersionUID = 1L;

	public AdminAlreadyApprovedRequestException(String message) {
		super(message);
	}

}
