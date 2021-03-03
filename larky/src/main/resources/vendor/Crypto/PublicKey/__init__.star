def _expand_subject_public_key_info(encoded):
    """
    Parse a SubjectPublicKeyInfo structure.

        It returns a triple with:
            * OID (string)
            * encoded public key (bytes)
            * Algorithm parameters (bytes or None)
    
    """
def _create_subject_public_key_info(algo_oid, secret_key, params=None):
    """
    Extract subjectPublicKeyInfo from a DER X.509 certificate.
    """
