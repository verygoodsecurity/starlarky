package com.verygood.security.larky.modules.x509;


import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.x509.Extension;

/**
 * Enumeration of X.509 certificate extensions.
 */
public enum X509ExtensionType {

  // ////////////////////////////////
  // Active X509Extension OIDs
  // ////////////////////////////////
  AuditIdentity(Extension.auditIdentity, "", false),

  /**
   * AuthorityInfoAccess extension field.
   */
  AuthorityInformationAccess(Extension.authorityInfoAccess, "Authority Information Access", false),

  /**
   * AuthorityKeyIdentifier extension field.
   */
  AuthorityKeyIdentifier(Extension.authorityKeyIdentifier, "Authority Key Identifier", false),

  /**
   * BasicConstraints extension field.
   */
  BasicConstraints(Extension.basicConstraints, "Basic Constraints", true),

  /**
   * CertificatePolicies extension field.
   */
  CertificatePolicies(Extension.certificatePolicies, "Certificate Policies", false),

  /**
   * CRLDistributionPoints extension field.
   */
  CRLDistributionPoints(Extension.cRLDistributionPoints, "CRL Distribution Points", false),

  /**
   * ExtendedKeyUsage extension field.
   */
  ExtendedKeyUsage(Extension.extendedKeyUsage, "Extended Key Usage", false),

  /**
   * IssuerAlternativeName extension field.
   */
  IssuerAlternativeName(Extension.issuerAlternativeName, "Issuer Alternative Name", false),

  /**
   * KeyUsage extension field.
   */
  KeyUsage(Extension.keyUsage, "Key Usage", true),

  /**
   * NameConstraints extension field.
   */
  NameConstraints(Extension.nameConstraints, "Name Constraints", true),

  /**
   * PolicyConstraints extension field.
   */
  PolicyConstraints(Extension.policyConstraints, "Policy Constraints", false),

  /**
   * PolicyMappings extension field.
   */
  PolicyMappings(Extension.policyMappings, "Policy Mappings", false),

  /**
   * PrivateKeyUsage extension field.
   */
  PrivateKeyUsagePeriod(Extension.privateKeyUsagePeriod, "Private Key Usage Period", false),

  /**
   * SubjectAlternativeName extension field.
   */
  SubjectAlternativeName(Extension.subjectAlternativeName, "Subject Alternative Name", false),

  /**
   * SubjectKeyIdentifier extension field.
   */
  SubjectKeyIdentifier(Extension.subjectKeyIdentifier, "Subject Key Identifier", false),

  /**
   * SubjectDirectoryAttributes extension field.
   */
  SubjectDirectoryAttributes(Extension.subjectDirectoryAttributes, "Subject Directory Attributes", false),
  /**
   * Certificate Issuer
   */
  CertificateIssuer(Extension.certificateIssuer, "Certificate Issuer", false),
  /**
   * CRL Number
   */
  CRLNumber(Extension.cRLNumber, "CRL Number", false),
  /**
   * Delta CRL Indicator
   */
  DeltaCRLIndicator(Extension.deltaCRLIndicator, "Delta CRL Indicator", false),
  /**
   * Expired Certs on CRL
   */

  ExpiredCertsOnCRL(Extension.expiredCertsOnCRL, "Expired Certs on CRL", false),
  /**
   * Freshest CRL
   */

  FreshestCRL(Extension.freshestCRL, "Freshest CRL Distribution Point", false),
  /**
   * Inhibit Any Policy
   */

  InhibitAnyPolicy(Extension.inhibitAnyPolicy, "Skip Certificates", false),
  /**
   * Instruction Code
   */

  InstructionCode(Extension.instructionCode, "Instruction Code", false),
  /**
   * Invalidity Date
   */

  InvalidityDate(Extension.invalidityDate, "Invalidity Date", false),
  /**
   * Issuing Distribution Point
   */

  IssuingDistributionPoint(Extension.issuingDistributionPoint, "Issuing Distribution Point", false),

  /**
   * No Revocation Availability
   */

  NoRevocationAvailability(Extension.noRevAvail, "No Revocation Availability", false),

  /**
   * Reason code
   */

  ReasonCode(Extension.reasonCode, "Reason Code", false),

  /**
   * Subject Information Access
   */

  SubjectInfoAccess(Extension.subjectInfoAccess, "Subject Information Access", false),
  /**
   * Target Information
   */

  TargetInformation(Extension.targetInformation, "Target Information", false),

  // ////////////////////////////////
  // RFC3739 QC PRIVATE EXTENSIONS
  // ////////////////////////////////

  /**
   * Stores biometric information for authentication purposes.
   */
  BiometricInfo(Extension.biometricInfo, "Biometric Info", false),

  /**
   * Indicates that the certificate is a Qualified Certificate in accordance with a particular legal system.
   */
  QcStatements(Extension.qCStatements, "Qualified Certificate Statements", false),

  // ////////////////////////////////
  // RFC 3709
  // ////////////////////////////////
  LogoType(Extension.logoType, "Logo Type", false),

  Unknown(null, "Unknown OID", false);


  private final ASN1ObjectIdentifier asn1obj;
  private final String name;
  private final boolean isCritical;

  /**
   * Creates a new type with the given OID value.
   *
   * @param asn1obj  ASN1ObjectIdentifier value.
   * @param name     A friendly name
   * @param critical True if extension MUST or SHOULD be marked critical under general circumstances, false otherwise.
   */
  X509ExtensionType(final ASN1ObjectIdentifier asn1obj, final String name, final boolean critical) {
    this.asn1obj = asn1obj;
    this.name = name;
    this.isCritical = critical;
  }

  /**
   * Gets the extension by name.
   *
   * @param name Case-sensitive X.509v3 extension name. The acceptable case of extension names is governed by
   *             conventions in RFC 2459.
   * @return Extension with given name.
   * @throws IllegalArgumentException If no extension with given name exists.
   */
  public static X509ExtensionType fromName(final String name) {
    try {
      return X509ExtensionType.valueOf(X509ExtensionType.class, name);
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException("Invalid X.509v3 extension name " + name);
    }
  }

  /**
   * Resolve the supplied object identifier to a matching type.
   *
   * @param oid Object identifier
   * @return Type or null if none
   */
  public static X509ExtensionType fromOid(String oid) {
    for (X509ExtensionType type : values()) {
      if (oid.equals(type.getOid())) {
        return type;
      }
    }

    return Unknown;
  }

  /**
   * @return True if extension MUST or SHOULD be marked critical under general circumstances according to RFC 2459,
   * false otherwise.
   */
  public boolean isCritical() {
    return this.isCritical;
  }

  /**
   * @return OID value of extension field.
   */
  public String getOid() {
    return this.asn1obj.getId();
  }

  /**
   * Returns  name.
   *
   * @return Friendly name
   */
  @Override
  public String toString() {
    return this.name;
  }
}