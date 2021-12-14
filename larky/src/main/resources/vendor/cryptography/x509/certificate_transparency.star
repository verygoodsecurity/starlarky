# This file is dual licensed under the terms of the Apache License, Version
# 2.0, and the BSD License. See the LICENSE file in the root of this repository
# for complete details.
load("@stdlib//enum", enum="enum")
load("@stdlib//larky", larky="larky")


LogEntryType = enum.Enum('LogEntryType', dict(
    X509_CERTIFICATE = 0,
    PRE_CERTIFICATE = 1
).items())


Version = enum.Enum('Version', [('v1', 0)])


def SignedCertificateTimestamp():
    self = larky.mutablestruct(__name__='SignedCertificateTimestamp',
                               __class__=SignedCertificateTimestamp)

    def version():
        """
        Returns the SCT version.
        """
    self.version = version

    def log_id():
        """
        Returns an identifier indicating which log this SCT is for.
        """
    self.log_id = log_id

    def timestamp():
        """
        Returns the timestamp for this SCT.
        """
    self.timestamp = timestamp

    def entry_type():
        """
        Returns whether this is an SCT for a certificate or pre-certificate.
        """
    self.entry_type = entry_type
    return self


certificate_transparency = larky.struct(
    __name__='certificate_transparency',
    LogEntryType=LogEntryType,
    Version=Version,
    SignedCertificateTimestamp=SignedCertificateTimestamp,
)