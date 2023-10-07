/*************************************************************************
 *                                                                       *
 *  CESeCore: CE Security Core                                           *
 *                                                                       *
 *  This software is free software; you can redistribute it and/or       *
 *  modify it under the terms of the GNU Lesser General Public           *
 *  License as published by the Free Software Foundation; either         *
 *  version 2.1 of the License, or any later version.                    *
 *                                                                       *
 *  See terms of license at gnu.org.                                     *
 *                                                                       *
 *************************************************************************/
package org.cesecore.certificates.crl;

import java.util.Date;
import java.util.List;

import org.cesecore.authentication.tokens.AuthenticationToken;
import org.cesecore.authorization.AuthorizationDeniedException;

/** Session bean storing and retrieving CRLs
 * 
 */
public interface CrlStoreSession {
    
    /**
     * Retrieves the unique CRLData based on the finger print provided.
     * 
     * @param fingerprint
     * @return CRLData matching the given fingerprint
     */
    CRLData findByFingerprint(String fingerprint);
    
    /**
     * Retrieves list of CRLDatas based on the issuer DN provided.
     * 
     * @param issuerDN
     * @return list of CRLData entities matching the given issuerDN 
     */
    List<CRLData> findByIssuerDN(final String issuerDN);
    
    /**
     * Removes list of CRLDatas from DB based on the issuer DN provided.
     * 
     * @param issuerDN
     * @return
     */
    void removeByIssuerDN(final String issuerDN);

	/**
	 * Retrieves the latest CRL issued by this CA.
	 * 
	 * @param issuerdn the CRL issuers DN (CAs subject DN)
	 * @param crlPartitionIndex CRL partition index, or CertificateConstants.NO_CRL_PARTITION if partitioning is not used.
	 * @param deltaCRL true to get the latest deltaCRL, false to get the latest complete CRL
	 * @return byte[] with DER encoded X509CRL or null of no CRLs have been issued.
	 */
	byte[] getLastCRL(String issuerdn, int crlPartitionIndex, boolean deltaCRL);

    /**
     * Retrieves a specific CRL issued by this CA.
     * 
     * @param issuerdn the CRL issuers DN (CAs subject DN)
     * @param crlPartitionIndex CRL partition index, or CertificateConstants.NO_CRL_PARTITION if partitioning is not used.
     * @param crlNumber a crlNumber of a complete, or delta, CRL
     * @return byte[] with DER encoded X509CRL or null of no CRLs have been issued.
     */
    byte[] getCRL(String issuerdn, int crlPartitionIndex, int crlNumber);

	/**
	 * Retrieves the information about the latest CRL issued by this CA.
	 * Retrieves less information than getLastCRL, i.e. not the actual CRL data.
	 * 
	 * @param issuerdn the CRL issuers DN (CAs subject DN)
	 * @param crlPartitionIndex CRL partition index, or CertificateConstants.NO_CRL_PARTITION if partitioning is not used.
	 * @param deltaCRL true to get the latest deltaCRL, false to get the latest complete CRL
	 * @return CRLInfo of last CRL by CA or null if no CRL exists.
	 */
	CRLInfo getLastCRLInfo(String issuerdn, int crlPartitionIndex, boolean deltaCRL);
	
	/**
	 * Same as the above function {@link #getLastCRLInfo} except that it ignores the crl, making it a lightweight version suitable for GUI use.
	 * 
	 * @param issuerdn
	 * @param crlPartitionIndex
	 * @param deltaCRL
	 * @return
	 */
	CRLInfo getLastCRLInfoLightWeight(String issuerdn, int crlPartitionIndex, boolean deltaCRL);
	
	/**
	 * Retrieves the crl expire data (AKA nextUpdate)
	 * @param issuerDn
	 * @param crlPartitionIndex
	 * @param deltaCRL
	 * @return
	 */
    Date getCrlExpireDate(final String issuerDn, final int crlPartitionIndex, final boolean deltaCRL);

	/**
	 * Retrieves the information about the specified CRL. Retrieves less
	 * information than getLastCRL, i.e. not the actual CRL data.
	 * 
	 * @param fingerprint fingerprint of the CRL
	 * @return CRLInfo of CRL or null if no CRL exists.
	 */
	CRLInfo getCRLInfo(String fingerprint);

	/**
	 * Retrieves the highest CRLNumber issued by the CA.
	 * 
	 * @param issuerdn the subjectDN of a CA certificate
	 * @param crlPartitionIndex CRL partition index, or CertificateConstants.NO_CRL_PARTITION if partitioning is not used.
	 * @param deltaCRL true to get the latest deltaCRL, false to get the latest complete CRL
	 * @return the highest CRL number of CRLs issued by issuerdn, or 0 if no CRLs were issued.
	 */
	int getLastCRLNumber(String issuerdn, int crlPartitionIndex, boolean deltaCRL);

	/**
	 * Checks if any CRL has been created for the given CA.
	 * @param issuerDn Issuer DN of CA.
	 * @return true if at least one base CRL exists (partitioned or not)
	 */
	boolean crlExistsForCa(String issuerDn);

	/**
     * Stores a CRL
     *
	 * @param admin Administrator performing the operation
     * @param incrl  The DER coded CRL to be stored.
     * @param cafp   Fingerprint (hex) of the CAs certificate.
     * @param number CRL number.
     * @param issuerDN the issuer of the CRL
     * @param crlPartitionIndex CRL partition index, or CertificateConstants.NO_CRL_PARTITION if partitioning is not used.
     * @param thisUpdate when this CRL was created
     * @param nextUpdate when this CRL expires
     * @param deltaCRLIndicator -1 for a normal CRL and 1 for a deltaCRL
     * 
     * @throws CrlStoreException (rollback) if an error occured storing the CRL
     * @throws AuthorizationDeniedException (rollback) if admin was not authorized to store CRL
     */
    void storeCRL(AuthenticationToken admin, byte[] incrl, String cafp, int number, String issuerDN, int crlPartitionIndex, Date thisUpdate, Date nextUpdate, int deltaCRLIndicator)
    	throws CrlStoreException, AuthorizationDeniedException;
	
}
