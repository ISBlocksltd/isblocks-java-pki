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

package org.ejbca.ui.cli.ra;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.cesecore.authorization.AuthorizationDeniedException;
import org.cesecore.certificates.ca.CADoesntExistsException;
import org.cesecore.certificates.ca.CAInfo;
import org.cesecore.certificates.ca.CaSessionRemote;
import org.cesecore.certificates.ca.IllegalNameException;
import org.cesecore.certificates.certificate.CertificateConstants;
import org.cesecore.certificates.certificate.exception.CertificateSerialNumberException;
import org.cesecore.certificates.certificateprofile.CertificateProfile;
import org.cesecore.certificates.certificateprofile.CertificateProfileConstants;
import org.cesecore.certificates.certificateprofile.CertificateProfileExistsException;
import org.cesecore.certificates.certificateprofile.CertificateProfileSessionRemote;
import org.cesecore.certificates.endentity.EndEntityConstants;
import org.cesecore.certificates.endentity.EndEntityType;
import org.cesecore.certificates.endentity.EndEntityTypes;
import org.cesecore.configuration.GlobalConfigurationSessionRemote;
import org.cesecore.util.EjbRemoteHelper;
import org.ejbca.config.GlobalConfiguration;
import org.ejbca.core.ejb.ra.EndEntityExistsException;
import org.ejbca.core.ejb.ra.EndEntityManagementSessionRemote;
import org.ejbca.core.ejb.ra.raadmin.EndEntityProfileSessionRemote;
import org.ejbca.core.model.SecConst;
import org.ejbca.core.model.approval.ApprovalException;
import org.ejbca.core.model.approval.WaitingForApprovalException;
import org.ejbca.core.model.authorization.AccessRulesConstants;
import org.ejbca.core.model.ra.CustomFieldException;
import org.ejbca.core.model.ra.raadmin.EndEntityProfile;
import org.ejbca.core.model.ra.raadmin.EndEntityProfileExistsException;
import org.ejbca.core.model.ra.raadmin.EndEntityProfileNotFoundException;
import org.ejbca.core.model.ra.raadmin.EndEntityProfileValidationException;
import org.ejbca.ui.cli.infrastructure.command.CommandResult;
import org.ejbca.ui.cli.infrastructure.parameter.Parameter;
import org.ejbca.ui.cli.infrastructure.parameter.ParameterContainer;
import org.ejbca.ui.cli.infrastructure.parameter.enums.MandatoryMode;
import org.ejbca.ui.cli.infrastructure.parameter.enums.ParameterMode;
import org.ejbca.ui.cli.infrastructure.parameter.enums.StandaloneMode;

import com.keyfactor.util.StringTools;

/**
 * Adds an end entity to the database.
 */
public class AddEndEntityCommand extends BaseRaCommand {

    private static final Logger log = Logger.getLogger(AddEndEntityCommand.class);

    private static final String USERGENERATED = "USERGENERATED";
    private static final String P12 = "P12";
    private static final String JKS = "JKS";
    private static final String PEM = "PEM";
    private static final String BCFKS = "BCFKS";
    private static final String OLD_SUBCOMMAND = "adduser";
    private static final String SUBCOMMAND = "addendentity";

    private static final String[] SOFT_TOKEN_NAMES = { USERGENERATED, P12, JKS, PEM, BCFKS };
    private static final int[] SOFT_TOKEN_IDS = { SecConst.TOKEN_SOFT_BROWSERGEN, SecConst.TOKEN_SOFT_P12, SecConst.TOKEN_SOFT_JKS,
            SecConst.TOKEN_SOFT_PEM, SecConst.TOKEN_SOFT_BCFKS };

    private static final Set<String> ALIASES = new HashSet<String>();
    static {
        ALIASES.add(OLD_SUBCOMMAND);
    }

    private static final String USERNAME_KEY = "--username";
    private static final String PASSWORD_KEY = "--password";
    private static final String DN_KEY = "--dn";
    private static final String CA_NAME_KEY = "--caname";
    private static final String TYPE_KEY = "--type";
    private static final String TOKEN_KEY = "--token";
    private static final String SUBJECT_ALT_NAME_KEY = "--altname";
    private static final String EMAIL_KEY = "--email";
    private static final String CERT_PROFILE_KEY = "--certprofile";
    private static final String EE_PROFILE_KEY = "--eeprofile";
    private static final String VALIDITY = "--validity";
    private static final String CLIENT_AUTHENTICATION_CERTPROFILE_NAME = "ClientAuthenticationCP";
    private static final String CLIENT_AUTHENTICATION_EEPROFILE_NAME = "ClientAuthenticationEEP";
    
    private final GlobalConfiguration globalConfiguration = (GlobalConfiguration) EjbRemoteHelper.INSTANCE.getRemoteSession(
            GlobalConfigurationSessionRemote.class).getCachedConfiguration(GlobalConfiguration.GLOBAL_CONFIGURATION_ID);
    
    {
        registerParameter(new Parameter(USERNAME_KEY, "Username", MandatoryMode.MANDATORY, StandaloneMode.ALLOW, ParameterMode.ARGUMENT,
                "Username for the new end entity."));
        registerParameter(new Parameter(PASSWORD_KEY, "Password", MandatoryMode.OPTIONAL, StandaloneMode.FORBID, ParameterMode.ARGUMENT,
                "Password for the new end entity. Will be prompted for if not set. Set to 'null' for empty password (used for auto-generated passwords)."));
        registerParameter(new Parameter(DN_KEY, "DN", MandatoryMode.MANDATORY, StandaloneMode.ALLOW, ParameterMode.ARGUMENT,
                "DN is of form \"C=SE, O=MyOrg, OU=MyOrgUnit, CN=MyName\" etc. " + "\nAn LDAP escaped DN is for example:\n"
                        + "DN: CN=Tomas Gustavsson, O=PrimeKey Solutions, C=SE\n"
                        + "LDAP escaped DN: CN=Tomas Gustavsson\\, O=PrimeKey Solutions\\, C=SE"));
        registerParameter(new Parameter(CA_NAME_KEY, "CA Name", MandatoryMode.MANDATORY, StandaloneMode.ALLOW, ParameterMode.ARGUMENT,
                "CA issuing this End Entity."));
        registerParameter(new Parameter(SUBJECT_ALT_NAME_KEY, "Subject Name", MandatoryMode.OPTIONAL, StandaloneMode.FORBID, ParameterMode.ARGUMENT,
                "SubjectAltName is of form \"rfc822Name=<email>, dNSName=<host name>, uri=<http://host.com/>,"
                        + " ipaddress=<address>, upn=<MS UPN>, guid=<MS globally unique id>, directoryName=<LDAP escaped DN>,"
                        + " krb5principal=<Krb5 principal name>, permanentIdentifier=<Permanent Identifier values>,"
                        + " subjectIdentificationMethod=<Subject Identification Method values or parameters>,"
                        + " registeredID=<object identifier>,"
                        + " xmppAddr=<RFC6120 XmppAddr>, srvName=<RFC4985 SRVName>, fascN=<FIPS 201-2 PIV FASC-N>\""));
        registerParameter(new Parameter(EMAIL_KEY, "E-Mail", MandatoryMode.OPTIONAL, StandaloneMode.FORBID, ParameterMode.ARGUMENT,
                "E-Mail of the new end entity."));
        registerParameter(new Parameter(TYPE_KEY, "Type", MandatoryMode.MANDATORY, StandaloneMode.ALLOW, ParameterMode.ARGUMENT,
                "Type (mask): INVALID=0; END-USER=1; " + (globalConfiguration.getEnableKeyRecovery() ? "KEYRECOVERABLE=128; " : "")
                        + "SENDNOTIFICATION=256; PRINTUSERDATA=512"));
        registerParameter(new Parameter(TOKEN_KEY, "Token", MandatoryMode.MANDATORY, StandaloneMode.ALLOW, ParameterMode.ARGUMENT,
                "Desired token type for the end entity: USERGENERATED, P12, JKS, PEM, BCFKS."));
        registerParameter(new Parameter(CERT_PROFILE_KEY, "Profile Name", MandatoryMode.OPTIONAL, StandaloneMode.FORBID, ParameterMode.ARGUMENT,
                "The certificate profile, will default to End User."));
        registerParameter(new Parameter(EE_PROFILE_KEY, "Profile Name", MandatoryMode.OPTIONAL, StandaloneMode.FORBID, ParameterMode.ARGUMENT,
                "The end entity profile, will default to Empty."));
        registerParameter(new Parameter(VALIDITY, "Validity", MandatoryMode.OPTIONAL, StandaloneMode.FORBID, ParameterMode.HIDDEN,
                "The validity of the end user certificate. Providing this option will result in creation of an EE profile and a Certificate Profile"
                + "which are going to be used for custom end entity's validity."));
    }

    @Override
    public Set<String> getMainCommandAliases() {
        return ALIASES;
    }

    @Override
    public String getMainCommand() {
        return SUBCOMMAND;
    }

    @Override
    public CommandResult execute(ParameterContainer parameters) {
        final String username = parameters.get(USERNAME_KEY);
        final String password = getAuthenticationCode(parameters.get(PASSWORD_KEY));
        final String dn = parameters.get(DN_KEY);
        final String caname = parameters.get(CA_NAME_KEY);
        final String subjectaltname = parameters.get(SUBJECT_ALT_NAME_KEY);
        final String email = parameters.get(EMAIL_KEY);
        final EndEntityType type;
        final String tokenString = parameters.get(TYPE_KEY);
        final String validity = parameters.get(VALIDITY);
        
        try {
            type = new EndEntityType(EndEntityTypes.getTypesFromHexCode(Integer.parseInt(tokenString)));
        } catch (NumberFormatException e) {
            log.error("ERROR: Invalid type: " + tokenString);
            return CommandResult.FUNCTIONAL_FAILURE;
        }
        String tokenname = parameters.get(TOKEN_KEY);

        boolean error = false;

        int caid = 0;
        try {
            CAInfo caInfo = EjbRemoteHelper.INSTANCE.getRemoteSession(CaSessionRemote.class).getCAInfo(getAuthenticationToken(), caname);
            if (caInfo != null) {
                // let it be 0 if not found, we will print a suitable error message below
                caid = caInfo.getCAId();
            }
        } catch (AuthorizationDeniedException e) {
            log.error("CLI user not authorized to CA " + caname);
            error = true;
        }
        int certificatetypeid = CertificateProfileConstants.CERTPROFILE_FIXED_ENDUSER;
        
        if(validity != null) {
            CertificateProfile clonedCertProfile = new CertificateProfile(certificatetypeid);
            clonedCertProfile.setEncodedValidity(validity);
            try {
                certificatetypeid = EjbRemoteHelper.INSTANCE.getRemoteSession(CertificateProfileSessionRemote.class).addCertificateProfile(getAuthenticationToken(), CLIENT_AUTHENTICATION_CERTPROFILE_NAME, clonedCertProfile);
            } catch (CertificateProfileExistsException | AuthorizationDeniedException e) {
                getLogger().error("Failed to create the certificate profile for the modified super admin validity of " + validity);
                error = true;
            }
        }
        
        final String certificateProfile = parameters.get(CERT_PROFILE_KEY);
        if (certificateProfile != null) {
            if (validity != null) {
                getLogger().error("The certificate profile is not meant to be used in combination with validity! Exiting!");
                return CommandResult.FUNCTIONAL_FAILURE;
            }
            // Use certificate type, no end entity profile.
            certificatetypeid = EjbRemoteHelper.INSTANCE.getRemoteSession(CertificateProfileSessionRemote.class).getCertificateProfileId(
                    certificateProfile);
        }
        getLogger().info(
                "Using certificate profile: "
                        + EjbRemoteHelper.INSTANCE.getRemoteSession(CertificateProfileSessionRemote.class).getCertificateProfileName(
                                certificatetypeid) + ", with id: " + certificatetypeid);

        final String endEntityProfile = parameters.get(EE_PROFILE_KEY);
        int endEntityProfileId = EndEntityConstants.EMPTY_END_ENTITY_PROFILE;
        
        if (validity != null) {
            EndEntityProfile clonedEEProfile = new EndEntityProfile(true);
            List<Integer> defaultAvailableCertProfileIds = clonedEEProfile.getAvailableCertificateProfileIds();
            defaultAvailableCertProfileIds.add(certificatetypeid);
            clonedEEProfile.setAvailableCertificateProfileIds(defaultAvailableCertProfileIds);
            clonedEEProfile.setDefaultCertificateProfile(certificatetypeid);
            try {
                endEntityProfileId = EjbRemoteHelper.INSTANCE.getRemoteSession(EndEntityProfileSessionRemote.class).addEndEntityProfile(getAuthenticationToken(), CLIENT_AUTHENTICATION_EEPROFILE_NAME, clonedEEProfile);
            } catch (EndEntityProfileExistsException | AuthorizationDeniedException e) {
                getLogger().error("Failed to create the end entity profile for the modified super admin validity of " + validity);
                error = true;
            }
        }
        
        if (endEntityProfile != null) {
            if (validity != null) {
                getLogger().error("The end entity profile is not meant to be used in combination with validity! Exiting!");
                return CommandResult.FUNCTIONAL_FAILURE;
            }
            try {
                endEntityProfileId = EjbRemoteHelper.INSTANCE.getRemoteSession(EndEntityProfileSessionRemote.class).getEndEntityProfileId(
                        endEntityProfile);
            } catch (EndEntityProfileNotFoundException e) {
                getLogger().error("ERROR: Could not find end entity profile in database.");
                error = true;
            }
            getLogger().info("Using entity profile: " + endEntityProfile + ", with id: " + endEntityProfileId);
        }

        int tokenid = getTokenId(tokenname);
        if (tokenid == 0) {
            getLogger().error("Invalid token id.");
            error = true;
        }

        if (certificatetypeid == CertificateProfileConstants.CERTPROFILE_NO_PROFILE) {
            // Certificate profile not found in database.
            getLogger().error("Could not find certificate profile in database.");
            error = true;
        }

        if (caid == 0) { // CA not found i database.
            getLogger().error("Could not find CA '" + caname + "' in database.");
            error = true;
        }
        
        if (email == null && type.contains(EndEntityTypes.SENDNOTIFICATION)) {
            getLogger().error("Email field cannot be null when send notification type is given.");
            error = true;
        }

        // Check if username already exists.
        if (EjbRemoteHelper.INSTANCE.getRemoteSession(EndEntityManagementSessionRemote.class).existsUser(username)) {
            getLogger().error("ERROR: User '" + username + "' already exists in the database.");
            error = true;
        }

        if (!error) {
            getLogger().info("Trying to add end entity:");
            getLogger().info("Username: " + username);
            getLogger().info("Password: <password hidden>");
            getLogger().info("DN: " + dn);
            getLogger().info("CA Name: " + caname);
            getLogger().info("SubjectAltName: " + subjectaltname);
            getLogger().info("Email: " + email);
            getLogger().info("Type: " + type.getHexValue());
            getLogger().info("Token: " + tokenname);
            getLogger().info("Certificate profile: " + certificatetypeid);
            getLogger().info("End entity profile: " + endEntityProfileId);
            try {
                EjbRemoteHelper.INSTANCE.getRemoteSession(EndEntityManagementSessionRemote.class).addUser(getAuthenticationToken(), username,
                        password, dn, subjectaltname, email, false, endEntityProfileId, certificatetypeid, type, tokenid, caid);
                getLogger().info("User '" + username + "' has been added.");
                getLogger().info("Note: If batch processing should be possible, also use 'ra setclearpwd " + username + " <pwd>'.");
                return CommandResult.SUCCESS;
            } catch (AuthorizationDeniedException e) {
                getLogger().error(e.getMessage());
                return CommandResult.AUTHORIZATION_FAILURE;
            } catch (EndEntityProfileValidationException e) {
                getLogger().error("Given userdata doesn't fulfill end entity profile. : " + e.getMessage());
            } catch (WaitingForApprovalException e) {
                getLogger().error("\nOperation pending, waiting for approval: " + e.getMessage());
            } catch (ApprovalException e) {
                getLogger().error("\nApproval exception: " + e.getMessage());
            }  catch (EndEntityExistsException e) {
                log.error("ERROR: End entity already exists.");
            } catch (CADoesntExistsException e) {
                throw new IllegalStateException("Should not happen, CA has already been checked.", e);
            } catch (CustomFieldException | IllegalNameException | CertificateSerialNumberException e) {
                getLogger().error(e.getMessage());
            }
        }
        return CommandResult.FUNCTIONAL_FAILURE;

    }

    /**
     * Returns the tokenid type of the user, returns 0 if invalid tokenname.
     */
    private int getTokenId(String tokenname) {
        int returnval = 0;
        
        for (int i = 0; i < SOFT_TOKEN_NAMES.length; i++) {
            if (SOFT_TOKEN_NAMES[i].equals(tokenname)) {
                returnval = SOFT_TOKEN_IDS[i];
                break;
            }
        }
        return returnval;
    }

    @Override
    public String getCommandDescription() {
        return "Adds an end entity";
    }

    @Override
    public String getFullHelpText() {
        StringBuilder sb = new StringBuilder();
        sb.append(getCommandDescription()).append("\n");
        StringBuilder existingCas = new StringBuilder();
        //The below require quite a few database operations, so shouldn't be run unless the help text has been called upon.
        for (String caName : EjbRemoteHelper.INSTANCE.getRemoteSession(CaSessionRemote.class).getActiveCANames(getAuthenticationToken())) {
            existingCas.append((existingCas.length() == 0 ? "" : ", ") + caName);
        }
        sb.append("Existing cas: " + existingCas + "\n");

        Map<Integer, String> certificateprofileidtonamemap = EjbRemoteHelper.INSTANCE.getRemoteSession(CertificateProfileSessionRemote.class)
                .getCertificateProfileIdToNameMap();
        StringBuilder existingCps = new StringBuilder();
        for (Integer id : EjbRemoteHelper.INSTANCE.getRemoteSession(CertificateProfileSessionRemote.class).getAuthorizedCertificateProfileIds(
                getAuthenticationToken(), CertificateConstants.CERTTYPE_ENDENTITY)) {
            existingCps.append((existingCps.length() == 0 ? "" : ", ") + certificateprofileidtonamemap.get(id));
        }
        sb.append("Existing certificate profiles: " + existingCps + "\n");

        
        sb.append("Existing tokens: " + USERGENERATED + ", " + P12 + ", " + JKS + ", " + PEM + ", " + BCFKS + ", \n");

        StringBuilder existingEeps = new StringBuilder();
        Map<Integer, String> endentityprofileidtonamemap = EjbRemoteHelper.INSTANCE.getRemoteSession(EndEntityProfileSessionRemote.class)
                .getEndEntityProfileIdToNameMap();
        for (Integer id : EjbRemoteHelper.INSTANCE.getRemoteSession(EndEntityProfileSessionRemote.class).getAuthorizedEndEntityProfileIds(
                getAuthenticationToken(), AccessRulesConstants.CREATE_END_ENTITY)) {
            existingEeps.append((existingEeps.length() == 0 ? "" : ", ") + endentityprofileidtonamemap.get(id));
        }
        sb.append("Existing endentity profiles: " + existingEeps + "\n");
        
        return sb.toString();
    }

    @Override
    protected Logger getLogger() {
        return log;
    }

    private String getAuthenticationCode(final String commandLineArgument) {
        final String authenticationCode;
        if (commandLineArgument == null) {
            getLogger().info("Enter password: ");
            getLogger().info("");
            authenticationCode = StringTools.passwordDecryption(String.valueOf(System.console().readPassword()), "End Entity Password");
        } else if ("null".equalsIgnoreCase(commandLineArgument)) {
            getLogger().info("Using no End Entity Password.");
            authenticationCode = null;
        } else {
            authenticationCode = StringTools.passwordDecryption(commandLineArgument, "End Entity Password");
        }
        return authenticationCode;
    }

}
